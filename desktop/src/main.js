const path = require('path');
const fs = require('fs');
const { app, BrowserWindow, Menu, Tray, nativeImage, shell, session, ipcMain } = require('electron');
const { createAppServer } = require('./server');

// Allow continuous playback & auto-advance between queued YouTube Music tracks.
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');

const IS_SMOKE_VERIFY = process.argv.includes('--smoke-verify');
if (IS_SMOKE_VERIFY) {
  app.commandLine.appendSwitch('mute-audio');
}

const SINGLE_INSTANCE = IS_SMOKE_VERIFY ? true : app.requestSingleInstanceLock();
if (!SINGLE_INSTANCE) {
  app.quit();
}

let mainWindow = null;
let tray = null;
let appServer = null;
let serverUrl = '';
let appUrl = '';
let isQuitting = false;
let pendingAuthCode = '';
const DEFAULT_BACKEND_URL = 'https://music.jasonhukom.my.id';

if (process.platform === 'win32' && !IS_SMOKE_VERIFY) {
  app.setAsDefaultProtocolClient('melofy');
}

function handleAuthDeepLink(rawUrl) {
  if (!rawUrl || !rawUrl.startsWith('melofy://auth/complete')) return;
  let code = '';
  try { code = new URL(rawUrl).searchParams.get('code') || ''; } catch (_) {}
  if (!code) return;
  if (!mainWindow || mainWindow.isDestroyed() || mainWindow.webContents.isLoading()) {
    pendingAuthCode = code;
    return;
  }
  mainWindow.webContents.executeJavaScript(
    `window.completeDesktopHandoff ? (window.completeDesktopHandoff(${JSON.stringify(code)}), true) : false`
  ).then((delivered) => { if (!delivered) pendingAuthCode = code; })
    .catch(() => { pendingAuthCode = code; });
}

ipcMain.handle('window:minimize', (event) => {
  if (mainWindow && event.sender === mainWindow.webContents) mainWindow.minimize();
});
ipcMain.handle('window:toggle-maximize', (event) => {
  if (mainWindow && event.sender === mainWindow.webContents) {
    if (mainWindow.isMaximized()) mainWindow.unmaximize();
    else mainWindow.maximize();
  }
});
ipcMain.handle('window:close', (event) => {
  if (mainWindow && event.sender === mainWindow.webContents) mainWindow.close();
});
ipcMain.handle('external:open', (event, rawUrl) => {
  if (!mainWindow || event.sender !== mainWindow.webContents) return;
  try {
    const url = new URL(rawUrl);
    const appHost = new URL(appUrl || DEFAULT_BACKEND_URL).hostname;
    if (url.protocol !== 'https:' || ![appHost, 'accounts.google.com'].includes(url.hostname)) return;
    return shell.openExternal(url.href);
  } catch (_) {}
});

function resolveIconPath() {
  const candidates = [
    path.join(__dirname, '..', 'assets', 'icon.png'),
    path.join(process.resourcesPath || '', 'app.asar.unpacked', 'assets', 'icon.png'),
    path.join(__dirname, '..', '..', 'backend', 'frontend', 'icons', 'icon-512.png'),
  ];
  return candidates.find((p) => p && fs.existsSync(p)) || null;
}

function getCleanUserAgent() {
  const raw = app.userAgentFallback || '';
  return raw
    .replace(/\s*Electron\/[^\s]+/gi, '')
    .replace(/\s*lyra-melofy-desktop\/[^\s]+/gi, '')
    .trim();
}

function setupSessionHeaders() {
  const ses = session.defaultSession;
  ses.setUserAgent(getCleanUserAgent());

  // Ensure YouTube IFrame embeds always receive a valid HTTP Referer matching our local server origin.
  ses.webRequest.onBeforeSendHeaders(
    { urls: ['https://www.youtube.com/*', 'https://www.youtube-nocookie.com/*', 'https://*.googlevideo.com/*'] },
    (details, callback) => {
      const headers = { ...details.requestHeaders };
      if (!headers.Referer && !headers.referer && serverUrl) {
        headers.Referer = `${appUrl || serverUrl}/`;
      }
      callback({ requestHeaders: headers });
    }
  );
}

function sendPlayerCommand(js) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.executeJavaScript(js).catch(() => {});
  }
}

function createTray(iconPath) {
  if (!iconPath) return;
  const img = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });
  tray = new Tray(img);
  tray.setToolTip('Lyra Melofy');
  const menu = Menu.buildFromTemplate([
    {
      label: 'Open Lyra Melofy',
      click: () => {
        if (mainWindow) {
          if (mainWindow.isMinimized()) mainWindow.restore();
          mainWindow.show();
          mainWindow.focus();
        }
      },
    },
    { type: 'separator' },
    { label: 'Play / Pause', click: () => sendPlayerCommand('window.Player && Player.togglePlay()') },
    { label: 'Next Track', click: () => sendPlayerCommand('window.Player && Player.next()') },
    { label: 'Previous Track', click: () => sendPlayerCommand('window.Player && Player.prev()') },
    { type: 'separator' },
    {
      label: 'Quit',
      click: () => {
        isQuitting = true;
        app.quit();
      },
    },
  ]);
  tray.setContextMenu(menu);
  tray.on('double-click', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

function createMenu() {
  const template = [
    {
      label: 'Lyra Melofy',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        {
          label: 'Quit',
          accelerator: 'CmdOrCtrl+Q',
          click: () => {
            isQuitting = true;
            app.quit();
          },
        },
      ],
    },
    {
      label: 'Playback',
      submenu: [
        { label: 'Play / Pause', accelerator: 'Space', click: () => sendPlayerCommand('if (document.activeElement?.tagName !== "INPUT") window.Player && Player.togglePlay()') },
        { label: 'Next Track', accelerator: 'CmdOrCtrl+Right', click: () => sendPlayerCommand('window.Player && Player.next()') },
        { label: 'Previous Track', accelerator: 'CmdOrCtrl+Left', click: () => sendPlayerCommand('window.Player && Player.prev()') },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'togglefullscreen' },
        { role: 'toggleDevTools' },
      ],
    },
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

function createWindow() {
  const iconPath = resolveIconPath();
  mainWindow = new BrowserWindow({
    show: !IS_SMOKE_VERIFY,
    width: 1280,
    height: 820,
    minWidth: 420,
    minHeight: 620,
    title: 'Lyra Melofy',
    frame: process.platform !== 'win32',
    backgroundColor: '#0A0905',
    icon: iconPath || undefined,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      backgroundThrottling: false,
    },
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      shell.openExternal(url);
    }
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  mainWindow.webContents.on('did-finish-load', () => {
    if (!pendingAuthCode) return;
    const code = pendingAuthCode;
    pendingAuthCode = '';
    handleAuthDeepLink(`melofy://auth/complete?code=${encodeURIComponent(code)}`);
  });
  mainWindow.loadURL(appUrl);
  if (!IS_SMOKE_VERIFY) {
    createTray(iconPath);
  }
}

async function runSmokeVerify() {
  try {
    const result = await mainWindow.webContents.executeJavaScript(`
      (async () => {
        const deadline = Date.now() + 20000;
        while (!(window.Player && window.Player.getState().isReady)) {
          if (Date.now() > deadline) return { ok: false, step: 'wait_ready_timeout' };
          await new Promise(r => setTimeout(r, 200));
        }
        const picks = await (await fetch('/api/random')).json();
        const search = await (await fetch('/api/search?q=lofi+beats')).json();
        if (!picks.results?.length || !search.results?.length) {
          return { ok: false, step: 'api_empty', picksCount: picks.results?.length || 0, searchCount: search.results?.length || 0 };
        }
        return await new Promise((resolve) => {
          let done = false;
          const t = setTimeout(() => {
            if (!done) { done = true; resolve({ ok: false, step: 'play_timeout' }); }
          }, 15000);
          window.Player.onEvent((type) => {
            if (type === 'play' && !done) {
              done = true;
              clearTimeout(t);
              resolve({ ok: true, randomCount: picks.results.length, searchCount: search.results.length, playing: window.Player.currentSong()?.title });
            }
          });
          window.Player.playQueue(picks.results, 0);
        });
      })();
    `);
    console.log('SMOKE_VERIFY:', JSON.stringify(result));
    await appServer.stop();
    app.exit(result && result.ok ? 0 : 1);
  } catch (err) {
    console.error('SMOKE_VERIFY_ERR:', err);
    app.exit(1);
  }
}

app.on('second-instance', (_event, commandLine) => {
  const deepLink = commandLine.find((arg) => arg.startsWith('melofy://auth/complete'));
  if (deepLink) handleAuthDeepLink(deepLink);
  if (mainWindow) {
    if (!mainWindow.isVisible()) mainWindow.show();
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.focus();
  }
});

app.on('open-url', (event, url) => {
  event.preventDefault();
  handleAuthDeepLink(url);
});

app.on('before-quit', async () => {
  isQuitting = true;
  if (appServer) {
    try {
      await appServer.stop();
    } catch (_) {}
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.whenReady().then(async () => {
  try {
    appServer = createAppServer();
    serverUrl = await appServer.start(0, '127.0.0.1');
    const configuredBackend = process.env.MELOFY_BACKEND_URL || DEFAULT_BACKEND_URL;
    appUrl = IS_SMOKE_VERIFY ? serverUrl : configuredBackend.replace(/\/+$/, '');
    handleAuthDeepLink(process.argv.find((arg) => arg.startsWith('melofy://auth/complete')));
    setupSessionHeaders();
    if (!IS_SMOKE_VERIFY) {
      createMenu();
    }
    createWindow();
    if (IS_SMOKE_VERIFY) {
      mainWindow.webContents.once('did-finish-load', () => {
        runSmokeVerify();
      });
    }
  } catch (err) {
    console.error('Failed to start Lyra Melofy desktop:', err);
    app.quit();
  }
});

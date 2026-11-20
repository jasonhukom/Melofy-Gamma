const http = require('http');
const fs = require('fs');
const path = require('path');
const { search, randomPicks } = require('./ytmusic');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.woff2': 'font/woff2',
};

function resolveFrontendDir() {
  const candidates = [
    path.join(__dirname, '..', 'app-frontend'),
    path.join(__dirname, '..', '..', 'backend', 'frontend'),
  ];
  for (const candidate of candidates) {
    if (fs.existsSync(path.join(candidate, 'index.html'))) {
      return candidate;
    }
  }
  throw new Error('Could not locate frontend directory (checked app-frontend and backend/frontend).');
}

function renderIndexHtml(rawHtml) {
  return rawHtml
    .replace(/\{%\s*load\s+static\s*%\}\r?\n?/g, '')
    .replace(/\{%\s*static\s+['"]([^'"]+)['"]\s*%\}/g, '/static/$1');
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
  });
  res.end(body);
}

function serveStaticFile(res, frontendDir, relPath) {
  const safeRel = relPath.replace(/^\/+/, '');
  const resolved = path.resolve(frontendDir, safeRel);
  const rootResolved = path.resolve(frontendDir);
  if (!resolved.startsWith(rootResolved + path.sep) && resolved !== rootResolved) {
    res.writeHead(403);
    res.end('Forbidden');
    return true;
  }
  if (!fs.existsSync(resolved) || fs.statSync(resolved).isDirectory()) {
    return false;
  }
  const ext = path.extname(resolved).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';
  const data = fs.readFileSync(resolved);
  res.writeHead(200, { 'Content-Type': contentType });
  res.end(data);
  return true;
}

function createAppServer(options = {}) {
  const frontendDir = options.frontendDir || resolveFrontendDir();
  const remoteBackend = (options.remoteBackend || process.env.MELOFY_BACKEND_URL || '').replace(/\/+$/, '');
  const indexTemplate = fs.readFileSync(path.join(frontendDir, 'index.html'), 'utf8');
  const renderedIndex = renderIndexHtml(indexTemplate);

  const server = http.createServer(async (req, res) => {
    try {
      const reqUrl = new URL(req.url || '/', 'http://127.0.0.1');
      const pathname = decodeURIComponent(reqUrl.pathname);

      if (pathname === '/api/health') {
        sendJson(res, 200, { ok: true, mode: 'desktop', remoteBackend: remoteBackend || null });
        return;
      }

      if (pathname === '/api/search') {
        const query = (reqUrl.searchParams.get('q') || '').trim();
        if (!query) {
          sendJson(res, 400, { error: 'Missing q parameter' });
          return;
        }
        try {
          const results = await search(query);
          sendJson(res, 200, { query, results });
        } catch (err) {
          sendJson(res, 502, { error: 'Search is unavailable right now. Please try again in a moment.' });
        }
        return;
      }

      if (pathname === '/api/random') {
        try {
          const payload = await randomPicks();
          sendJson(res, 200, payload);
        } catch (err) {
          sendJson(res, 502, { error: 'Could not fetch picks right now.' });
        }
        return;
      }

      if (pathname === '/api/config') {
        if (remoteBackend) {
          try {
            const upstream = await fetch(`${remoteBackend}/api/config`, { signal: AbortSignal.timeout(5000) });
            const json = await upstream.json();
            sendJson(res, upstream.status, json);
            return;
          } catch (_) {
            // Fall through to standalone config
          }
        }
        sendJson(res, 200, { firebase: null, youtubeLinking: false, desktop: true });
        return;
      }

      if (pathname === '/api/auth/me') {
        sendJson(res, 200, { signedIn: false });
        return;
      }

      if (pathname.startsWith('/static/')) {
        const rel = pathname.slice('/static/'.length);
        if (serveStaticFile(res, frontendDir, rel)) return;
        res.writeHead(404);
        res.end('Not found');
        return;
      }

      if (pathname === '/favicon.ico' || pathname === '/favicon.svg' || pathname === '/site.webmanifest') {
        if (serveStaticFile(res, frontendDir, pathname.slice(1))) return;
      }

      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderedIndex);
    } catch (err) {
      sendJson(res, 500, { error: 'Internal server error' });
    }
  });

  function start(port = 0, host = '127.0.0.1') {
    return new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(port, host, () => {
        const addr = server.address();
        resolve(`http://${host}:${addr.port}`);
      });
    });
  }

  function stop() {
    return new Promise((resolve) => server.close(() => resolve()));
  }

  return { server, start, stop, frontendDir };
}

module.exports = { createAppServer, renderIndexHtml, resolveFrontendDir };

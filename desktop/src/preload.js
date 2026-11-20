const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('LyraDesktop', {
  isDesktop: true,
  platform: process.platform,
  versions: {
    electron: process.versions.electron,
    chrome: process.versions.chrome,
  },
});

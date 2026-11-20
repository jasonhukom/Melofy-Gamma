const fs = require('fs');
const path = require('path');

const desktopRoot = path.resolve(__dirname, '..');
const backendFrontend = path.resolve(desktopRoot, '..', 'backend', 'frontend');
const targetFrontend = path.join(desktopRoot, 'app-frontend');
const assetsDir = path.join(desktopRoot, 'assets');

function copyDirSync(src, dest) {
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDirSync(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

if (!fs.existsSync(backendFrontend)) {
  throw new Error(`Expected backend/frontend at ${backendFrontend}`);
}

fs.rmSync(targetFrontend, { recursive: true, force: true });
copyDirSync(backendFrontend, targetFrontend);

fs.mkdirSync(assetsDir, { recursive: true });
const srcPng = path.join(backendFrontend, 'icons', 'icon-512.png');
const dstPng = path.join(assetsDir, 'icon.png');
if (fs.existsSync(srcPng)) {
  fs.copyFileSync(srcPng, dstPng);
}

console.log(`Prepared frontend assets (${targetFrontend}) and app icon (${dstPng}).`);

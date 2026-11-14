const { app, BrowserWindow, shell } = require('electron');
const path = require('node:path');

let windowRef;
const port = 41765;
app.whenReady().then(async () => {
  process.env.PORT = String(port);
  process.env.PXG_HOST = '127.0.0.1';
  process.env.PXG_DATA_DIR = path.join(app.getPath('userData'), 'workspace');
  // The app server is bundled with the desktop client; only the local UI may reach it.
  await import(pathToFileURL(path.join(app.getAppPath(), 'server', 'index.js')).href);
  await waitForServer();
  windowRef = new BrowserWindow({
    width: 1320, height: 880, minWidth: 360, minHeight: 600,
    title: 'PXG File Workbench', backgroundColor: '#f7f8fc',
    webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true }
  });
  windowRef.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://')) shell.openExternal(url);
    return { action: 'deny' };
  });
  windowRef.loadURL(`http://127.0.0.1:${port}`);
  windowRef.on('closed', () => { windowRef = null; });
});

function pathToFileURL(file) { return require('node:url').pathToFileURL(file); }
async function waitForServer() {
  for (let i=0;i<80;i++) {
    try { const r=await fetch(`http://127.0.0.1:${port}/api/health`); if(r.ok)return; } catch {}
    await new Promise(resolve=>setTimeout(resolve,125));
  }
  throw new Error('The local PXG service could not start.');
}
app.on('window-all-closed', () => { if(process.platform!=='darwin')app.quit(); });
app.on('activate', () => { if(BrowserWindow.getAllWindows().length===0)app.whenReady().then(()=>windowRef?.show()); });

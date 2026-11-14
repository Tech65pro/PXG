# PXG File Workbench

A full-stack desktop-ready workspace for safely inspecting unfamiliar files. It combines a React dashboard with an Express API and persistent local JSON/file storage.

## Run locally

```sh
npm install
npm run dev
```

Open the Vite preview (normally `http://localhost:5173`). Demo sign-in:

- **Email:** `demo@pxg.dev`
- **Password:** `demo1234`

Production server: `npm run build && npm start` (serves the built UI and API on port 4100). Uploads and workspace data persist under `data/` by default; set `PXG_DATA_DIR` to choose another location.

## Windows installer

On Windows with Node.js 22 installed:

```powershell
npm ci
npm run dist:win
```

The installer is written to `release/`. The **Windows desktop installer** GitHub Actions workflow also builds an `.exe` and uploads it as a downloadable workflow artifact when manually run (or on matching pushes to `main`). The installer stores personal data in the user's Electron application-data directory.

## What it can inspect

- Upload files up to 100 MB (15 per batch), identify basic file metadata, SHA-256 fingerprint, and known source-language labels.
- Safely preview recognized text/source files (Python, Java, JS/TS, C/C++, C#, Go, Rust, PHP, Ruby, HTML/CSS, JSON, Markdown, shell, Dart, and more).
- List ZIP archive entries without extracting or running them. RAR is recognized by extension but its contents are not unpacked.
- Organize uploads into workspaces; create, edit, and remove workspaces and notes; browse activity history.
- Data and uploaded originals persist locally. This demo uses a single seeded demo login, not a public multi-user identity service.

## Safety boundaries

PXG is an **inspector, not a universal runtime**. It never executes uploaded programs or scripts, launches installers, cracks software or game accounts, or bypasses authentication. Executables are handled as static metadata only. EXR and other binary image/media formats are identified but not rendered. Text preview is read-only; ZIP listing is capped. Treat files from unknown sources carefully.

## Stack

React + Vite, Express, JSON-backed persistent storage, session-cookie authentication, Multer uploads, AdmZip ZIP listing, and an Electron wrapper for Windows packaging.

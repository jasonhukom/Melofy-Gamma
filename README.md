# Lyra Melofy
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
A free, YouTube-powered music player with a **Black & Yellow (`#0A0905` / `#FACC15`) UI**,
concentric sound-wave circle logo, and a **Metrolist-inspired bottom player bar** —
unified in one repository across **Web** (Django + SPA), **Windows Desktop**
(Electron `.exe`), and **Android** (WebView + embedded local YouTube Music bridge `.apk`).

> **AI Agents:** Read [`AGENTS.md`](AGENTS.md) (and [`CLAUDE.md`](CLAUDE.md)) before making changes for the full architecture and file map across all three platforms.

```
lyra-melofy/
├── backend/            Django project — Web API + shared SPA frontend
│   ├── melofy_backend/ settings, urls, wsgi
│   ├── core/           keyless ytmusicapi / InnerTube search + random picks
│   ├── accounts/       Firebase login + YouTube account linking
│   ├── frontend/       shared single-page app (HTML/CSS/JS)
│   └── requirements.txt, .env.example, .python-version
├── desktop/            Electron desktop app (Windows .exe — JavaScript / Node.js)
│   ├── src/main.js     BrowserWindow, Tray, background playback, clean Chrome UA
│   ├── src/server.js   embedded HTTP server on 127.0.0.1 serving shared frontend
│   ├── src/ytmusic.js  keyless YouTube Music InnerTube WEB_REMIX client
│   └── package.json    electron-builder config (Portable + NSIS Setup .exe)
├── android/            Android Studio project (.apk — Java + WebView + local YouTube Music server)
├── figma/              Figma UI kit
└── .github/workflows/  CI/CD builds for Windows .exe, Android .apk, and Web
```

## Architecture across Web, `.exe`, and `.apk`

- **Single shared UI (`backend/frontend/`):** Plain HTML/CSS/JS SPA (no build step required), featuring a black (`#0A0905`) & yellow (`#FACC15`) palette, circular concentric sound-wave branding, Metrolist-inspired 3-zone desktop player bar + floating mobile mini-player card, slidable drawer, inline/floating sound mixer, mobile 5-tab navigation, and persistent YouTube IFrame Player API playback.
- **Desktop `.exe` (`desktop/` — Electron + JavaScript/Node.js):**
  - **Best fit & language:** **Electron (JavaScript / Node.js)** shares the exact same language and UI as `backend/frontend/`, embeds Chromium + Node.js into a standalone `.exe` (requiring zero Python or API key setup from end users), disables background throttling so music never stops when minimized, and runs a built-in loopback HTTP server + keyless YouTube Music InnerTube client (`WEB_REMIX`) so YouTube IFrame Player API always has a valid `http://127.0.0.1:<port>` origin and `Referer`.
- **Android `.apk` (`android/` — Java + WebView + embedded `LocalMelofyServer`):**
  - Syncs `backend/frontend/` at build time (`syncWebFrontend` Gradle task), runs a loopback HTTP server (`LocalMelofyServer`) + YouTube Music client (`YouTubeMusicClient` supporting both keyless InnerTube `WEB_REMIX` and optional YouTube Data API v3 via `YOUTUBE_API_KEY`) directly inside the APK so search, random picks, playlists, liked songs, and YouTube Music playback work out of the box without requiring an external backend server.
- **Web (`backend/` — Django + Python):**
  - Uses `ytmusicapi` with direct InnerTube `WEB_REMIX` and optional `YOUTUBE_API_KEY` fallbacks, plus optional Firebase Auth & Google OAuth YouTube library integration.

## What's still true

Playback goes through YouTube's official IFrame Player API — the legitimate way to embed YouTube, and the reason ads on individual videos can't be suppressed (that's controlled by YouTube/the content owner, not this app). Downloading/saving audio streams outside YouTube's player isn't built for the same reason.

## 1. Run / Build the Windows `.exe` (`desktop/`)

Requires **Node.js 20+**:

```bash
cd desktop
npm install
npm start          # launch desktop app in dev mode
npm run test:smoke # verify YouTube Music search, random picks, and local server
npm run build      # builds Portable + Setup .exe into desktop/dist/
```

Outputs in `desktop/dist/`:
- `Lyra-Melofy-Portable-1.1.0.exe` — run directly without installing
- `Lyra-Melofy-Setup-1.1.0.exe` — standard Windows installer

## 2. Run / Build the Android `.apk` (`android/`)

Open `android/` in Android Studio or build via Gradle:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

APKs are generated under `android/app/build/outputs/apk/`.

## 3. Run / Deploy the Web App (`backend/`)

### One-time optional setup (Account Sign-in & YouTube Library Sync)
Search and home picks use keyless YouTube Music (`ytmusicapi` / InnerTube `WEB_REMIX`), so **no API key is required**.

Optionally set in `backend/.env` (copy from `backend/.env.example`):
1. `YOUTUBE_API_KEY`: Optional YouTube Data API v3 fallback key.
2. Firebase Auth (`FIREBASE_API_KEY`, `FIREBASE_AUTH_DOMAIN`, `FIREBASE_PROJECT_ID`, `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON`): Enables "Sign in with Google".
3. Google OAuth (`GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `PUBLIC_BASE_URL`): Enables "Connect your YouTube account" (liked videos, subscriptions, playlists, and playlist tracks).

The Windows desktop app opens the production web backend (`https://melofy-gamma.vercel.app`) by default so Google sign-in and YouTube account linking use the same configured service as web and Android. Set `MELOFY_BACKEND_URL` to use another deployment. Desktop Google authorization opens in the system browser and returns to the app through the `melofy://` protocol. Set Vercel's `PUBLIC_BASE_URL` to this hostname for Google OAuth callbacks; use `https://music.jasonhukom.my.id` after the home server and DNS are ready.

YouTube's Data API does not expose a user's watch history. Melofy shows its own recently played list from the current device instead; that list is stored locally and is not the Google account's YouTube history.

### Run locally

```bash
cd backend
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
python manage.py migrate
python manage.py runserver
```

Open http://127.0.0.1:8000.

### Deploy to Vercel

1. Push to GitHub and import the repo on [vercel.com](https://vercel.com) with **Root Directory** set to `backend`.
2. Add environment variables from `backend/.env.example` (including `DATABASE_URL` Postgres connection string, `PUBLIC_BASE_URL`, and `DJANGO_SECRET_KEY`).
3. Run `python manage.py migrate` against your production database.

## License

MIT — see [LICENSE](LICENSE).

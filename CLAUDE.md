# CLAUDE.md

See [`AGENTS.md`](AGENTS.md) for the complete architecture, visual identity tokens, file map, and build/test commands for **Lyra Melofy**.

## Quick Orientation for AI Agents

1. **Shared UI (`backend/frontend/`)**: Plain HTML/CSS/JS SPA served by all three platforms:
   - **Web**: Django (`backend/`)
   - **Windows `.exe`**: Electron (`desktop/`, synced via `desktop/scripts/prepare-assets.js`)
   - **Android `.apk`**: Java + `WebView` + `LocalMelofyServer` (`android/`, synced via Gradle `syncWebFrontend`)
   Always modify UI in `backend/frontend/` — never edit `android/app/src/main/assets/www/` or `desktop/app-frontend/`.
2. **Design Identity**: Black (`#0A0905`) & Yellow (`#FACC15`) palette, circular concentric sound-wave logo (`backend/frontend/favicon.svg`, `backend/frontend/icons/*.png`, `android/app/src/main/res/drawable/ic_launcher_foreground.xml`), `Space Grotesk` + `Manrope` typography, and the **Metrolist-inspired** bottom player bar (`backend/frontend/js/playerbar.js` + `backend/frontend/css/player.css`).
3. **YouTube Music Search & API v3**:
   - Python: `backend/core/youtube.py` (`ytmusicapi` -> InnerTube `WEB_REMIX` -> YouTube Data API v3 fallback)
   - Node.js: `desktop/src/ytmusic.js` (InnerTube `WEB_REMIX` -> YouTube Data API v3 fallback)
   - Android Java: `android/app/src/main/java/com/melofy/app/data/YouTubeMusicClient.java` (InnerTube `WEB_REMIX` -> YouTube Data API v3 fallback via `BuildConfig.YOUTUBE_API_KEY` or `YOUTUBE_API_KEY` env var)
4. **Quick Verification**:
   - `cd desktop && node scripts/prepare-assets.js && node scripts/smoke-test.js`
   - `cd android && .\gradlew.bat testDebugUnitTest assembleDebug --no-daemon`
   - `cd backend && python manage.py check && python manage.py test`

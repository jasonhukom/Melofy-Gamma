# AGENTS.md — AI Agent Guide for Lyra Melofy

This document gives AI coding agents a complete, accurate map of **Lyra Melofy** (`Melofy-Gamma`) so you can work on Web, Windows Desktop, or Android without re-scanning the repository.

---

## 1. Visual Identity & Design System

- **Theme**: Original Android **Black & Yellow** palette across all platforms.
  - Background Void: `#0A0905` (`--bg-void`, Android `bg_void`, Electron `backgroundColor`)
  - Base Surface: `#0E0D08` (`--bg-base`, Android `nav_bg`)
  - Card Surface: `#16140E` (`--bg-card`, Android `surface_card`)
  - Elevated Surface: `#18160F` (`--bg-elevated`) / `#1E1B12` (Android `surface_elevated`)
  - Highlight / Border: `#262217` (`--bg-highlight`) / `#2A261A` (`--border`, Android `surface_border`)
  - Primary Accent (Yellow): `#FACC15` (`--accent`, Android `yellow_primary`)
  - Secondary / Hover Accent (Amber): `#F59E0B` (`--accent-hover`, Android `amber_secondary`)
  - Love / Liked Accent: `#EF4444` (`--love`, Android `red_liked`)
  - Typography: `'Space Grotesk'` (display headings) and `'Manrope'` (body/UI), loaded via Google Fonts in `backend/frontend/index.html`.
- **Logo & Icons**: Black (`#0A0905`) rounded/circular base with a yellow (`#FACC15`) concentric circular sound-wave emblem:
  - Web vector & topbar mark: `backend/frontend/favicon.svg` and inline `.ytm-logo-badge` SVG in `backend/frontend/index.html`.
  - Web PNGs & ICO: `backend/frontend/favicon.ico` and `backend/frontend/icons/{favicon-16,favicon-32,favicon-48,apple-touch-icon,icon-192,icon-512}.png`.
  - Android adaptive icon foreground: `android/app/src/main/res/drawable/ic_launcher_foreground.xml` over `@color/bg_void` (`#0A0905`) in `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
- **Bottom Player Bar (`Metrolist`-inspired)**:
  - **Desktop (`> 768px`)** (`backend/frontend/js/playerbar.js` + `css/player.css`):
    - Two-tier bar (`--player-height: 88px`): top `.pb-progress-row` (`#pb-current-time`, `#pb-progress` seek slider, `#pb-duration`), bottom `.pb-main-row` 3-zone grid:
      1. `.pb-track-left`: `48x48` artwork (`#pb-art` containing `#pb-art-img`, `#yt-player-mount`, `.pb-eq-badge`), `#pb-title`, `#pb-artist`, and pill quick actions (`#pb-like`, `#pb-add`).
      2. `.pb-transport-center`: `#pb-prev`, expressive squircle/circle `#pb-play` (`.is-playing` morphs radius to `15px`), `#pb-next`.
      3. `.pb-right` + `#sound-mixer`: `#pb-shuffle`, `#pb-repeat` (cycles `off` -> `all` -> `one`), and inline horizontal volume control (`#sound-mixer-btn` + `#sound-mixer-slider` + `#sound-mixer-value`).
  - **Mobile (`<= 768px`)**:
    - `.player-bar` transforms into a floating rounded mini-player card (`border-radius: 18px`, `height: 66px`) hovering above the 5-button `.mobile-tabbar` (`Home`, `Explore`, enlarged `62px` circular yellow `Search`, `Saved`, `Profile`), with a `3px` bottom progress bar and floating `#sound-mixer` above the card.

---

## 2. Single Source of Truth Architecture

**`backend/frontend/` is the shared Single-Page App (SPA) UI for ALL three targets:**

1. **Web (`backend/`)**: Django 5+ serves `backend/frontend/index.html` via `core.views.spa_shell` and static files under `/static/`.
2. **Windows Desktop (`desktop/`)**: Electron app runs `desktop/src/server.js` on `http://127.0.0.1:<ephemeralPort>`, rewriting Django `{% load static %}` / `{% static '...' %}` tags on the fly. `desktop/scripts/prepare-assets.js` copies `backend/frontend/` into `desktop/app-frontend/` (gitignored) and `icons/icon-512.png` into `desktop/assets/icon.png` (gitignored).
3. **Android (`android/`)**: `MainActivity.java` hosts a `WebView` pointed at `LocalMelofyServer.java` (`http://127.0.0.1:<ephemeralPort>`), which rewrites Django static tags in `index.html` and serves assets from `src/main/assets/www/`. Gradle's `syncWebFrontend` task (`preBuild.dependsOn(syncWebFrontend)` in `android/app/build.gradle.kts`) automatically mirrors `backend/frontend/` into `android/app/src/main/assets/www/` (gitignored) at build time.

**Rule for UI edits**: Always edit `backend/frontend/` (never edit generated copies in `android/app/src/main/assets/www/` or `desktop/app-frontend/`).

---

## 3. Complete Repository Map

### `backend/` (Django Backend + Shared Frontend)
- `backend/melofy_backend/settings.py`: Env-driven settings (`DJANGO_SECRET_KEY`, `DJANGO_DEBUG`, `DATABASE_URL`, `YOUTUBE_API_KEY`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `FIREBASE_WEB_CONFIG`, `PUBLIC_BASE_URL`).
- `backend/melofy_backend/urls.py`: Mounts `/admin/`, `/api/` (`core.urls` + `accounts.urls`), and fallback `""` -> `core.views.spa_shell`.
- `backend/core/youtube.py`: Search & random picks (`search()`, `random_picks()`). Uses `ytmusicapi` primary -> direct InnerTube `WEB_REMIX` HTTP fallback -> official YouTube Data API v3 (`YOUTUBE_API_KEY`) fallback; 600s in-memory cache.
- `backend/core/views.py` & `backend/core/urls.py`: `GET /api/search?q=...`, `GET /api/random`, and `spa_shell`.
- `backend/core/tests.py`: Django unit tests (`CoreEndpointsTest`).
- `backend/accounts/`: Firebase Google sign-in + YouTube Data API v3 OAuth library sync (`models.py` with `YouTubeAccount` & `OAuthState`, `views.py`, `urls.py`, `firebase.py`, `google_oauth.py`, `handoff.py`, `youtube_account.py`).
  - Endpoints: `/api/config`, `/api/auth/session`, `/api/auth/mobile-handoff`, `/api/auth/exchange`, `/api/auth/logout`, `/api/auth/me`, `/api/youtube/{connect,callback,disconnect,liked,subscriptions,playlists}`.

### `backend/frontend/` (Shared SPA)
- `index.html`: Shell markup (`#topbar`, `#sidebar`, `#main-content`, `#sound-mixer`, `#player-bar-root`, `#mobile-tabbar`, `#modal-root`, `#toast-root`), loads CSS and deferred JS scripts in order.
- `css/`:
  - `variables.css`: Core black/yellow palette, layout dimensions (`--player-height: 88px`), typography variables.
  - `base.css`: Reset, typography defaults, reduced-motion support, focus outlines.
  - `layout.css`: `.topbar`, `.sidebar` drawer, `.app-shell`, `.main-content`, and `@media (max-width: 768px)` `.mobile-tabbar` 5-button layout.
  - `components.css`: Buttons, mood `.genre-chip` row, `.ytm-quick-picks-grid`, `.ytm-shelf-grid`, `.song-row`, `.library-grid`, search input, modals, toasts, account badges.
  - `player.css`: Metrolist-inspired `.player-bar` (desktop top progress bar + 3-zone grid + inline `.sound-mixer`) and mobile floating mini-player card.
  - `theme.css`: Loaded last; visual identity refinements, glass backdrops, yellow glow & button styling.
- `js/`:
  - `storage.js`: `Storage` namespace backed by `localStorage` (`melofy:liked`, `melofy:playlists`, `melofy:recent`, `melofy:settings`, `melofy:playerState`).
  - `ui.js`: `UI` DOM helper `el()`, inline SVG `icons` map (`play`, `pause`, `next`, `prev`, `heart`, `heartFilled`, `shuffle`, `repeat`, `repeatOne`, `plus`, `volumeHigh`, `volumeMute`, etc.), `formatTime`, `debounce`, `toast`, `openModal`/`closeModal`, `songRow`, `mediaCard`, `openAddToPlaylist`.
  - `player.js`: `Player` wrapper over official YouTube IFrame Player API (`YT.Player` mounted in `#yt-player-mount`), queue management, shuffle/repeat (`off`/`all`/`one`), `navigator.mediaSession` lockscreen/OS metadata & actions, auto-skip on unavailable tracks.
  - `playerbar.js`: `PlayerBar` renders `.player-bar`, syncs progress slider, like/add-to-playlist buttons, shuffle/repeat states, and `#pb-art-img` artwork.
  - `soundMixer.js`: `SoundMixer` volume & mute controller synced to `Player.setVolume()` and `Storage`.
  - `youtubeApi.js`: `YouTubeAPI.search(query)` -> `GET /api/search?q=...` (with client `Map` cache) and `YouTubeAPI.randomPicks()` -> `GET /api/random`.
  - `router.js`: `Router` hash SPA router (`/`, `/trending`, `/search`, `/library`, `/profile`, `/liked`, `/playlist`).
  - `sidebar.js`: `Sidebar` drawer controller (open by default above `960px`).
  - `firebaseAuth.js`: `window.FirebaseAuth` client + `window.completeAndroidHandoff(code)` deep-link callback.
  - `app.js`: Bootstraps `PlayerBar`, `Player`, `Sidebar`, `SoundMixer`, topbar search, routes, and Firebase Auth.
  - `views/`: `home.js` (`HomeView`), `search.js` (`SearchView`), `library.js` (`LibraryView`), `liked.js` (`LikedView`), `playlist.js` (`PlaylistView`).

### `desktop/` (Electron Windows `.exe`)
- `desktop/package.json`: Electron 35 + electron-builder config (`Portable` + `NSIS Setup` x64 targets).
- `desktop/src/main.js`: `BrowserWindow` (`backgroundColor: '#0A0905'`, `backgroundThrottling: false`, clean Chrome UA, YouTube `Referer` header hook), system tray controls, playback shortcuts, and `--smoke-verify` CI mode.
- `desktop/src/preload.js`: Exposes `window.LyraDesktop` via `contextBridge`.
- `desktop/src/server.js`: Loopback HTTP server on `127.0.0.1` serving `app-frontend/` (or `backend/frontend/`), `/api/health`, `/api/search`, `/api/random`, `/api/config`, `/api/auth/me`.
- `desktop/src/ytmusic.js`: Keyless InnerTube `WEB_REMIX` client (`search()`, `randomPicks()`, `parseInnerTubeSongs()`) with optional `process.env.YOUTUBE_API_KEY` Data API v3 fallback.
- `desktop/scripts/prepare-assets.js` & `desktop/scripts/smoke-test.js`: Asset sync and headless API/server smoke tests.

### `android/` (Android APK — Java + WebView)
- `android/app/build.gradle.kts`: `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`, Java 17, `buildFeatures { buildConfig = true }`, `BuildConfig.YOUTUBE_API_KEY` from Gradle property or env `YOUTUBE_API_KEY`, and `syncWebFrontend` task syncing `backend/frontend/` to `src/main/assets/www/`.
- `android/app/src/main/java/com/melofy/app/MainActivity.java`: Configures edge-to-edge dark bars (`#0A0905`), clean Chrome-like UA (`MelofyAndroidApp/1.0`), starts `LocalMelofyServer` with `new YouTubeMusicClient(BuildConfig.YOUTUBE_API_KEY)`, routes OAuth URLs to Chrome Custom Tabs, and forwards `melofy://auth?code=...` deep links to `window.completeAndroidHandoff`.
- `android/app/src/main/java/com/melofy/app/server/LocalMelofyServer.java`: Embedded `ServerSocket` on `127.0.0.1:<ephemeralPort>` serving `assets/www/`, rewriting Django static tags, and exposing `/api/health`, `/api/search`, `/api/random`, `/api/config`, `/api/auth/me`.
- `android/app/src/main/java/com/melofy/app/data/YouTubeMusicClient.java`: Keyless InnerTube `WEB_REMIX` search + random picks + official **YouTube Data API v3** (`https://www.googleapis.com/youtube/v3/search`) fallback when an API key is configured (`YouTubeMusicClient(String apiKey)` or `YOUTUBE_API_KEY` env var). Includes `parseInnerTubeSongs()` and `parseDataApiSongs()`.
- `android/app/src/test/java/com/melofy/app/YouTubeMusicAndServerTest.java`: JUnit tests for InnerTube parsing, YouTube Data API v3 parsing, and `LocalMelofyServer` endpoints + frontend serving.
- `android/app/src/main/res/`: Black & yellow `colors.xml`, `themes.xml`, `activity_main.xml` (WebView + fullscreen video container), and sound-wave `ic_launcher_foreground.xml`.

### `figma/` & `.github/workflows/`
- `figma/index.html` & `figma/lyra-melofy-all-screens.svg`: Figma UI reference kit.
- `.github/workflows/android-release.yml`: CI/CD workflow running Django checks/tests, Android unit tests + debug/release APK builds, Node smoke tests + Windows Portable/Setup `.exe` builds, and publishing GitHub Releases on `v*` tags or `workflow_dispatch`.

---

## 4. Build, Test & Verification Commands

- **Web Backend (Django)**:
  ```bash
  cd backend
  python manage.py check
  python manage.py test
  python manage.py runserver
  ```
- **Windows Desktop (Electron)**:
  ```bash
  cd desktop
  node scripts/prepare-assets.js
  node scripts/smoke-test.js
  npm run build
  ```
- **Android (Gradle)**:
  - On Windows without global `JAVA_HOME`, point `JAVA_HOME` to Android Studio's JBR (`C:\Program Files\Android\Android Studio\jbr`) and `ANDROID_HOME` to `%LOCALAPPDATA%\Android\Sdk`:
  ```powershell
  cd android
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  .\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
  ```
- **Regenerating Web Icons**: Edit `backend/frontend/favicon.svg` and regenerate `backend/frontend/favicon.ico` + `backend/frontend/icons/*.png` (`16`, `32`, `48`, `180`, `192`, `512`) so desktop (`prepare-assets.js`) and Android (`syncWebFrontend`) pick them up automatically.

---

## 5. Key Invariants to Preserve

1. **Never edit `android/app/src/main/assets/www/` or `desktop/app-frontend/` directly** — they are gitignored build outputs overwritten from `backend/frontend/`.
2. **Preserve Django template tags** (`{% load static %}`, `{% static '...' %}`) in `backend/frontend/index.html`; both `LocalMelofyServer.renderIndexHtml()` and `desktop/src/server.js` `renderIndexHtml()` rewrite them automatically.
3. **Keyless by default, API v3 compatible**: Every platform (`backend/core/youtube.py`, `desktop/src/ytmusic.js`, `android/.../YouTubeMusicClient.java`) must keep keyless InnerTube `WEB_REMIX` working with zero API key while supporting `YOUTUBE_API_KEY` (YouTube Data API v3) when provided.
4. **No hardcoded secrets** in code or commits (`backend/.env` and `android/local.properties` are gitignored).

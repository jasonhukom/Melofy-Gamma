/* library.js — TrendingView (fragment_trending.xml), DownloadsView (fragment_download.xml),
   and ProfileView (fragment_profile.xml) matching the Android app. */

const TrendingView = {
  render(container) {
    const topHeader = UI.el('div', { class: 'android-top-header' });
    const headerLeft = UI.el('div', { class: 'android-header-left' });
    headerLeft.append(
      UI.el('div', { class: 'android-header-icon', html: UI.icons.trending }),
      UI.el('div', {
        class: 'android-header-titles',
        children: [
          UI.el('div', { class: 'android-header-title', text: 'Trending Charts' }),
          UI.el('div', { class: 'android-header-subtitle', text: 'Top played tracks this week' })
        ]
      })
    );
    topHeader.appendChild(headerLeft);

    const list = UI.el('div', { class: 'song-list' });
    const sorted = Storage.getSampleTracks().slice().sort((a, b) => (a.rank || 99) - (b.rank || 99));
    sorted.forEach((track, idx) => {
      list.appendChild(UI.songRow(track, {
        context: sorted,
        showRank: true,
        rankNumber: idx + 1
      }));
    });

    container.append(topHeader, list);
  }
};

const DownloadsView = {
  render(container) {
    // Header
    const topHeader = UI.el('div', { class: 'android-top-header' });
    const headerLeft = UI.el('div', { class: 'android-header-left' });
    headerLeft.append(
      UI.el('div', { class: 'android-header-icon', html: UI.icons.downloadDoneSolid }),
      UI.el('div', {
        class: 'android-header-titles',
        children: [
          UI.el('div', { class: 'android-header-title', text: 'Offline Music Library' }),
          UI.el('div', { class: 'android-header-subtitle', text: 'Playable anytime without internet connection' })
        ]
      })
    );
    topHeader.appendChild(headerLeft);

    // Storage Card
    const storageCard = UI.el('div', { class: 'storage-card' });
    const topRow = UI.el('div', { class: 'storage-top-row' });
    const activeBadge = UI.el('div', { class: 'storage-active-badge', text: '• OFFLINE STORAGE ACTIVE' });
    const countText = UI.el('div', { class: 'storage-count-text', attrs: { id: 'text-download-count' } });
    topRow.append(activeBadge, countText);

    const btnRow = UI.el('div', { class: 'storage-btn-row' });
    const playAllBtn = UI.el('button', { class: 'btn-android-primary', text: 'Play All' });
    const shuffleBtn = UI.el('button', { class: 'btn-android-outlined', text: 'Shuffle' });
    btnRow.append(playAllBtn, shuffleBtn);
    storageCard.append(topRow, btnRow);

    const sectionTitle = UI.el('h2', {
      class: 'section-heading',
      text: 'Downloaded Tracks',
      attrs: { style: 'font-size:18px;' }
    });
    const list = UI.el('div', { class: 'song-list' });

    function drawList() {
      const downloaded = Storage.getDownloaded();
      countText.textContent = `${downloaded.length} Tracks Saved`;
      list.innerHTML = '';
      if (!downloaded.length) {
        list.appendChild(UI.el('p', {
          class: 'empty-state',
          text: 'No offline tracks saved yet. Tap the download icon on any track to save it for offline listening.'
        }));
        return;
      }
      downloaded.forEach(track => {
        list.appendChild(UI.songRow(track, {
          context: downloaded,
          onDownloadClick: (s) => {
            Storage.removeDownload(s.id);
            UI.toast('Removed from Offline Library');
            drawList();
          }
        }));
      });
    }

    playAllBtn.addEventListener('click', () => {
      const downloaded = Storage.getDownloaded();
      if (downloaded.length) Player.playQueue(downloaded, 0);
    });

    shuffleBtn.addEventListener('click', () => {
      const downloaded = Storage.getDownloaded().slice();
      if (downloaded.length) {
        for (let i = downloaded.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [downloaded[i], downloaded[j]] = [downloaded[j], downloaded[i]];
        }
        Player.playQueue(downloaded, 0);
      }
    });

    drawList();
    container.append(topHeader, storageCard, sectionTitle, list);
  }
};

const ProfileView = {
  render(container) {
    const profileCard = UI.el('div', { class: 'profile-card' });

    const avatarCircle = UI.el('div', { class: 'profile-avatar-circle', attrs: { id: 'profile-avatar-wrap' } });
    const userNameEl = UI.el('div', { class: 'profile-user-name', text: 'Anonymous Listener' });
    const userStatusEl = UI.el('div', { class: 'profile-user-status', text: 'Guest Mode • Offline Listening Available' });

    // Stats Row (3 columns matching fragment_profile.xml)
    const statsRow = UI.el('div', { class: 'profile-stats-row' });
    const likedCount = Storage.getLiked().length;
    const downloadedCount = Storage.getDownloaded().length;

    const colLiked = UI.el('div', {
      class: 'profile-stat-col',
      children: [
        UI.el('div', { class: 'profile-stat-val stat-liked', text: String(likedCount) }),
        UI.el('div', { class: 'profile-stat-label', text: 'Liked Songs' })
      ]
    });
    const colOffline = UI.el('div', {
      class: 'profile-stat-col',
      children: [
        UI.el('div', { class: 'profile-stat-val stat-offline', text: String(downloadedCount) }),
        UI.el('div', { class: 'profile-stat-label', text: 'Offline Tracks' })
      ]
    });
    const colQuality = UI.el('div', {
      class: 'profile-stat-col',
      children: [
        UI.el('div', { class: 'profile-stat-val stat-quality', text: '320k' }),
        UI.el('div', { class: 'profile-stat-label', text: 'Audio Quality' })
      ]
    });
    statsRow.append(colLiked, colOffline, colQuality);

    const authBtn = UI.el('button', {
      class: 'btn-profile-auth',
      text: 'Connect YouTube / Google Account'
    });

    function syncAuthUi() {
      const fbUser = window.FirebaseAuth ? window.FirebaseAuth.getUser() : null;
      const guestAuth = window.__melofyGuestSignedIn || false;
      const isSignedIn = !!fbUser || guestAuth;

      avatarCircle.innerHTML = '';
      if (isSignedIn) {
        const displayName = (fbUser && (fbUser.displayName || fbUser.email)) || 'Alex Melofy';
        const photoUrl = (fbUser && fbUser.photoURL) || 'https://picsum.photos/id/64/200/200';
        userNameEl.textContent = displayName;
        userStatusEl.textContent = 'YouTube Connected Account';
        authBtn.textContent = 'Sign Out';
        authBtn.classList.add('signed-in');
        avatarCircle.appendChild(UI.el('img', { attrs: { src: photoUrl, alt: displayName } }));
      } else {
        userNameEl.textContent = 'Anonymous Listener';
        userStatusEl.textContent = 'Guest Mode • Offline Listening Available';
        authBtn.textContent = 'Connect YouTube / Google Account';
        authBtn.classList.remove('signed-in');
        avatarCircle.innerHTML = UI.icons.anonymousPfp;
      }
      if (window.updateFooterProfileAvatar) window.updateFooterProfileAvatar();
    }

    authBtn.addEventListener('click', async () => {
      const fbUser = window.FirebaseAuth ? window.FirebaseAuth.getUser() : null;
      if (fbUser) {
        await window.FirebaseAuth.signOutEverywhere();
        window.__melofyGuestSignedIn = false;
        UI.toast('Signed out');
      } else if (window.__melofyGuestSignedIn) {
        window.__melofyGuestSignedIn = false;
        UI.toast('Signed out');
      } else {
        try {
          if (window.FirebaseAuth) {
            await window.FirebaseAuth.signIn();
          } else {
            throw new Error('Fallback');
          }
        } catch (e) {
          window.__melofyGuestSignedIn = true;
          UI.toast('Signed in as Alex Melofy');
        }
      }
      syncAuthUi();
    });

    syncAuthUi();
    profileCard.append(avatarCircle, userNameEl, userStatusEl, statsRow, authBtn);

    // App Settings Section
    const settingsHeading = UI.el('h2', { class: 'section-heading', text: 'App Settings' });
    const settingsBox = UI.el('div', { class: 'settings-box' });
    const settings = Storage.getSettings();

    function buildSwitchRow(labelText, checked, onChange) {
      const row = UI.el('div', { class: 'settings-row' });
      const label = UI.el('span', { class: 'settings-label', text: labelText });
      const switchWrap = UI.el('label', { class: 'android-switch' });
      const input = UI.el('input', { attrs: { type: 'checkbox' } });
      input.checked = checked;
      input.addEventListener('change', () => onChange(input.checked));
      const slider = UI.el('span', { class: 'android-switch-slider' });
      switchWrap.append(input, slider);
      row.append(label, switchWrap);
      return row;
    }

    const rowHq = buildSwitchRow('High Quality Audio (320 kbps)', settings.highQuality !== false, (val) => {
      Storage.updateSettings({ highQuality: val });
    });
    const divider = UI.el('div', { class: 'settings-divider' });
    const rowWifi = buildSwitchRow('Download Over Wi-Fi Only', settings.wifiOnly !== false, (val) => {
      Storage.updateSettings({ wifiOnly: val });
    });

    settingsBox.append(rowHq, divider, rowWifi);
    container.append(profileCard, settingsHeading, settingsBox);
  }
};

const LibraryView = DownloadsView;

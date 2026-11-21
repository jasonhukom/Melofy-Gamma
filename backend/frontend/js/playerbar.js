/* playerbar.js — builds the persistent Metrolist-inspired bottom player bar
   and wires it to player.js.
   Desktop: top-row full-width seek slider flanked by current time & duration,
   plus a 3-zone control row (left art/info/like/add, center prev/play/next,
   right shuffle/repeat + inline volume dock).
   Mobile: floating rounded mini-player card sitting above the 5-button tab bar
   with a sleek bottom progress bar. */
const PlayerBar = (() => {
  let progressTimer = null;

  function render() {
    const root = document.getElementById('player-bar-root');
    root.innerHTML = '';
    const bar = UI.el('div', { class: 'player-bar' });

    // Top row (Desktop) / Bottom edge (Mobile): Seek slider with inline time readouts
    const progressRow = UI.el('div', { class: 'pb-progress-row' });
    const currentTimeEl = UI.el('span', { class: 'pb-time pb-time-start', text: '0:00', attrs: { id: 'pb-current-time' } });
    const progressInput = UI.el('input', {
      class: 'pb-progress',
      attrs: { id: 'pb-progress', type: 'range', min: '0', max: '100', value: '0', step: '0.1', 'aria-label': 'Seek' }
    });
    const durationEl = UI.el('span', { class: 'pb-time pb-time-end', text: '0:00', attrs: { id: 'pb-duration' } });
    progressRow.append(currentTimeEl, progressInput, durationEl);

    const mk = (id, label, icon, extraClass = '') => {
      const b = UI.el('button', {
        class: ('icon-btn ' + extraClass).trim(),
        attrs: { id, type: 'button', 'aria-label': label, title: label }
      });
      b.innerHTML = icon;
      return b;
    };

    const mainRow = UI.el('div', { class: 'pb-main-row' });

    // Left: Artwork + Track Info + Quick Actions (Like & Add to Playlist)
    const trackLeft = UI.el('div', { class: 'pb-track-left' });
    const artWrap = UI.el('div', { class: 'pb-art', attrs: { id: 'pb-art' } });
    const artImg = UI.el('img', { class: 'pb-art-img', attrs: { id: 'pb-art-img', src: '', alt: '' } });
    artImg.hidden = true;
    const mount = UI.el('div', { attrs: { id: 'yt-player-mount' } });
    const eqBadge = UI.el('div', { class: 'pb-eq-badge' });
    eqBadge.innerHTML = '<span></span><span></span><span></span>';
    artWrap.append(artImg, mount, eqBadge);

    const info = UI.el('div', { class: 'pb-info' });
    info.append(
      UI.el('div', { class: 'pb-title', text: 'Not playing', attrs: { id: 'pb-title' } }),
      UI.el('div', { class: 'pb-artist', text: 'Select a song or search to start listening', attrs: { id: 'pb-artist' } })
    );

    const quickActions = UI.el('div', { class: 'pb-quick-actions' });
    const likeBtn = mk('pb-like', 'Like', UI.icons.heart, 'pb-action-pill pb-like');
    const addBtn = mk('pb-add', 'Add to playlist', UI.icons.plus, 'pb-action-pill pb-add');
    quickActions.append(likeBtn, addBtn);

    trackLeft.append(artWrap, info, quickActions);

    // Center: Primary Transport Controls (Metrolist PlayerPrimaryControls style)
    const controlsWrap = UI.el('div', { class: 'pb-transport-center' });
    const prevBtn = mk('pb-prev', 'Previous', UI.icons.prev, 'pb-side-btn');
    const playBtn = mk('pb-play', 'Play', UI.icons.play, 'pb-play');
    const nextBtn = mk('pb-next', 'Next', UI.icons.next, 'pb-side-btn');
    controlsWrap.append(prevBtn, playBtn, nextBtn);

    // Right: Playback options and a track details menu; external track links live in the menu.
    const right = UI.el('div', { class: 'pb-right' });
    const shuffleBtn = mk('pb-shuffle', 'Shuffle', UI.icons.shuffle, 'pb-util-btn');
    const repeatBtn = mk('pb-repeat', 'Repeat', UI.icons.repeat, 'pb-util-btn');
    const trackMenu = UI.el('div', { class: 'pb-track-menu' });
    const moreBtn = mk('pb-more', 'Track details', UI.icons.more, 'pb-util-btn pb-more-btn');
    moreBtn.setAttribute('aria-haspopup', 'dialog');
    moreBtn.setAttribute('aria-expanded', 'false');
    const menuPanel = UI.el('section', {
      class: 'pb-track-menu-panel',
      attrs: { id: 'pb-track-menu-panel', role: 'dialog', 'aria-label': 'Track details', hidden: '' }
    });
    const titleField = UI.el('div', { class: 'pb-menu-field' });
    titleField.append(UI.el('span', { class: 'pb-menu-label', text: 'Title' }), UI.el('span', { class: 'pb-menu-value', attrs: { id: 'pb-menu-title' } }));
    menuPanel.append(UI.el('div', { class: 'pb-menu-heading', text: 'Track details' }), titleField);
    const artistField = UI.el('div', { class: 'pb-menu-field' });
    artistField.append(UI.el('span', { class: 'pb-menu-label', text: 'Artist' }), UI.el('span', { class: 'pb-menu-value', attrs: { id: 'pb-menu-artist' } }));
    const albumField = UI.el('div', { class: 'pb-menu-field', attrs: { id: 'pb-menu-album-field' } });
    albumField.append(UI.el('span', { class: 'pb-menu-label', text: 'Album' }), UI.el('span', { class: 'pb-menu-value', attrs: { id: 'pb-menu-album' } }));
    const descriptionField = UI.el('div', { class: 'pb-menu-description' });
    descriptionField.append(UI.el('span', { class: 'pb-menu-label', text: 'Description' }), UI.el('p', { attrs: { id: 'pb-menu-description' } }));
    const links = UI.el('div', { class: 'pb-menu-links' });
    links.append(
      UI.el('a', { text: 'Open on YouTube', attrs: { id: 'pb-menu-youtube', target: '_blank', rel: 'noopener noreferrer' } }),
      UI.el('a', { text: 'Open on YouTube Music', attrs: { id: 'pb-menu-youtube-music', target: '_blank', rel: 'noopener noreferrer' } })
    );
    menuPanel.append(artistField, albumField, descriptionField, links);
    trackMenu.append(moreBtn, menuPanel);
    right.append(shuffleBtn, repeatBtn, trackMenu);

    mainRow.append(trackLeft, controlsWrap, right);
    bar.append(progressRow, mainRow);
    root.appendChild(bar);
    wireEvents();
  }

  function updateProgressStyle(pct) {
    const progressEl = document.getElementById('pb-progress');
    if (!progressEl) return;
    const clamped = Math.max(0, Math.min(100, Number(pct) || 0));
    progressEl.style.setProperty('--progress-pct', `${clamped}%`);
  }

  function updateRepeatButton(mode) {
    const btn = document.getElementById('pb-repeat');
    if (!btn) return;
    const activeMode = mode || 'off';
    btn.classList.toggle('active', activeMode !== 'off');
    btn.innerHTML = activeMode === 'one' ? (UI.icons.repeatOne || UI.icons.repeat) : UI.icons.repeat;
    btn.setAttribute('aria-label', activeMode === 'one' ? 'Repeat one' : activeMode === 'all' ? 'Repeat all' : 'Repeat off');
  }

  function wireEvents() {
    document.getElementById('pb-play').addEventListener('click', () => Player.togglePlay());
    document.getElementById('pb-next').addEventListener('click', () => Player.next());
    document.getElementById('pb-prev').addEventListener('click', () => Player.prev());
    document.getElementById('pb-shuffle').addEventListener('click', (e) => {
      const on = Player.toggleShuffle();
      e.currentTarget.classList.toggle('active', on);
    });
    document.getElementById('pb-repeat').addEventListener('click', () => {
      const mode = Player.cycleRepeat();
      updateRepeatButton(mode);
    });

    const moreBtn = document.getElementById('pb-more');
    const menuPanel = document.getElementById('pb-track-menu-panel');
    const closeTrackMenu = () => {
      menuPanel.hidden = true;
      moreBtn.setAttribute('aria-expanded', 'false');
    };
    moreBtn.addEventListener('click', () => {
      const opening = menuPanel.hidden;
      menuPanel.hidden = !opening;
      moreBtn.setAttribute('aria-expanded', String(opening));
    });
    document.addEventListener('click', (event) => {
      if (!(event.target instanceof Element) || !event.target.closest('.pb-track-menu')) closeTrackMenu();
    });
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') closeTrackMenu();
    });

    const progressEl = document.getElementById('pb-progress');
    progressEl.addEventListener('input', (e) => {
      updateProgressStyle(e.target.value);
    });
    progressEl.addEventListener('change', (e) => {
      const state = Player.getState();
      updateProgressStyle(e.target.value);
      Player.seekTo((Number(e.target.value) / 100) * (state.duration || 0));
    });

    document.getElementById('pb-like').addEventListener('click', () => {
      const song = Player.currentSong();
      if (!song) return;
      const liked = Storage.toggleLiked(song);
      const likeBtn = document.getElementById('pb-like');
      likeBtn.innerHTML = liked ? UI.icons.heartFilled : UI.icons.heart;
      likeBtn.classList.toggle('active', liked);
    });

    document.getElementById('pb-add').addEventListener('click', () => {
      const song = Player.currentSong();
      if (!song) {
        UI.toast('Start playing a song first');
        return;
      }
      UI.openAddToPlaylist(song);
    });

    const settings = Storage.getSettings();
    document.getElementById('pb-shuffle').classList.toggle('active', !!settings.shuffle);
    updateRepeatButton(settings.repeat || 'off');

    Player.onEvent((type, data) => {
      if (type === 'trackchange') updateTrackInfo(data);
      if (type === 'play') setPlayIcon(true);
      if (type === 'pause') setPlayIcon(false);
      if (type === 'ready') startProgressLoop();
      if (type === 'shufflechange') {
        document.getElementById('pb-shuffle')?.classList.toggle('active', !!data);
      }
      if (type === 'repeatchange') {
        updateRepeatButton(data);
      }
    });
  }

  function updateTrackInfo(song) {
    if (!song) return;
    document.getElementById('pb-title').textContent = song.title || 'Unknown Title';
    document.getElementById('pb-artist').textContent = song.artist || 'Unknown Artist';
    const artImg = document.getElementById('pb-art-img');
    if (artImg) {
      if (song.thumbnail) {
        artImg.src = song.thumbnail;
        artImg.hidden = false;
      } else {
        artImg.hidden = true;
      }
    }
    const liked = Storage.isLiked(song.id);
    const likeBtn = document.getElementById('pb-like');
    likeBtn.innerHTML = liked ? UI.icons.heartFilled : UI.icons.heart;
    likeBtn.classList.toggle('active', liked);

    document.getElementById('pb-menu-title').textContent = song.title || 'Unknown Title';
    document.getElementById('pb-menu-artist').textContent = song.artist || 'Unknown Artist';
    const albumField = document.getElementById('pb-menu-album-field');
    albumField.hidden = !song.album;
    document.getElementById('pb-menu-album').textContent = song.album || '';
    document.getElementById('pb-menu-description').textContent = song.description || 'Open the track on YouTube to view its description and additional details.';
    const videoId = encodeURIComponent(song.id || '');
    document.getElementById('pb-menu-youtube').href = `https://www.youtube.com/watch?v=${videoId}`;
    document.getElementById('pb-menu-youtube-music').href = `https://music.youtube.com/watch?v=${videoId}`;
  }

  function setPlayIcon(isPlaying) {
    const playBtn = document.getElementById('pb-play');
    if (playBtn) {
      playBtn.innerHTML = isPlaying ? UI.icons.pause : UI.icons.play;
      playBtn.classList.toggle('is-playing', isPlaying);
      playBtn.setAttribute('aria-label', isPlaying ? 'Pause' : 'Play');
    }
    document.getElementById('pb-art')?.classList.toggle('is-playing', isPlaying);
  }

  function startProgressLoop() {
    if (progressTimer) clearInterval(progressTimer);
    progressTimer = setInterval(() => {
      const state = Player.getState();
      if (!state.duration) return;
      const pct = (state.position / state.duration) * 100;
      const progressEl = document.getElementById('pb-progress');
      if (progressEl && document.activeElement !== progressEl) {
        progressEl.value = String(pct);
        updateProgressStyle(pct);
      }
      document.getElementById('pb-current-time').textContent = UI.formatTime(state.position);
      document.getElementById('pb-duration').textContent = UI.formatTime(state.duration);
    }, 500);
  }

  return { render };
})();

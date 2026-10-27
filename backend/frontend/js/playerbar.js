/* playerbar.js — builds the persistent YouTube Music-style bottom player bar
   and wires it to player.js. Includes top-edge full-width red progress bar,
   left transport controls + time counter, center track art/info/like, and
   right repeat/shuffle controls. */
const PlayerBar = (() => {
  let progressTimer = null;

  function render() {
    const root = document.getElementById('player-bar-root');
    root.innerHTML = '';
    const bar = UI.el('div', { class: 'player-bar' });

    // Top-edge full-width YouTube Music progress bar
    const progressRow = UI.el('div', { class: 'pb-progress-row' });
    const progressInput = UI.el('input', {
      class: 'pb-progress',
      attrs: { id: 'pb-progress', type: 'range', min: '0', max: '100', value: '0', 'aria-label': 'Seek' }
    });
    progressRow.appendChild(progressInput);

    const mk = (id, label, icon) => {
      const b = UI.el('button', { class: 'icon-btn', attrs: { id, 'aria-label': label } });
      b.innerHTML = icon;
      return b;
    };

    // Left: Transport controls + Time counter (YouTube Music layout)
    const controlsWrap = UI.el('div', { class: 'pb-transport' });
    const controls = UI.el('div', { class: 'pb-controls' });
    const prevBtn = mk('pb-prev', 'Previous', UI.icons.prev);
    const playBtn = UI.el('button', { class: 'icon-btn pb-play', attrs: { id: 'pb-play', 'aria-label': 'Play' } });
    playBtn.innerHTML = UI.icons.play;
    const nextBtn = mk('pb-next', 'Next', UI.icons.next);
    controls.append(prevBtn, playBtn, nextBtn);

    const timeWrap = UI.el('div', { class: 'pb-time-wrap' });
    timeWrap.append(
      UI.el('span', { class: 'pb-time', text: '0:00', attrs: { id: 'pb-current-time' } }),
      UI.el('span', { class: 'pb-time-sep', text: ' / ' }),
      UI.el('span', { class: 'pb-time', text: '0:00', attrs: { id: 'pb-duration' } })
    );
    controlsWrap.append(controls, timeWrap);

    // Center: Album/Video Art + Track Info + Like/Add buttons
    const center = UI.el('div', { class: 'pb-left pb-track-center' });
    const artWrap = UI.el('div', { class: 'pb-art', attrs: { id: 'pb-art' } });
    const mount = UI.el('div', { attrs: { id: 'yt-player-mount' } });
    const eqBadge = UI.el('div', { class: 'pb-eq-badge' });
    eqBadge.innerHTML = '<span></span><span></span><span></span>';
    artWrap.append(mount, eqBadge);

    const info = UI.el('div', { class: 'pb-info' });
    info.append(
      UI.el('div', { class: 'pb-title', text: 'Not playing', attrs: { id: 'pb-title' } }),
      UI.el('div', { class: 'pb-artist', text: 'Select a song or search to start listening', attrs: { id: 'pb-artist' } })
    );
    const likeBtn = UI.el('button', { class: 'icon-btn pb-like', attrs: { id: 'pb-like', 'aria-label': 'Like' } });
    likeBtn.innerHTML = UI.icons.heart;
    center.append(artWrap, info, likeBtn);

    // Right: Shuffle + Repeat controls
    const right = UI.el('div', { class: 'pb-right' });
    const shuffleBtn = mk('pb-shuffle', 'Shuffle', UI.icons.shuffle);
    const repeatBtn = mk('pb-repeat', 'Repeat', UI.icons.repeat);
    right.append(repeatBtn, shuffleBtn);

    bar.append(progressRow, controlsWrap, center, right);
    root.appendChild(bar);
    wireEvents();
  }

  function updateProgressStyle(pct) {
    const progressEl = document.getElementById('pb-progress');
    if (!progressEl) return;
    const clamped = Math.max(0, Math.min(100, Number(pct) || 0));
    progressEl.style.setProperty('--progress-pct', `${clamped}%`);
  }

  function wireEvents() {
    document.getElementById('pb-play').addEventListener('click', () => Player.togglePlay());
    document.getElementById('pb-next').addEventListener('click', () => Player.next());
    document.getElementById('pb-prev').addEventListener('click', () => Player.prev());
    document.getElementById('pb-shuffle').addEventListener('click', (e) => {
      const on = Player.toggleShuffle();
      e.currentTarget.classList.toggle('active', on);
    });
    document.getElementById('pb-repeat').addEventListener('click', (e) => {
      const mode = Player.cycleRepeat();
      e.currentTarget.classList.toggle('active', mode !== 'off');
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
      document.getElementById('pb-like').innerHTML = liked ? UI.icons.heartFilled : UI.icons.heart;
      document.getElementById('pb-like').classList.toggle('active', liked);
    });

    const settings = Storage.getSettings();
    document.getElementById('pb-shuffle').classList.toggle('active', !!settings.shuffle);
    document.getElementById('pb-repeat').classList.toggle('active', (settings.repeat || 'off') !== 'off');

    Player.onEvent((type, data) => {
      if (type === 'trackchange') updateTrackInfo(data);
      if (type === 'play') setPlayIcon(true);
      if (type === 'pause') setPlayIcon(false);
      if (type === 'ready') startProgressLoop();
    });
  }

  function updateTrackInfo(song) {
    if (!song) return;
    document.getElementById('pb-title').textContent = song.title;
    document.getElementById('pb-artist').textContent = song.artist;
    const liked = Storage.isLiked(song.id);
    const likeBtn = document.getElementById('pb-like');
    likeBtn.innerHTML = liked ? UI.icons.heartFilled : UI.icons.heart;
    likeBtn.classList.toggle('active', liked);
  }

  function setPlayIcon(isPlaying) {
    document.getElementById('pb-play').innerHTML = isPlaying ? UI.icons.pause : UI.icons.play;
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

/* playerbar.js — builds the Android-matched Mini Player Bar (layout_mini_player.xml)
   and Full Screen Player Dialog (dialog_full_player.xml). */
const PlayerBar = (() => {
  let progressTimer = null;

  function render() {
    const root = document.getElementById('player-bar-root');
    root.innerHTML = '';

    // Mini Player (hidden until a track is selected, matching MainActivity.java)
    const mini = UI.el('div', { class: 'mini-player hidden', attrs: { id: 'mini-player-layout' } });
    const inner = UI.el('div', { class: 'mini-player-inner' });

    const cover = UI.el('img', {
      class: 'mini-cover',
      attrs: { id: 'img-mini-cover', src: '', alt: 'Mini Cover' }
    });

    const info = UI.el('div', { class: 'mini-info' });
    info.append(
      UI.el('div', { class: 'mini-title', text: '', attrs: { id: 'text-mini-title' } }),
      UI.el('div', { class: 'mini-artist', text: '', attrs: { id: 'text-mini-artist' } })
    );

    const playPauseBtn = UI.el('button', {
      class: 'mini-btn play-pause',
      attrs: { id: 'btn-mini-play-pause', 'aria-label': 'Play or Pause' },
      html: UI.icons.play
    });

    const nextBtn = UI.el('button', {
      class: 'mini-btn',
      attrs: { id: 'btn-mini-next', 'aria-label': 'Next' },
      html: UI.icons.next
    });

    inner.append(cover, info, playPauseBtn, nextBtn);

    const progressTrack = UI.el('div', { class: 'mini-progress-track' });
    const progressFill = UI.el('div', { class: 'mini-progress-fill', attrs: { id: 'progress-mini-player' } });
    progressTrack.appendChild(progressFill);

    mini.append(inner, progressTrack);
    root.appendChild(mini);

    renderFullPlayerDialog();
    wireEvents();
  }

  function renderFullPlayerDialog() {
    const dialog = document.getElementById('full-player-dialog');
    if (!dialog) return;
    dialog.innerHTML = '';

    // Top Bar
    const topBar = UI.el('div', { class: 'fp-topbar' });
    const collapseBtn = UI.el('button', {
      class: 'fp-collapse-btn',
      attrs: { id: 'btn-collapse', 'aria-label': 'Collapse' },
      html: UI.icons.collapseDown
    });
    const topCenter = UI.el('div', { class: 'fp-topbar-center' });
    topCenter.append(
      UI.el('div', { class: 'fp-kicker', text: 'PLAYING FROM LIBRARY' }),
      UI.el('div', { class: 'fp-album', text: 'Melofy Player', attrs: { id: 'text-full-album' } })
    );
    const spacer = UI.el('div', { attrs: { style: 'width:36px;' } });
    topBar.append(collapseBtn, topCenter, spacer);

    // Album Cover
    const coverWrap = UI.el('div', { class: 'fp-cover-wrap' });
    const fullCover = UI.el('img', {
      class: 'fp-cover',
      attrs: { id: 'img-full-cover', src: '', alt: 'Full Cover Art' }
    });
    coverWrap.appendChild(fullCover);

    // Title, Artist, Offline Tag, Like & Download
    const metaRow = UI.el('div', { class: 'fp-meta-row' });
    const metaText = UI.el('div', { class: 'fp-meta-text' });
    const fullTitle = UI.el('div', { class: 'fp-title', text: '', attrs: { id: 'text-full-title' } });
    const artistRow = UI.el('div', { class: 'fp-artist-row' });
    const fullArtist = UI.el('div', { class: 'fp-artist', text: '', attrs: { id: 'text-full-artist' } });
    const offlineBadge = UI.el('span', {
      class: 'fp-offline-badge',
      text: 'OFFLINE',
      attrs: { id: 'tag-offline', style: 'display:none;' }
    });
    artistRow.append(fullArtist, offlineBadge);
    metaText.append(fullTitle, artistRow);

    const actions = UI.el('div', { class: 'fp-actions' });
    const fullLikeBtn = UI.el('button', {
      class: 'icon-btn like-btn',
      attrs: { id: 'btn-full-like', 'aria-label': 'Like Song' },
      html: UI.icons.starOff
    });
    const fullDlBtn = UI.el('button', {
      class: 'icon-btn download-btn',
      attrs: { id: 'btn-full-download', 'aria-label': 'Download Song' },
      html: UI.icons.downloadSolid
    });
    actions.append(fullLikeBtn, fullDlBtn);
    metaRow.append(metaText, actions);

    // Seeker Slider & Timers
    const seekerWrap = UI.el('div', { class: 'fp-seeker-wrap' });
    const seeker = UI.el('input', {
      class: 'fp-seeker',
      attrs: { id: 'seeker-full-progress', type: 'range', min: '0', max: '1000', value: '0' }
    });
    const timers = UI.el('div', { class: 'fp-timers' });
    timers.append(
      UI.el('span', { text: '00:00', attrs: { id: 'text-full-position' } }),
      UI.el('span', { text: '03:30', attrs: { id: 'text-full-duration' } })
    );
    seekerWrap.append(seeker, timers);

    // Playback Controls
    const controls = UI.el('div', { class: 'fp-controls' });
    const shuffleBtn = UI.el('button', {
      class: 'fp-ctrl-btn',
      attrs: { id: 'btn-full-shuffle', 'aria-label': 'Shuffle' },
      html: UI.icons.shuffle
    });
    const prevBtn = UI.el('button', {
      class: 'fp-ctrl-btn primary-tint skip',
      attrs: { id: 'btn-full-prev', 'aria-label': 'Previous' },
      html: UI.icons.prev
    });
    const playCircle = UI.el('button', {
      class: 'fp-play-circle',
      attrs: { id: 'btn-full-play-pause', 'aria-label': 'Play/Pause' },
      html: UI.icons.play
    });
    const nextBtn = UI.el('button', {
      class: 'fp-ctrl-btn primary-tint skip',
      attrs: { id: 'btn-full-next', 'aria-label': 'Next' },
      html: UI.icons.next
    });
    const repeatBtn = UI.el('button', {
      class: 'fp-ctrl-btn',
      attrs: { id: 'btn-full-repeat', 'aria-label': 'Repeat' },
      html: UI.icons.repeat
    });
    controls.append(shuffleBtn, prevBtn, playCircle, nextBtn, repeatBtn);

    dialog.append(topBar, coverWrap, metaRow, seekerWrap, controls);
  }

  function showFullPlayer() {
    if (!Player.currentSong()) return;
    const dialog = document.getElementById('full-player-dialog');
    if (dialog) {
      dialog.classList.add('open');
      dialog.setAttribute('aria-hidden', 'false');
      syncUi();
    }
  }

  function hideFullPlayer() {
    const dialog = document.getElementById('full-player-dialog');
    if (dialog) {
      dialog.classList.remove('open');
      dialog.setAttribute('aria-hidden', 'true');
    }
  }

  function wireEvents() {
    const mini = document.getElementById('mini-player-layout');
    mini.addEventListener('click', () => showFullPlayer());

    document.getElementById('btn-mini-play-pause').addEventListener('click', (e) => {
      e.stopPropagation();
      Player.togglePlay();
    });
    document.getElementById('btn-mini-next').addEventListener('click', (e) => {
      e.stopPropagation();
      Player.next();
    });

    document.getElementById('btn-collapse').addEventListener('click', hideFullPlayer);
    document.getElementById('btn-full-play-pause').addEventListener('click', () => Player.togglePlay());
    document.getElementById('btn-full-prev').addEventListener('click', () => Player.prev());
    document.getElementById('btn-full-next').addEventListener('click', () => Player.next());

    document.getElementById('btn-full-shuffle').addEventListener('click', () => {
      Player.toggleShuffle();
      syncUi();
    });
    document.getElementById('btn-full-repeat').addEventListener('click', () => {
      Player.cycleRepeat();
      syncUi();
    });

    document.getElementById('btn-full-like').addEventListener('click', () => {
      const song = Player.currentSong();
      if (!song) return;
      Storage.toggleLiked(song);
      syncUi();
    });

    document.getElementById('btn-full-download').addEventListener('click', () => {
      const song = Player.currentSong();
      if (!song) return;
      const nowDl = Storage.toggleDownload(song);
      UI.toast(nowDl ? 'Saved to Offline Library' : 'Removed from Offline Library');
      syncUi();
    });

    document.getElementById('seeker-full-progress').addEventListener('input', (e) => {
      const state = Player.getState();
      if (state.duration > 0) {
        const targetSec = (Number(e.target.value) / 1000) * state.duration;
        Player.seekTo(targetSec);
      }
    });

    Player.onEvent((type) => {
      if (type === 'trackchange' || type === 'play' || type === 'pause' || type === 'shufflechange' || type === 'repeatchange') {
        syncUi();
      }
      if (type === 'ready') startProgressLoop();
    });

    Storage.onChange(() => syncUi());
  }

  function syncUi() {
    const song = Player.currentSong();
    const mini = document.getElementById('mini-player-layout');
    if (!song) {
      mini.classList.add('hidden');
      return;
    }

    mini.classList.remove('hidden');
    const coverSrc = song.coverUrl || song.thumbnail || '';
    const genreSuffix = song.genre ? ` • ${song.genre}` : '';
    const playing = Player.isPlaying();

    document.getElementById('img-mini-cover').src = coverSrc;
    document.getElementById('text-mini-title').textContent = song.title;
    document.getElementById('text-mini-artist').textContent = `${song.artist}${genreSuffix}`;
    document.getElementById('btn-mini-play-pause').innerHTML = playing ? UI.icons.pause : UI.icons.play;

    // Update playing highlight in song lists
    document.querySelectorAll('.song-row').forEach(row => {
      row.classList.toggle('is-playing', row.getAttribute('data-id') === song.id);
    });

    // Full Player Dialog updates
    document.getElementById('img-full-cover').src = coverSrc;
    document.getElementById('text-full-title').textContent = song.title;
    document.getElementById('text-full-artist').textContent = song.artist;
    document.getElementById('text-full-album').textContent = song.album || 'Melofy Player';
    document.getElementById('btn-full-play-pause').innerHTML = playing ? UI.icons.pause : UI.icons.play;

    const settings = Storage.getSettings();
    document.getElementById('btn-full-shuffle').classList.toggle('active', !!settings.shuffle);
    document.getElementById('btn-full-repeat').classList.toggle('active', (settings.repeat || 'off') !== 'off');

    const liked = Storage.isLiked(song.id);
    const fullLike = document.getElementById('btn-full-like');
    fullLike.innerHTML = liked ? UI.icons.starOn : UI.icons.starOff;
    fullLike.classList.toggle('active', liked);

    const downloaded = Storage.isDownloaded(song.id);
    const fullDl = document.getElementById('btn-full-download');
    const offlineTag = document.getElementById('tag-offline');
    if (downloaded) {
      offlineTag.style.display = 'inline-block';
      fullDl.innerHTML = UI.icons.downloadDoneSolid;
      fullDl.classList.add('downloaded');
    } else {
      offlineTag.style.display = 'none';
      fullDl.innerHTML = UI.icons.downloadSolid;
      fullDl.classList.remove('downloaded');
    }
  }

  function startProgressLoop() {
    if (progressTimer) clearInterval(progressTimer);
    progressTimer = setInterval(() => {
      const state = Player.getState();
      if (!state.duration) return;
      const pct1000 = Math.min(1000, Math.max(0, (state.position / state.duration) * 1000));
      const miniFill = document.getElementById('progress-mini-player');
      if (miniFill) miniFill.style.width = `${pct1000 / 10}%`;

      const seeker = document.getElementById('seeker-full-progress');
      if (seeker && document.activeElement !== seeker) {
        seeker.value = String(Math.round(pct1000));
      }
      const posEl = document.getElementById('text-full-position');
      const durEl = document.getElementById('text-full-duration');
      if (posEl) posEl.textContent = UI.formatTime(state.position);
      if (durEl) durEl.textContent = UI.formatTime(state.duration);
    }, 300);
  }

  return { render, showFullPlayer, hideFullPlayer };
})();

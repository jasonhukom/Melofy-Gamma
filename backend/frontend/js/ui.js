/* ui.js — shared DOM helpers, Android-matched vector icons, and reusable track cards / rows. */
const UI = (() => {
  function el(tag, opts = {}) {
    const node = document.createElement(tag);
    if (opts.class) node.className = opts.class;
    if (opts.text !== undefined) node.textContent = opts.text;
    if (opts.html !== undefined) node.innerHTML = opts.html;
    if (opts.attrs) Object.entries(opts.attrs).forEach(([k, v]) => node.setAttribute(k, v));
    if (opts.children) opts.children.forEach(c => node.appendChild(c));
    return node;
  }

  const icons = {
    house: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>',
    trending: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="6.7" cy="18.7" r="2.9"/><circle cx="17.3" cy="16.3" r="2.9"/><rect x="7.8" y="5.2" width="1.9" height="13.5"/><rect x="18.4" y="2.8" width="1.9" height="13.5"/><polygon points="7.8,5.2 20.3,2.8 20.3,5.0 7.8,7.4"/><polygon points="7.8,8.3 20.3,5.9 20.3,7.9 7.8,10.3"/></svg>',
    downloadSolid: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>',
    downloadDoneSolid: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 18h14v2H5v-2zM19 9l-1.41-1.41L11 14.17l-3.59-3.58L6 12l5 5 8-8z"/></svg>',
    anonymousPfp: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>',
    play: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>',
    pause: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 5h4v14H6zm8 0h4v14h-4z"/></svg>',
    next: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>',
    prev: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>',
    starOff: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    starOn: '<svg viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    heart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 21s-7-4.35-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 6c-2.5 4.65-9.5 9-9.5 9z"/></svg>',
    heartFilled: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21s-7-4.35-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 6c-2.5 4.65-9.5 9-9.5 9z"/></svg>',
    shuffle: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6h3l11 12h4M14 6h5v5M3 18h3l4-5"/></svg>',
    repeat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 2l4 4-4 4M3 11V9a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v2a4 4 0 0 1-4 4H3"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
    collapseDown: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>'
  };

  function formatTime(sec) {
    if (!sec || isNaN(sec) || sec < 0) return '00:00';
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = Math.floor(sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  function debounce(fn, delay = 400) {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
  }

  function toast(message) {
    const root = document.getElementById('toast-root');
    if (!root) return;
    const t = el('div', { class: 'toast', text: message });
    root.appendChild(t);
    requestAnimationFrame(() => t.classList.add('show'));
    setTimeout(() => { t.classList.remove('show'); setTimeout(() => t.remove(), 250); }, 2200);
  }

  function trackCard(song, opts = {}) {
    const card = el('div', { class: 'track-card', attrs: { 'data-id': song.id } });
    const cover = el('img', {
      class: 'track-card-cover',
      attrs: { src: song.coverUrl || song.thumbnail || '', alt: song.title, loading: 'lazy' }
    });
    const title = el('div', { class: 'track-card-title', text: song.title });
    const artist = el('div', { class: 'track-card-artist', text: song.artist });
    card.append(cover, title, artist);
    card.addEventListener('click', () => Player.playNow(song, opts.context || null));
    return card;
  }

  function songRow(song, opts = {}) {
    const current = Player.currentSong();
    const isPlayingThis = current && current.id === song.id;
    const row = el('div', {
      class: 'song-row' + (isPlayingThis ? ' is-playing' : ''),
      attrs: { 'data-id': song.id }
    });

    if (opts.showRank) {
      const rankNum = song.rank || opts.rankNumber || 1;
      row.appendChild(el('div', { class: 'song-rank', text: `#${rankNum}` }));
    }

    const thumb = el('img', {
      class: 'song-thumb',
      attrs: { src: song.coverUrl || song.thumbnail || '', alt: song.title, loading: 'lazy' }
    });

    const info = el('div', { class: 'song-info' });
    const title = el('div', { class: 'song-title', text: song.title });
    const metaRow = el('div', { class: 'song-meta-row' });
    const genreSuffix = song.genre ? ` • ${song.genre}` : '';
    const artist = el('div', { class: 'song-artist', text: `${song.artist}${genreSuffix}` });
    const offlineTag = el('span', {
      class: 'song-offline-tag',
      text: '• Offline'
    });
    const downloadedInit = Storage.isDownloaded(song.id);
    offlineTag.style.display = downloadedInit ? 'inline' : 'none';
    metaRow.append(artist, offlineTag);
    info.append(title, metaRow);

    const actions = el('div', { class: 'song-actions' });

    // Like Button (matching Android TrackAdapter btnLike)
    const likeBtn = el('button', { class: 'icon-btn like-btn', attrs: { 'aria-label': 'Like Song' } });
    const likedInit = Storage.isLiked(song.id);
    likeBtn.innerHTML = likedInit ? icons.starOn : icons.starOff;
    likeBtn.classList.toggle('active', likedInit);
    likeBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const nowLiked = Storage.toggleLiked(song);
      likeBtn.innerHTML = nowLiked ? icons.starOn : icons.starOff;
      likeBtn.classList.toggle('active', nowLiked);
    });

    // Download Button (matching Android TrackAdapter btnDownload)
    const dlBtn = el('button', {
      class: 'icon-btn download-btn' + (downloadedInit ? ' downloaded' : ''),
      attrs: { 'aria-label': 'Download Song' }
    });
    dlBtn.innerHTML = downloadedInit ? icons.downloadDoneSolid : icons.downloadSolid;
    dlBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (opts.onDownloadClick) {
        opts.onDownloadClick(song);
        return;
      }
      const nowDl = Storage.toggleDownload(song);
      dlBtn.innerHTML = nowDl ? icons.downloadDoneSolid : icons.downloadSolid;
      dlBtn.classList.toggle('downloaded', nowDl);
      offlineTag.style.display = nowDl ? 'inline' : 'none';
      toast(nowDl ? 'Saved to Offline Library' : 'Removed from Offline Library');
    });

    actions.append(likeBtn, dlBtn);
    row.append(thumb, info, actions);
    row.addEventListener('click', () => Player.playNow(song, opts.context || null));
    return row;
  }

  return { el, icons, formatTime, debounce, toast, trackCard, songRow };
})();

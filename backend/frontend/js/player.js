/* player.js — wraps the official YouTube IFrame Player API. This is the
   legitimate, documented way to embed YouTube playback in a page:
   https://developers.google.com/youtube/iframe_api_reference
   It does not (and cannot) strip YouTube's own ads — those are controlled
   by YouTube/content owners, not by the embedding page. */
const Player = (() => {
  let ytPlayer = null;
  let queue = [];
  let queueIndex = -1;
  let isReady = false;
  let pendingPlay = false;
  let consecutiveErrors = 0;
  let listeners = [];

  function onEvent(cb) { listeners.push(cb); }
  function emit(type, data) { listeners.forEach(cb => { try { cb(type, data); } catch (e) {} }); }

  function updateMediaSession(song, state) {
    if (!('mediaSession' in navigator)) return;
    try {
      if (song && typeof MediaMetadata !== 'undefined') {
        navigator.mediaSession.metadata = new MediaMetadata({
          title: song.title || 'Lyra Melofy',
          artist: song.artist || 'YouTube Music',
          album: 'Lyra Melofy',
          artwork: song.thumbnail ? [{ src: song.thumbnail, sizes: '512x512', type: 'image/jpeg' }] : []
        });
      }
      if (state) navigator.mediaSession.playbackState = state;
    } catch (e) {}
  }

  function registerMediaSessionHandlers() {
    if (!('mediaSession' in navigator)) return;
    const safeHandle = (action, fn) => {
      try { navigator.mediaSession.setActionHandler(action, fn); } catch (e) {}
    };
    safeHandle('play', () => togglePlay());
    safeHandle('pause', () => togglePlay());
    safeHandle('previoustrack', () => prev());
    safeHandle('nexttrack', () => next());
    safeHandle('seekto', (details) => {
      if (details && typeof details.seekTime === 'number') seekTo(details.seekTime);
    });
  }

  function loadYouTubeAPI() {
    return new Promise((resolve) => {
      if (window.YT && window.YT.Player) { resolve(); return; }
      const prevCallback = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => { if (prevCallback) prevCallback(); resolve(); };
      if (!document.querySelector('script[src="https://www.youtube.com/iframe_api"]')) {
        const tag = document.createElement('script');
        tag.src = 'https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
      }
    });
  }

  async function init(containerId) {
    registerMediaSessionHandlers();
    await loadYouTubeAPI();
    const playerVars = {
      autoplay: 0,
      controls: 0,
      disablekb: 1,
      modestbranding: 1,
      rel: 0,
      playsinline: 1,
      enablejsapi: 1
    };
    if (window.location && /^https?:/.test(window.location.origin)) {
      playerVars.origin = window.location.origin;
      playerVars.widget_referrer = window.location.href;
    }
    ytPlayer = new YT.Player(containerId, {
      height: '100%',
      width: '100%',
      playerVars,
      events: { onReady: handleReady, onStateChange: handleStateChange, onError: handleError }
    });
  }

  function handleReady() {
    isReady = true;
    const settings = Storage.getSettings();
    ytPlayer.setVolume(settings.volume ?? 80);

    if (pendingPlay && currentSong()) {
      pendingPlay = false;
      playCurrent();
    } else {
      const saved = Storage.getPlayerState();
      if (saved && saved.videoId && saved.queue && saved.queue.length) {
        queue = saved.queue;
        queueIndex = saved.queueIndex ?? 0;
        ytPlayer.cueVideoById(saved.videoId, saved.positionSeconds || 0);
        const restored = currentSong();
        emit('trackchange', restored);
        updateMediaSession(restored, 'paused');
        if (saved.isPlaying) {
          // Resuming playback after a page navigation. Browsers may block
          // autoplay here until the user interacts with the new page — if so
          // the bar will simply show a paused state, which is expected.
          setTimeout(() => {
            try { ytPlayer.seekTo(saved.positionSeconds || 0, true); ytPlayer.playVideo(); } catch (e) {}
          }, 300);
        }
      }
    }
    setInterval(savePlayerState, 3000);
    window.addEventListener('beforeunload', savePlayerState);
    emit('ready');
  }

  function handleStateChange(e) {
    const S = window.YT.PlayerState;
    if (e.data === S.PLAYING) {
      consecutiveErrors = 0;
      updateMediaSession(currentSong(), 'playing');
      emit('play');
    }
    if (e.data === S.PAUSED) {
      updateMediaSession(currentSong(), 'paused');
      emit('pause');
    }
    if (e.data === S.ENDED) handleEnded();
    if (e.data === S.PLAYING || e.data === S.PAUSED) savePlayerState();
  }

  function handleError(e) {
    const code = e && typeof e.data !== 'undefined' ? e.data : null;
    consecutiveErrors++;
    emit('error', { code, song: currentSong() });

    const settings = Storage.getSettings();
    const canAdvance = queue.length > 1 &&
      consecutiveErrors < Math.min(queue.length, 5) &&
      (queueIndex < queue.length - 1 || settings.repeat === 'all');

    if (canAdvance) {
      if (window.UI) window.UI.toast('Track unavailable — skipping to next');
      setTimeout(() => next(), 900);
    } else {
      emit('pause');
      if (window.UI) window.UI.toast('Unable to play this track — try another song');
    }
  }

  function handleEnded() {
    const settings = Storage.getSettings();
    if (settings.repeat === 'one') { ytPlayer.seekTo(0, true); ytPlayer.playVideo(); return; }
    next();
  }

  function currentSong() { return queue[queueIndex] || null; }

  function playQueue(songs, startIndex = 0) {
    if (!songs || !songs.length) return;
    queue = songs.slice();
    queueIndex = startIndex;
    consecutiveErrors = 0;
    if (Storage.getSettings().shuffle) shuffleQueue(true);
    playCurrent();
  }

  function playCurrent() {
    const song = currentSong();
    if (!song) return;
    Storage.addRecent(song);
    emit('trackchange', song);
    updateMediaSession(song, 'playing');
    if (!ytPlayer || !isReady) {
      pendingPlay = true;
      return;
    }
    pendingPlay = false;
    ytPlayer.loadVideoById(song.id);
    savePlayerState();
  }

  function playNow(song, contextList) {
    if (contextList && contextList.length) {
      const idx = contextList.findIndex(s => s.id === song.id);
      playQueue(contextList, idx >= 0 ? idx : 0);
    } else {
      playQueue([song], 0);
    }
  }

  function next() {
    if (!queue.length) return;
    const settings = Storage.getSettings();
    if (queueIndex < queue.length - 1) queueIndex++;
    else if (settings.repeat === 'all') queueIndex = 0;
    else { emit('pause'); return; }
    playCurrent();
  }

  function prev() {
    if (!queue.length || !ytPlayer) return;
    if (ytPlayer.getCurrentTime() > 3) { ytPlayer.seekTo(0, true); return; }
    if (queueIndex > 0) queueIndex--;
    playCurrent();
  }

  function togglePlay() {
    if (!ytPlayer) return;
    const state = ytPlayer.getPlayerState();
    if (state === YT.PlayerState.PLAYING) ytPlayer.pauseVideo();
    else ytPlayer.playVideo();
  }

  function seekTo(seconds) { ytPlayer && ytPlayer.seekTo(seconds, true); }

  function setVolume(v) { ytPlayer && ytPlayer.setVolume(v); Storage.updateSettings({ volume: v }); }

  function shuffleQueue(keepCurrentFirst) {
    const current = queue[queueIndex];
    const rest = queue.filter((_, i) => i !== queueIndex);
    for (let i = rest.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [rest[i], rest[j]] = [rest[j], rest[i]];
    }
    queue = keepCurrentFirst && current ? [current, ...rest] : rest;
    queueIndex = 0;
  }

  function toggleShuffle() {
    const next = !Storage.getSettings().shuffle;
    Storage.updateSettings({ shuffle: next });
    if (next) shuffleQueue(true);
    emit('shufflechange', next);
    return next;
  }

  function cycleRepeat() {
    const order = ['off', 'all', 'one'];
    const mode = order[(order.indexOf(Storage.getSettings().repeat || 'off') + 1) % order.length];
    Storage.updateSettings({ repeat: mode });
    emit('repeatchange', mode);
    return mode;
  }

  function savePlayerState() {
    if (!ytPlayer || !isReady) return;
    try {
      Storage.setPlayerState({
        videoId: currentSong()?.id,
        positionSeconds: ytPlayer.getCurrentTime ? ytPlayer.getCurrentTime() : 0,
        isPlaying: ytPlayer.getPlayerState() === YT.PlayerState.PLAYING,
        queue, queueIndex, savedAt: Date.now()
      });
    } catch (e) {}
  }

  function getState() {
    return {
      isReady,
      queue, queueIndex,
      duration: (ytPlayer && ytPlayer.getDuration) ? ytPlayer.getDuration() : 0,
      position: (ytPlayer && ytPlayer.getCurrentTime) ? ytPlayer.getCurrentTime() : 0
    };
  }

  return {
    init, onEvent, playNow, playQueue, next, prev, togglePlay,
    seekTo, setVolume, toggleShuffle, cycleRepeat, currentSong, getState
  };
})();
window.Player = Player;

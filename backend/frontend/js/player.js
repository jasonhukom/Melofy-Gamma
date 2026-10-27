/* player.js — unified playback engine supporting both HTML5 Audio (for Melofy sample/offline tracks)
   and YouTube IFrame Player API (for YouTube tracks). */
const Player = (() => {
  let ytPlayer = null;
  let ytReady = false;
  let htmlAudio = new Audio();
  let activeEngine = 'audio'; // 'audio' | 'yt'

  let queue = [];
  let queueIndex = -1;
  let isPlayingState = false;
  let listeners = [];

  function onEvent(cb) { listeners.push(cb); }
  function emit(type, data) { listeners.forEach(cb => { try { cb(type, data); } catch (e) {} }); }

  function setupHtmlAudio() {
    htmlAudio.preload = 'metadata';
    htmlAudio.addEventListener('play', () => {
      if (activeEngine === 'audio') {
        isPlayingState = true;
        emit('play');
      }
    });
    htmlAudio.addEventListener('pause', () => {
      if (activeEngine === 'audio') {
        isPlayingState = false;
        emit('pause');
      }
    });
    htmlAudio.addEventListener('ended', () => {
      if (activeEngine === 'audio') handleEnded();
    });
    htmlAudio.addEventListener('error', () => {
      if (activeEngine === 'audio') {
        emit('error');
        setTimeout(() => next(), 1200);
      }
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
    setupHtmlAudio();
    const settings = Storage.getSettings();
    htmlAudio.volume = (settings.volume ?? 80) / 100;

    emit('ready');

    try {
      await loadYouTubeAPI();
      ytPlayer = new YT.Player(containerId, {
        height: '100%',
        width: '100%',
        playerVars: { autoplay: 0, controls: 0, disablekb: 1, modestbranding: 1, rel: 0, playsinline: 1 },
        events: {
          onReady: () => {
            ytReady = true;
            ytPlayer.setVolume(Storage.getSettings().volume ?? 80);
          },
          onStateChange: (e) => {
            if (activeEngine !== 'yt') return;
            const S = window.YT.PlayerState;
            if (e.data === S.PLAYING) { isPlayingState = true; emit('play'); }
            if (e.data === S.PAUSED) { isPlayingState = false; emit('pause'); }
            if (e.data === S.ENDED) handleEnded();
          },
          onError: () => {
            if (activeEngine === 'yt') {
              emit('error');
              setTimeout(() => next(), 1200);
            }
          }
        }
      });
    } catch (e) {}
  }

  function handleEnded() {
    const settings = Storage.getSettings();
    if (settings.repeat === 'one') {
      seekTo(0);
      if (activeEngine === 'audio') htmlAudio.play().catch(() => {});
      else if (ytPlayer && ytReady) ytPlayer.playVideo();
      return;
    }
    next();
  }

  function currentSong() { return queue[queueIndex] || null; }

  function isPlaying() { return isPlayingState; }

  function playQueue(songs, startIndex = 0) {
    if (!songs || !songs.length) return;
    queue = songs.slice();
    queueIndex = startIndex;
    if (Storage.getSettings().shuffle) shuffleQueue(true);
    playCurrent();
  }

  function playCurrent() {
    const song = currentSong();
    if (!song) return;

    if (song.audioUrl) {
      // Stop YouTube if running
      if (ytPlayer && ytReady && ytPlayer.pauseVideo) {
        try { ytPlayer.pauseVideo(); } catch (e) {}
      }
      activeEngine = 'audio';
      htmlAudio.src = song.audioUrl;
      htmlAudio.play().catch(() => {});
    } else {
      // Stop HTML5 Audio if running
      htmlAudio.pause();
      activeEngine = 'yt';
      if (ytPlayer && ytReady) {
        ytPlayer.loadVideoById(song.id);
      }
    }

    Storage.addRecent(song);
    emit('trackchange', song);
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
    if (queueIndex < queue.length - 1) queueIndex++;
    else queueIndex = 0;
    playCurrent();
  }

  function prev() {
    if (!queue.length) return;
    const state = getState();
    if (state.position > 3) { seekTo(0); return; }
    if (queueIndex > 0) queueIndex--;
    else queueIndex = queue.length - 1;
    playCurrent();
  }

  function togglePlay() {
    const song = currentSong();
    if (!song) {
      const samples = Storage.getSampleTracks();
      if (samples.length) playQueue(samples, 0);
      return;
    }
    if (activeEngine === 'audio') {
      if (htmlAudio.paused) htmlAudio.play().catch(() => {});
      else htmlAudio.pause();
    } else if (ytPlayer && ytReady) {
      const st = ytPlayer.getPlayerState();
      if (st === YT.PlayerState.PLAYING) ytPlayer.pauseVideo();
      else ytPlayer.playVideo();
    }
  }

  function seekTo(seconds) {
    if (activeEngine === 'audio') {
      htmlAudio.currentTime = seconds;
    } else if (ytPlayer && ytReady) {
      ytPlayer.seekTo(seconds, true);
    }
  }

  function setVolume(v) {
    htmlAudio.volume = Math.max(0, Math.min(1, v / 100));
    if (ytPlayer && ytReady) ytPlayer.setVolume(v);
    Storage.updateSettings({ volume: v });
  }

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
    const nextVal = !Storage.getSettings().shuffle;
    Storage.updateSettings({ shuffle: nextVal });
    if (nextVal && queue.length) shuffleQueue(true);
    emit('shufflechange', nextVal);
    return nextVal;
  }

  function cycleRepeat() {
    const current = Storage.getSettings().repeat || 'off';
    const nextMode = current === 'off' ? 'one' : 'off';
    Storage.updateSettings({ repeat: nextMode });
    emit('repeatchange', nextMode);
    return nextMode;
  }

  function getState() {
    const song = currentSong();
    if (activeEngine === 'audio') {
      const dur = (htmlAudio.duration && !isNaN(htmlAudio.duration))
        ? htmlAudio.duration
        : (song && song.durationMs ? song.durationMs / 1000 : 0);
      return {
        isReady: true,
        queue,
        queueIndex,
        duration: dur,
        position: htmlAudio.currentTime || 0
      };
    }
    return {
      isReady: ytReady,
      queue,
      queueIndex,
      duration: (ytPlayer && ytPlayer.getDuration) ? ytPlayer.getDuration() : 0,
      position: (ytPlayer && ytPlayer.getCurrentTime) ? ytPlayer.getCurrentTime() : 0
    };
  }

  return {
    init, onEvent, playNow, playQueue, next, prev, togglePlay,
    seekTo, setVolume, toggleShuffle, cycleRepeat, currentSong, isPlaying, getState
  };
})();

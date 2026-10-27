/* storage.js — local persistence + built-in Melofy tracks matching Android TrackRepository.java */
const Storage = (() => {
  const KEYS = {
    liked: 'melofy:liked',
    downloads: 'melofy:downloads',
    playlists: 'melofy:playlists',
    recent: 'melofy:recent',
    settings: 'melofy:settings',
    playerState: 'melofy:playerState'
  };

  const SAMPLE_TRACKS = [
    {
      id: 'melofy_1',
      title: 'Midnight Horizon',
      artist: 'Lofi Sunset',
      album: 'Chill Vibes Vol. 1',
      thumbnail: 'https://picsum.photos/id/1025/400/400',
      coverUrl: 'https://picsum.photos/id/1025/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      durationMs: 372000,
      genre: 'Lo-Fi',
      playCount: 1420500,
      rank: 1
    },
    {
      id: 'melofy_2',
      title: 'Neon Synthwave Drift',
      artist: 'Aetherium',
      album: 'Cyber Dreams',
      thumbnail: 'https://picsum.photos/id/1084/400/400',
      coverUrl: 'https://picsum.photos/id/1084/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      durationMs: 423000,
      genre: 'Electronic',
      playCount: 982100,
      rank: 2
    },
    {
      id: 'melofy_3',
      title: 'Acoustic Morning Coffee',
      artist: 'Luna & The Pines',
      album: 'Unplugged Sessions',
      thumbnail: 'https://picsum.photos/id/1062/400/400',
      coverUrl: 'https://picsum.photos/id/1062/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
      durationMs: 344000,
      genre: 'Pop',
      playCount: 850300,
      rank: 3
    },
    {
      id: 'melofy_4',
      title: 'Ethereal Echoes',
      artist: 'Solaris Wave',
      album: 'Starlight Odyssey',
      thumbnail: 'https://picsum.photos/id/1069/400/400',
      coverUrl: 'https://picsum.photos/id/1069/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',
      durationMs: 302000,
      genre: 'Ambient',
      playCount: 740120,
      rank: 4
    },
    {
      id: 'melofy_5',
      title: 'Urban Rhythm Groove',
      artist: 'Metro Beatmakers',
      album: 'City Lights',
      thumbnail: 'https://picsum.photos/id/1074/400/400',
      coverUrl: 'https://picsum.photos/id/1074/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',
      durationMs: 360000,
      genre: 'Hip-Hop',
      playCount: 620400,
      rank: 5
    },
    {
      id: 'melofy_6',
      title: 'Velvet Afterdark',
      artist: 'Kira Sound',
      album: 'Smooth Midnight',
      thumbnail: 'https://picsum.photos/id/1080/400/400',
      coverUrl: 'https://picsum.photos/id/1080/400/400',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',
      durationMs: 285000,
      genre: 'R&B',
      playCount: 512000,
      rank: 6
    }
  ];

  const listeners = [];
  function onChange(cb) { listeners.push(cb); }
  function notify() { listeners.forEach(cb => { try { cb(); } catch (e) {} }); }

  function read(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) { return fallback; }
  }
  function write(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) {}
  }

  function getSampleTracks() {
    return SAMPLE_TRACKS.map(t => ({
      ...t,
      isLiked: isLiked(t.id),
      isDownloaded: isDownloaded(t.id)
    }));
  }

  // Liked songs
  function getLiked() { return read(KEYS.liked, []); }
  function isLiked(id) { return getLiked().some(s => s.id === id); }
  function toggleLiked(song) {
    const liked = getLiked();
    const idx = liked.findIndex(s => s.id === song.id);
    let nowLiked;
    if (idx >= 0) {
      liked.splice(idx, 1);
      nowLiked = false;
    } else {
      liked.unshift(song);
      nowLiked = true;
    }
    write(KEYS.liked, liked);
    notify();
    return nowLiked;
  }

  // Downloaded / Offline saved tracks
  function getDownloaded() { return read(KEYS.downloads, []); }
  function isDownloaded(id) { return getDownloaded().some(s => s.id === id); }
  function toggleDownload(song) {
    const downloads = getDownloaded();
    const idx = downloads.findIndex(s => s.id === song.id);
    let nowDownloaded;
    if (idx >= 0) {
      downloads.splice(idx, 1);
      nowDownloaded = false;
    } else {
      downloads.unshift({ ...song, isDownloaded: true });
      nowDownloaded = true;
    }
    write(KEYS.downloads, downloads);
    notify();
    return nowDownloaded;
  }
  function addDownload(song) {
    if (!isDownloaded(song.id)) {
      const downloads = getDownloaded();
      downloads.unshift({ ...song, isDownloaded: true });
      write(KEYS.downloads, downloads);
      notify();
    }
  }
  function removeDownload(songId) {
    const downloads = getDownloaded().filter(s => s.id !== songId);
    write(KEYS.downloads, downloads);
    notify();
  }

  // Playlists
  function getPlaylists() { return read(KEYS.playlists, []); }
  function getPlaylist(id) { return getPlaylists().find(p => p.id === id) || null; }
  function createPlaylist(name) {
    const playlists = getPlaylists();
    const pl = { id: 'pl_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6), name, createdAt: Date.now(), songs: [] };
    playlists.unshift(pl);
    write(KEYS.playlists, playlists);
    notify();
    return pl;
  }
  function deletePlaylist(id) {
    write(KEYS.playlists, getPlaylists().filter(p => p.id !== id));
    notify();
  }
  function addToPlaylist(id, song) {
    const playlists = getPlaylists();
    const pl = playlists.find(p => p.id === id);
    if (!pl) return;
    if (!pl.songs.some(s => s.id === song.id)) pl.songs.push(song);
    write(KEYS.playlists, playlists);
    notify();
  }
  function removeFromPlaylist(id, songId) {
    const playlists = getPlaylists();
    const pl = playlists.find(p => p.id === id);
    if (!pl) return;
    pl.songs = pl.songs.filter(s => s.id !== songId);
    write(KEYS.playlists, playlists);
    notify();
  }

  // Recently played
  function getRecent() { return read(KEYS.recent, []); }
  function addRecent(song) {
    let recent = getRecent().filter(s => s.id !== song.id);
    recent.unshift(song);
    write(KEYS.recent, recent.slice(0, 24));
  }

  // Settings
  function getSettings() {
    return read(KEYS.settings, {
      apiKey: '',
      volume: 80,
      shuffle: false,
      repeat: 'off',
      highQuality: true,
      wifiOnly: true
    });
  }
  function updateSettings(patch) {
    write(KEYS.settings, { ...getSettings(), ...patch });
    notify();
  }

  function getPlayerState() { return read(KEYS.playerState, null); }
  function setPlayerState(state) { write(KEYS.playerState, state); }

  return {
    getSampleTracks,
    getLiked, isLiked, toggleLiked,
    getDownloaded, isDownloaded, toggleDownload, addDownload, removeDownload,
    getPlaylists, getPlaylist, createPlaylist, deletePlaylist, addToPlaylist, removeFromPlaylist,
    getRecent, addRecent,
    getSettings, updateSettings,
    getPlayerState, setPlayerState,
    onChange
  };
})();

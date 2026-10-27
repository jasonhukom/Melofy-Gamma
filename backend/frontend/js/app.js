/* app.js — boots the whole app: player + player bar, sidebar navigation rail,
   topbar search box, sound mixer, the router and its routes, and the account
   section. */

function renderSidebarPlaylists() {
  const container = document.getElementById('sidebar-playlists');
  if (!container) return;
  container.innerHTML = '';
  Storage.getPlaylists().forEach(pl => {
    const link = UI.el('a', {
      class: 'sidebar-playlist-link',
      attrs: { href: `#/playlist?id=${pl.id}` }
    });
    const meta = UI.el('div', { class: 'sidebar-playlist-meta' });
    meta.append(
      UI.el('span', { class: 'sidebar-playlist-title', text: pl.name }),
      UI.el('span', { class: 'sidebar-playlist-sub', text: `${pl.songs.length} song${pl.songs.length === 1 ? '' : 's'}` })
    );
    link.appendChild(meta);
    container.appendChild(link);
  });
}

function openCreatePlaylistModal() {
  const wrap = UI.el('div', {});
  wrap.appendChild(UI.el('h3', { text: 'New playlist' }));
  const input = UI.el('input', { attrs: { type: 'text', placeholder: 'Title' } });
  const createBtn = UI.el('button', { class: 'btn-primary', text: 'Create' });
  createBtn.addEventListener('click', () => {
    const name = input.value.trim() || 'My Playlist';
    const pl = Storage.createPlaylist(name);
    UI.closeModal();
    renderSidebarPlaylists();
    Router.navigate(`/playlist?id=${pl.id}`);
  });
  wrap.append(input, createBtn);
  UI.openModal(wrap);
  setTimeout(() => input.focus(), 50);
}
window.openCreatePlaylistModal = openCreatePlaylistModal;

function renderAccountSection() {
  const container = document.getElementById('sidebar-account');
  const topbarAccount = document.getElementById('topbar-account');
  const user = window.FirebaseAuth ? window.FirebaseAuth.getUser() : null;

  if (topbarAccount) {
    topbarAccount.innerHTML = '';
    if (!user) {
      const signInPill = UI.el('button', { class: 'ytm-signin-pill', text: 'Sign in' });
      signInPill.addEventListener('click', () => window.FirebaseAuth && window.FirebaseAuth.signIn());
      topbarAccount.appendChild(signInPill);
    } else {
      const initial = (user.displayName || user.email || 'U').trim().charAt(0).toUpperCase();
      const avatarBtn = UI.el('button', {
        class: 'ytm-avatar-btn',
        text: initial,
        attrs: { title: user.displayName || user.email || 'Account', 'aria-label': 'Account' }
      });
      avatarBtn.addEventListener('click', () => Router.navigate('/library'));
      topbarAccount.appendChild(avatarBtn);
    }
  }

  if (!container || !window.FirebaseAuth) return;
  container.innerHTML = '';

  if (!user) {
    const signInBtn = UI.el('button', { class: 'btn-secondary', text: 'Sign in with Google' });
    signInBtn.style.width = '100%';
    signInBtn.addEventListener('click', () => window.FirebaseAuth.signIn());
    container.appendChild(signInBtn);
    return;
  }

  container.appendChild(UI.el('div', { class: 'sidebar-account-name', text: user.displayName || user.email || 'Signed in' }));

  if (user.youtubeConnected) {
    const badge = UI.el('div', { class: 'status-badge connected' });
    badge.append(UI.el('span', { class: 'dot' }), document.createTextNode('YouTube connected'));
    container.appendChild(badge);
  } else {
    const connectBtn = UI.el('button', { class: 'btn-secondary', text: 'Connect YouTube' });
    connectBtn.style.width = '100%';
    connectBtn.addEventListener('click', () => window.FirebaseAuth.connectYouTube());
    container.appendChild(connectBtn);
  }

  const signOutBtn = UI.el('button', { class: 'sidebar-account-sub', text: 'Sign out' });
  signOutBtn.style.textAlign = 'left';
  signOutBtn.addEventListener('click', () => window.FirebaseAuth.signOutEverywhere());
  container.appendChild(signOutBtn);
}

function wireTopbarSearch() {
  const input = document.getElementById('topbar-search-input');
  const clearBtn = document.getElementById('topbar-search-clear');
  if (!input || !clearBtn) return;

  const syncClear = () => {
    clearBtn.hidden = !input.value.trim();
  };

  const triggerSearch = UI.debounce((q) => {
    if (q.length >= 2) {
      Router.navigate(`/search?q=${encodeURIComponent(q)}`);
    }
  }, 450);

  input.addEventListener('input', () => {
    syncClear();
    const q = input.value.trim();
    if (q.length >= 2) triggerSearch(q);
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const q = input.value.trim();
      if (q) Router.navigate(`/search?q=${encodeURIComponent(q)}`);
      else Router.navigate('/search');
    }
  });

  clearBtn.addEventListener('click', () => {
    input.value = '';
    syncClear();
    input.focus();
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  PlayerBar.render();
  Player.init('yt-player-mount');
  Sidebar.render();
  SoundMixer.render();
  renderSidebarPlaylists();
  wireTopbarSearch();
  renderAccountSection();

  Router.register('/', (c) => HomeView.render(c));
  Router.register('/trending', (c) => HomeView.render(c, { mode: 'explore' }));
  Router.register('/search', (c, p) => SearchView.render(c, p));
  Router.register('/library', (c) => LibraryView.render(c));
  Router.register('/profile', (c) => LibraryView.render(c));
  Router.register('/liked', (c) => LikedView.render(c));
  Router.register('/playlist', (c, p) => PlaylistView.render(c, p));
  Router.start(document.getElementById('main-content'));

  if (window.FirebaseAuth) {
    window.FirebaseAuth.onChange(() => {
      renderAccountSection();
      if (location.hash.startsWith('#/library')) Router.navigate('/library');
    });
    await window.FirebaseAuth.init();
    renderAccountSection();

    const hashQuery = new URLSearchParams((location.hash.split('?')[1]) || '');
    if (hashQuery.get('youtube') === 'connected') UI.toast('YouTube account connected');
    if (hashQuery.get('youtube') === 'error') UI.toast('Could not connect YouTube — try again');
  }
});

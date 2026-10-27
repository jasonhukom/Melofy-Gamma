/* app.js — boots the Melofy Gamma SPA with the 5-tab Android footer navigation */

function updateFooterProfileAvatar() {
  const svgIcon = document.getElementById('icon-nav-profile-svg');
  const imgIcon = document.getElementById('icon-nav-profile-img');
  if (!svgIcon || !imgIcon) return;

  const fbUser = window.FirebaseAuth ? window.FirebaseAuth.getUser() : null;
  const guestAuth = window.__melofyGuestSignedIn || false;
  const isSignedIn = !!fbUser || guestAuth;

  if (isSignedIn) {
    const photoUrl = (fbUser && fbUser.photoURL) || 'https://picsum.photos/id/64/200/200';
    imgIcon.src = photoUrl;
    imgIcon.style.display = 'block';
    svgIcon.style.display = 'none';
  } else {
    imgIcon.style.display = 'none';
    svgIcon.style.display = 'block';
  }
}
window.updateFooterProfileAvatar = updateFooterProfileAvatar;

document.addEventListener('DOMContentLoaded', async () => {
  PlayerBar.render();
  Player.init('yt-player-mount');
  Sidebar.render();
  SoundMixer.render();

  Router.register('/', (c) => HomeView.render(c));
  Router.register('/trending', (c) => TrendingView.render(c));
  Router.register('/search', (c, p) => SearchView.render(c, p));
  Router.register('/downloads', (c) => DownloadsView.render(c));
  Router.register('/library', (c) => DownloadsView.render(c));
  Router.register('/profile', (c) => ProfileView.render(c));
  Router.register('/liked', (c) => LikedView.render(c));
  Router.register('/playlist', (c, p) => PlaylistView.render(c, p));
  Router.start(document.getElementById('main-content'));

  if (window.FirebaseAuth) {
    window.FirebaseAuth.onChange(() => {
      updateFooterProfileAvatar();
      if (location.hash.startsWith('#/profile')) Router.refresh();
    });
    await window.FirebaseAuth.init();
    updateFooterProfileAvatar();
  }
});

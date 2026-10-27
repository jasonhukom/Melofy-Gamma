/* home.js — matches Android fragment_home.xml & HomeFragment.java */
const HomeView = {
  render(container) {
    // 1. Top Header with Simple House Icon & "• Offline Ready" Badge
    const topHeader = UI.el('div', { class: 'android-top-header' });
    const headerLeft = UI.el('div', { class: 'android-header-left' });
    headerLeft.append(
      UI.el('div', { class: 'android-header-icon', html: UI.icons.house }),
      UI.el('div', { class: 'android-header-title', text: 'Home' })
    );
    const badge = UI.el('div', { class: 'offline-ready-badge', text: '• Offline Ready' });
    topHeader.append(headerLeft, badge);

    // 2. Simple Clean Hero Banner
    const hero = UI.el('div', { class: 'hero-banner' });
    hero.append(
      UI.el('div', { class: 'hero-kicker', text: 'MELOFY MUSIC' }),
      UI.el('div', { class: 'hero-title', text: 'Your Home Music Hub' }),
      UI.el('div', { class: 'hero-subtitle', text: 'Listen online or offline to your saved songs.' })
    );

    // 3. Quick Picks (Horizontal Track Cards)
    const quickHeading = UI.el('h2', { class: 'section-heading', text: 'Quick Picks' });
    const quickRow = UI.el('div', { class: 'quick-picks-row' });

    const allTracks = Storage.getSampleTracks();
    const quickPicks = allTracks.slice(0, 4);
    quickPicks.forEach(track => {
      quickRow.appendChild(UI.trackCard(track, { context: allTracks }));
    });

    // 4. Recommended For You (Track Rows)
    const recHeading = UI.el('h2', { class: 'section-heading', text: 'Recommended For You' });
    const recList = UI.el('div', { class: 'song-list' });
    allTracks.forEach(track => {
      recList.appendChild(UI.songRow(track, { context: allTracks }));
    });

    container.append(topHeader, hero, quickHeading, quickRow, recHeading, recList);

    // Also fetch server random picks if YouTube API key is configured
    YouTubeAPI.randomPicks().then(songs => {
      if (songs && songs.length) {
        songs.slice(0, 6).forEach(s => {
          recList.appendChild(UI.songRow({ ...s, genre: 'YouTube' }, { context: allTracks.concat(songs) }));
        });
      }
    }).catch(() => {});
  }
};

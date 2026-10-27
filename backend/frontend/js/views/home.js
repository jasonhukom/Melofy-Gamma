const HomeView = {
  render(container, opts = {}) {
    const isExplore = opts.mode === 'explore';

    const moods = [
      ['All', ''],
      ['Relax', 'chill lofi relax'],
      ['Energize', 'upbeat pop dance hits'],
      ['Workout', 'workout phonk gym music'],
      ['Focus', 'instrumental study ambient'],
      ['Commute', 'indie pop road trip'],
      ['Pop', 'pop hits'],
      ['Hip-Hop', 'hip hop hits'],
      ['Rock', 'rock classics'],
      ['Electronic', 'electronic dance'],
      ['R&B', 'r&b soul'],
      ['K-Pop', 'k-pop hits'],
      ['Jazz', 'jazz lounge']
    ];

    const chipRow = UI.el('section', { class: 'chip-row' });

    // Quick Picks Section (3-column YouTube Music grid)
    const quickPicksSection = UI.el('section', {});
    const qpHeader = UI.el('div', { class: 'ytm-section-header' });
    const qpTitleWrap = UI.el('div', {});
    qpTitleWrap.append(
      UI.el('div', { class: 'section-kicker', text: isExplore ? 'CHARTS & TRENDING' : 'START RADIO FROM A SONG' }),
      UI.el('h2', { class: 'section-heading', text: isExplore ? 'Trending tracks' : 'Quick picks' })
    );
    const playAllBtn = UI.el('button', { class: 'ytm-pill-outline-btn', text: 'Play all' });
    qpHeader.append(qpTitleWrap, playAllBtn);

    const quickPicksGrid = UI.el('div', { class: 'ytm-quick-picks-grid' });
    quickPicksGrid.appendChild(UI.el('p', { class: 'empty-state', text: 'Loading picks…' }));
    quickPicksSection.append(qpHeader, quickPicksGrid);

    // Mixed For You / Recommended Shelf (Square cards with play overlay)
    const shelfSection = UI.el('section', {});
    const shelfHeader = UI.el('div', { class: 'ytm-section-header' });
    const shelfTitleWrap = UI.el('div', {});
    shelfTitleWrap.append(
      UI.el('div', { class: 'section-kicker', text: 'RECOMMENDED FOR YOU' }),
      UI.el('h2', { class: 'section-heading', text: isExplore ? 'New releases & mixes' : 'Mixed for you' })
    );
    shelfHeader.appendChild(shelfTitleWrap);
    const shelfGrid = UI.el('div', { class: 'ytm-shelf-grid' });
    shelfSection.append(shelfHeader, shelfGrid);

    // Listen Again / Recently Played Section
    const recentSection = UI.el('section', {});
    const recentHeader = UI.el('div', { class: 'ytm-section-header' });
    const recentTitleWrap = UI.el('div', {});
    recentTitleWrap.append(
      UI.el('div', { class: 'section-kicker', text: 'YOUR HISTORY' }),
      UI.el('h2', { class: 'section-heading', text: 'Listen again' })
    );
    recentHeader.appendChild(recentTitleWrap);
    const recentRow = UI.el('div', { class: 'ytm-quick-picks-grid' });
    const recents = Storage.getRecent();
    if (recents.length) {
      recents.slice(0, 9).forEach(song => recentRow.appendChild(UI.songRow(song, { context: recents })));
    } else {
      recentRow.appendChild(UI.el('p', { class: 'empty-state', text: 'Songs you listen to will appear here.' }));
    }
    recentSection.append(recentHeader, recentRow);

    container.append(chipRow, quickPicksSection, shelfSection, recentSection);

    const fallbackPicks = [
      { id: 'jfKfPfyJRdk', title: 'lofi hip hop radio - beats to relax/study to', artist: 'Lofi Girl', thumbnail: 'https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg' },
      { id: '5qap5aO4i9A', title: 'Lofi Hip Hop Radio 24/7', artist: 'Lofi Girl', thumbnail: 'https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg' },
      { id: 'DWcJFNfaw9c', title: 'Chillhop Radio - jazzy & lofi hip hop beats', artist: 'Chillhop Music', thumbnail: 'https://i.ytimg.com/vi/DWcJFNfaw9c/hqdefault.jpg' },
      { id: '21qNxnCS8WU', title: 'Japanese City Pop / Chill Vibes', artist: 'Plastic Love Mix', thumbnail: 'https://i.ytimg.com/vi/21qNxnCS8WU/hqdefault.jpg' },
      { id: '4xDzrJKXOOY', title: 'Synthwave Radio - chill synth / retro beats', artist: 'Lofi Girl Synthwave', thumbnail: 'https://i.ytimg.com/vi/4xDzrJKXOOY/hqdefault.jpg' },
      { id: 'rUxyKA_-grg', title: 'Sleepy Lofi Beats - late night ambient', artist: 'Lofi Girl', thumbnail: 'https://i.ytimg.com/vi/rUxyKA_-grg/hqdefault.jpg' }
    ];

    let activeSongs = fallbackPicks;

    function populateSections(songs) {
      const list = (songs && songs.length) ? songs : fallbackPicks;
      activeSongs = list;

      quickPicksGrid.innerHTML = '';
      list.slice(0, 9).forEach(song => quickPicksGrid.appendChild(UI.songRow(song, { context: list })));

      shelfGrid.innerHTML = '';
      const shelfItems = list.length > 4 ? list.slice(0, 6) : fallbackPicks;
      shelfItems.forEach(song => shelfGrid.appendChild(UI.mediaCard(song, { context: list })));
    }

    playAllBtn.addEventListener('click', () => {
      if (activeSongs && activeSongs.length) Player.playQueue(activeSongs, 0);
    });

    function loadFeed(query) {
      quickPicksGrid.innerHTML = '';
      quickPicksGrid.appendChild(UI.el('p', { class: 'empty-state', text: 'Loading picks…' }));
      const req = query ? YouTubeAPI.search(query) : YouTubeAPI.randomPicks();
      req.then(songs => populateSections(songs)).catch(() => populateSections(fallbackPicks));
    }

    moods.forEach(([label, query], idx) => {
      const chip = UI.el('button', { class: 'genre-chip' + (idx === 0 ? ' active' : ''), text: label });
      chip.addEventListener('click', () => {
        chipRow.querySelectorAll('.genre-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        loadFeed(query);
      });
      chipRow.appendChild(chip);
    });

    loadFeed(isExplore ? 'top music hits 2026' : '');
  }
};

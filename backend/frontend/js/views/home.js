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
      UI.el('div', { class: 'section-kicker', text: isExplore ? 'Charts and risers' : 'Pick a vibe' }),
      UI.el('h2', { class: 'section-heading', text: isExplore ? 'Rising now' : 'Spin something' })
    );
    const playAllBtn = UI.el('button', { class: 'ytm-pill-outline-btn', text: 'Play all' });
    qpHeader.append(qpTitleWrap, playAllBtn);

    const quickPicksGrid = UI.el('div', { class: 'ytm-quick-picks-grid' });
    quickPicksGrid.appendChild(UI.el('p', { class: 'empty-state', text: 'Tuning in...' }));
    quickPicksSection.append(qpHeader, quickPicksGrid);

    // Mixed For You / Recommended Shelf (Square cards with play overlay)
    const shelfSection = UI.el('section', {});
    const shelfHeader = UI.el('div', { class: 'ytm-section-header' });
    const shelfTitleWrap = UI.el('div', {});
    shelfTitleWrap.append(
      UI.el('div', { class: 'section-kicker', text: 'Fresh in the mix' }),
      UI.el('h2', { class: 'section-heading', text: isExplore ? 'New and next' : 'Worth a listen' })
    );
    shelfHeader.appendChild(shelfTitleWrap);
    const shelfGrid = UI.el('div', { class: 'ytm-shelf-grid' });
    shelfSection.append(shelfHeader, shelfGrid);

    // Listen Again / Recently Played Section
    const recentSection = UI.el('section', {});
    const recentHeader = UI.el('div', { class: 'ytm-section-header' });
    const recentTitleWrap = UI.el('div', {});
    recentTitleWrap.append(
      UI.el('div', { class: 'section-kicker', text: 'Your history' }),
      UI.el('h2', { class: 'section-heading', text: 'Back on repeat' })
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

    let activeSongs = [];
    let currentQuery = '';

    function showState(message) {
      quickPicksGrid.innerHTML = '';
      shelfGrid.innerHTML = '';
      const box = UI.el('div', { class: 'empty-state state-box' });
      box.appendChild(UI.el('p', { text: message }));
      const retry = UI.el('button', { class: 'btn-secondary', text: 'Try again' });
      retry.addEventListener('click', () => loadFeed(currentQuery));
      box.appendChild(retry);
      quickPicksGrid.appendChild(box);
    }

    function populateSections(list) {
      if (!list || !list.length) { showState('Nothing came back for that one. Try another mood.'); return; }
      activeSongs = list;
      quickPicksGrid.innerHTML = '';
      list.slice(0, 9).forEach(song => quickPicksGrid.appendChild(UI.songRow(song, { context: list })));
      shelfGrid.innerHTML = '';
      (list.length > 12 ? list.slice(9, 15) : list.slice(0, 6))
        .forEach(song => shelfGrid.appendChild(UI.mediaCard(song, { context: list })));
    }

    playAllBtn.addEventListener('click', () => {
      if (activeSongs && activeSongs.length) Player.playQueue(activeSongs, 0);
    });

    function loadFeed(query) {
      currentQuery = query;
      quickPicksGrid.innerHTML = '';
      quickPicksGrid.appendChild(UI.el('p', { class: 'empty-state', text: 'Tuning in...' }));
      const req = query ? YouTubeAPI.search(query) : YouTubeAPI.randomPicks();
      req.then(populateSections).catch(err => {
        const m = err && err.message;
        showState(m && !/_FAILED$/.test(m) ? m : 'Could not load music right now.');
      });
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

/* search.js — matches Android fragment_search.xml & SearchFragment.java */
const SearchView = {
  render(container, params) {
    // Header
    const topHeader = UI.el('div', { class: 'android-top-header' });
    const headerLeft = UI.el('div', { class: 'android-header-left' });
    headerLeft.append(
      UI.el('div', { class: 'android-header-icon', html: UI.icons.search }),
      UI.el('div', { class: 'android-header-title', text: 'Search & Explore' })
    );
    topHeader.appendChild(headerLeft);

    // Search Input Box
    const searchWrap = UI.el('div', { class: 'search-box-wrap' });
    const searchIcon = UI.el('span', { class: 'android-search-icon', html: UI.icons.search });
    const input = UI.el('input', {
      class: 'android-search-input',
      attrs: {
        type: 'text',
        placeholder: 'Search songs, artists, genres...',
        autocomplete: 'off'
      }
    });
    searchWrap.append(searchIcon, input);

    const sectionTitle = UI.el('h2', { class: 'section-heading', text: 'Browse Categories' });
    const resultsWrap = UI.el('div', {});

    container.append(topHeader, searchWrap, sectionTitle, resultsWrap);

    const categories = [
      { name: 'Chill Lo-Fi', variant: 'variant-elevated' },
      { name: 'Synth Wave', variant: 'variant-hero' },
      { name: 'Acoustic Vibes', variant: 'variant-surface' },
      { name: 'Offline Tracks', variant: 'variant-elevated' },
      { name: 'Hip-Hop Beats', variant: 'variant-hero' },
      { name: 'Ambient & Zen', variant: 'variant-surface' }
    ];

    function renderCategories() {
      sectionTitle.textContent = 'Browse Categories';
      resultsWrap.innerHTML = '';
      const grid = UI.el('div', { class: 'category-grid' });
      categories.forEach(cat => {
        const card = UI.el('button', { class: `category-card ${cat.variant}` });
        card.appendChild(UI.el('span', { class: 'category-card-name', text: cat.name }));
        card.addEventListener('click', () => {
          const clean = cat.name.replace(' Beats', '').replace(' Tracks', '');
          input.value = clean;
          updateResults(clean);
        });
        grid.appendChild(card);
      });
      resultsWrap.appendChild(grid);
    }

    const debouncedRemoteSearch = UI.debounce(async (query, listEl, localMatches) => {
      try {
        const remote = await YouTubeAPI.search(query);
        if (input.value.trim() !== query) return;
        remote.forEach(song => {
          if (!localMatches.some(m => m.id === song.id)) {
            listEl.appendChild(UI.songRow({ ...song, genre: 'YouTube' }, { context: localMatches.concat(remote) }));
          }
        });
      } catch (e) {}
    }, 450);

    function updateResults(rawQuery) {
      const q = rawQuery.trim();
      if (!q) {
        renderCategories();
        return;
      }
      sectionTitle.textContent = 'Search Results';
      resultsWrap.innerHTML = '';
      const list = UI.el('div', { class: 'song-list' });
      const all = Storage.getSampleTracks();
      const lower = q.toLowerCase();
      const filtered = all.filter(t =>
        t.title.toLowerCase().includes(lower) ||
        t.artist.toLowerCase().includes(lower) ||
        (t.genre && t.genre.toLowerCase().includes(lower)) ||
        (lower === 'offline' && t.isDownloaded)
      );
      filtered.forEach(t => list.appendChild(UI.songRow(t, { context: filtered })));
      resultsWrap.appendChild(list);

      if (q.length >= 2) {
        debouncedRemoteSearch(q, list, filtered);
      }
    }

    const initialQ = params ? (params.get('q') || '') : '';
    if (initialQ) {
      input.value = initialQ;
      updateResults(initialQ);
    } else {
      renderCategories();
    }

    input.addEventListener('input', () => updateResults(input.value));
  }
};

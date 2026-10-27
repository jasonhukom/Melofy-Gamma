const SearchView = {
  render(container, params) {
    const heading = UI.el('h1', { class: 'page-heading', text: 'Search' });

    const searchBarWrap = UI.el('div', { class: 'search-bar-wrap' });
    const icon = UI.el('span', { class: 'search-icon' });
    icon.innerHTML = UI.icons.search;
    const input = UI.el('input', {
      class: 'search-input',
      attrs: { type: 'text', placeholder: 'What do you want to listen to?', autocomplete: 'off' }
    });
    searchBarWrap.append(icon, input);

    const status = UI.el('p', { class: 'search-status' });
    const results = UI.el('div', { class: 'song-list' });
    container.append(heading, searchBarWrap, status, results);

    const initialQuery = params.get('q') || '';
    const topbarInput = document.getElementById('topbar-search-input');
    const topbarClear = document.getElementById('topbar-search-clear');
    if (topbarInput && initialQuery) {
      topbarInput.value = initialQuery;
      if (topbarClear) topbarClear.hidden = false;
    }

    if (initialQuery) {
      input.value = initialQuery;
      runSearch(initialQuery);
    } else {
      input.focus();
    }

    const debouncedSearch = UI.debounce((q) => runSearch(q), 450);
    input.addEventListener('input', () => {
      const q = input.value.trim();
      if (topbarInput) {
        topbarInput.value = input.value;
        if (topbarClear) topbarClear.hidden = !q;
      }
      if (q.length >= 2) debouncedSearch(q);
      if (!q) { results.innerHTML = ''; status.textContent = ''; }
    });

    async function runSearch(query) {
      status.textContent = 'Searching…';
      results.innerHTML = '';
      try {
        const songs = await YouTubeAPI.search(query);
        status.textContent = songs.length ? `Songs (${songs.length})` : 'No results found';
        songs.forEach(song => results.appendChild(UI.songRow(song, { context: songs })));
      } catch (err) {
        status.textContent = '';
        results.appendChild(UI.el('p', { class: 'empty-state', text: 'Search failed — try again in a moment.' }));
      }
    }
  }
};

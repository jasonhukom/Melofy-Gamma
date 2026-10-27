/* router.js — hash-based SPA router with Android 5-tab footer color updates */
const Router = (() => {
  const routes = {};
  let container = null;

  function register(path, renderFn) { routes[path] = renderFn; }

  function parseHash() {
    const raw = location.hash.slice(1) || '/';
    const [path, queryString] = raw.split('?');
    return { path: path || '/', params: new URLSearchParams(queryString || '') };
  }

  function navigate(path) {
    if (location.hash.slice(1) === path) { handle(); return; }
    location.hash = path;
  }

  function handle() {
    const { path, params } = parseHash();
    const renderFn = routes[path] || routes['/'];
    container.innerHTML = '';
    renderFn(container, params);
    highlightActiveNav(path.split('?')[0]);
    container.scrollTop = 0;
  }

  function highlightActiveNav(path) {
    const normalized = path === '/library' ? '/downloads' : path;
    const isSearch = normalized === '/search';

    // Outer 4 tabs: white when inactive (and all white when search is active), yellow when selected
    document.querySelectorAll('.bottom-nav-item').forEach(link => {
      const route = link.getAttribute('data-route');
      link.classList.toggle('active', !isSearch && route === normalized);
    });

    // Center circular Search button: white stroke when active
    const searchCircle = document.getElementById('container-search-circle');
    if (searchCircle) {
      searchCircle.classList.toggle('active', isSearch);
    }
  }

  function refresh() {
    if (container) handle();
  }

  function start(mountEl) {
    container = mountEl;
    window.addEventListener('hashchange', handle);
    handle();
  }

  return { register, navigate, refresh, start };
})();

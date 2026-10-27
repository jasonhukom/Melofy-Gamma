/* sidebar.js — YouTube Music navigation rail / drawer. Docked open by default
   on wide screens, collapsible via the topbar menu button, and slide-over
   drawer with backdrop on mobile/tablet screens. */
const Sidebar = (() => {
  let sidebarEl, backdropEl;
  const WIDE_BREAKPOINT = 960;

  function render() {
    sidebarEl = document.getElementById('sidebar');
    backdropEl = document.getElementById('sidebar-backdrop');
    document.getElementById('menu-btn').addEventListener('click', toggle);
    backdropEl.addEventListener('click', close);
    window.innerWidth > WIDE_BREAKPOINT ? open() : close();
  }

  function isOpen() { return sidebarEl.classList.contains('open'); }
  function toggle() { isOpen() ? close() : open(); }
  function open() {
    sidebarEl.classList.add('open');
    backdropEl.classList.add('show');
    document.body.classList.add('sidebar-open');
  }
  function close() {
    sidebarEl.classList.remove('open');
    backdropEl.classList.remove('show');
    document.body.classList.remove('sidebar-open');
  }
  function closeOnNavigate() { if (window.innerWidth <= WIDE_BREAKPOINT) close(); }

  return { render, toggle, open, close, closeOnNavigate };
})();
window.Sidebar = Sidebar;

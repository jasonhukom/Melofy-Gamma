/* soundMixer.js - the volume dock. A round button (tap to mute on desktop,
   tap to open on touch screens) that expands into a horizontal slider with a
   live gradient fill. Keyboard accessible; arrow keys adjust by 5. */
const SoundMixer = (() => {
  let root, btn, slider, valueLabel;
  let lastVolume = 80;
  const canHover = window.matchMedia('(hover: hover)').matches;

  function paint(v) {
    slider.style.setProperty('--fill', v + '%');
    valueLabel.textContent = String(v);
    btn.innerHTML = v === 0 ? UI.icons.volumeMute : UI.icons.volumeHigh;
    btn.setAttribute('aria-label', v === 0 ? 'Unmute' : 'Mute');
  }
  function apply(v) { Player.setVolume(v); paint(v); }
  function setOpen(open) { root.classList.toggle('open', open); }

  function render() {
    root = document.getElementById('sound-mixer');
    btn = document.getElementById('sound-mixer-btn');
    slider = document.getElementById('sound-mixer-slider');
    valueLabel = document.getElementById('sound-mixer-value');

    const start = Storage.getSettings().volume ?? 80;
    if (start > 0) lastVolume = start;
    slider.value = String(start);
    paint(start);

    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (!canHover) { setOpen(!root.classList.contains('open')); return; }
      const v = Number(slider.value);
      const next = v === 0 ? (lastVolume || 80) : 0;
      if (v > 0) lastVolume = v;
      slider.value = String(next);
      apply(next);
    });
    slider.addEventListener('input', (e) => {
      const v = Number(e.target.value);
      if (v > 0) lastVolume = v;
      apply(v);
    });
    root.addEventListener('mouseenter', () => canHover && setOpen(true));
    root.addEventListener('mouseleave', () => canHover && !root.contains(document.activeElement) && setOpen(false));
    root.addEventListener('focusin', () => setOpen(true));
    document.addEventListener('click', (e) => { if (!root.contains(e.target)) setOpen(false); });
  }
  return { render };
})();

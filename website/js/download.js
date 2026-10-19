/* Download page behaviour: collapse the top bar and fade the banner title on scroll. */
(function () {
  const topBar = document.querySelector('.top-bar');
  const heroTitle = document.querySelector('.hero h1');
  if (!topBar || !heroTitle) return;

  const update = () => {
    const y = window.scrollY;
    const collapsed = y > 50;
    topBar.classList.toggle('collapsed', collapsed);
    heroTitle.style.opacity = collapsed ? String(Math.max(0, 1 - (y - 50) / 100)) : '1';
    heroTitle.style.transform = collapsed ? `translateY(${Math.min(20, (y - 50) * 0.2)}px)` : 'translateY(0)';
  };
  window.addEventListener('scroll', update, { passive: true });
  update();
})();

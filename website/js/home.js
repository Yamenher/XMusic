(function () {
  const topBar = document.querySelector('.top-bar');
  const hero = document.querySelector('.hero');
  if (topBar && hero) {
    const barHeight = topBar.offsetHeight;
    new IntersectionObserver(([entry]) => {
      topBar.classList.toggle('collapsed', !entry.isIntersecting);
    }, { rootMargin: `-${barHeight}px 0px 0px 0px`, threshold: 0 }).observe(hero);
  }

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  document.querySelectorAll('.qa-list details').forEach((details) => {
    const summary = details.querySelector('summary');
    let anim = null;
    summary.addEventListener('click', (e) => {
      e.preventDefault();
      if (reduceMotion.matches) { details.open = !details.open; return; }
      if (anim) anim.cancel();
      const from = details.offsetHeight;
      const closing = details.open;
      if (closing) details.classList.add('closing'); else details.open = true;
      const to = closing ? summary.offsetHeight : details.offsetHeight;
      anim = details.animate({ height: [from + 'px', to + 'px'] },
        { duration: closing ? 220 : 300, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
      anim.onfinish = () => {
        if (closing) { details.open = false; details.classList.remove('closing'); }
        anim = null;
      };
      anim.oncancel = () => { details.classList.remove('closing'); anim = null; };
    });
  });
})();

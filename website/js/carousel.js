(function () {
  document.querySelectorAll('.carousel').forEach(initCarousel);

  function initCarousel(root) {
    const stage = root.querySelector('.carousel-stage');
    const scroller = root.querySelector('.carousel-scroller');
    const items = Array.from(root.querySelectorAll('.carousel-item'));
    const imgs = Array.from(root.querySelectorAll('.carousel-item img'));
    const snaps = Array.from(root.querySelectorAll('.carousel-snap'));
    const prevBtn = root.querySelector('.carousel-nav--prev');
    const nextBtn = root.querySelector('.carousel-nav--next');
    if (!scroller || !items.length) return;

    const N = items.length;
    const firstImg = imgs[0];
    let aspect = 1.25;
    let W = 0, L = 0, H = 0, GAP = 12, PEEK = 0, RADIUS = 28, stride = 0, maxScroll = 0, snapTargets = [];

    root.style.maxWidth = '1160px';
    root.style.width = 'calc(100% - (var(--page-gutter) * 2))';
    root.style.marginLeft = 'auto';
    root.style.marginRight = 'auto';

    [root, stage, scroller, ...items, ...imgs, ...snaps].forEach((el) => {
      if (el) {
        el.style.maxHeight = 'none';
        el.style.minHeight = '0';
      }
    });

    function setBtnDisabled(btn, isDisabled) {
      if (!btn) return;
      btn.disabled = isDisabled;
      btn.classList.toggle('is-disabled', isDisabled);
      if (isDisabled) {
        btn.setAttribute('disabled', '');
        btn.setAttribute('aria-disabled', 'true');
        btn.style.pointerEvents = 'none';
        btn.style.opacity = '0.38';
      } else {
        btn.removeAttribute('disabled');
        btn.setAttribute('aria-disabled', 'false');
        btn.style.pointerEvents = '';
        btn.style.opacity = '';
      }
    }

    function updateAspect() {
      if (firstImg && firstImg.naturalWidth > 0 && firstImg.naturalHeight > 0) {
        aspect = firstImg.naturalWidth / firstImg.naturalHeight;
      }
    }

    if (firstImg) {
      if (firstImg.complete) updateAspect();
      else firstImg.addEventListener('load', () => { updateAspect(); measure(); }, { once: true });
    }

    function measure() {
      updateAspect();
      const box = stage || root;
      W = box.clientWidth;
      GAP = W >= 600 ? 12 : 6;
      PEEK = W >= 600 ? Math.max(110, Math.round(W * 0.18)) : 64;
      RADIUS = W >= 600 ? Math.min(48, Math.max(28, Math.round(W * 0.042))) : 28;
      L = W - PEEK - GAP;
      H = Math.round(L / aspect);
      stride = L + GAP;

      root.style.height = H + 'px';
      if (stage) stage.style.height = H + 'px';
      scroller.style.height = H + 'px';
      scroller.style.borderRadius = RADIUS + 'px';
      scroller.style.gap = GAP + 'px';

      snaps.forEach((el) => {
        el.style.width = L + 'px';
        el.style.height = H + 'px';
      });
      items.forEach((el) => {
        el.style.width = L + 'px';
        el.style.height = H + 'px';
      });

      const realMax = scroller.scrollWidth - scroller.clientWidth;
      maxScroll = realMax > 0 ? realMax : Math.max(0, N * L + (N - 1) * GAP - W);
      snapTargets = items.map((_, i) => (i === N - 1 ? maxScroll : Math.min(i * stride, maxScroll)));
      render();
    }

    function getProgress(s) {
      if (N <= 1 || maxScroll <= 0) return 0;
      const lastPrev = snapTargets[N - 2];
      if (s <= lastPrev) return stride > 0 ? s / stride : 0;
      const rem = maxScroll - lastPrev;
      return (N - 2) + (rem > 0 ? (s - lastPrev) / rem : 1);
    }

    function render() {
      const realMax = scroller.scrollWidth - scroller.clientWidth;
      if (realMax > 0 && Math.abs(realMax - maxScroll) > 1) {
        maxScroll = realMax;
        snapTargets[N - 1] = maxScroll;
      }

      const s = Math.min(Math.max(scroller.scrollLeft, 0), maxScroll);
      const progress = getProgress(s);

      const lastStart = Math.max(0, N - 2);
      const blend = progress > lastStart ? Math.min(progress - lastStart, 1) : 0;

      const normalKeylines = [
        { p: -1, left: -(PEEK + GAP), size: PEEK },
        { p: 0, left: 0, size: L },
        { p: 1, left: L + GAP, size: PEEK },
        { p: 2, left: W + GAP, size: PEEK }
      ];

      const endKeylines = [
        { p: -2, left: -(PEEK + GAP), size: PEEK },
        { p: -1, left: 0, size: PEEK },
        { p: 0, left: PEEK + GAP, size: L },
        { p: 1, left: W + GAP, size: PEEK }
      ];

      function sample(k, rel) {
        if (rel <= k[0].p) return { left: k[0].left + (rel - k[0].p) * (PEEK + GAP), size: k[0].size };
        const last = k[k.length - 1];
        if (rel >= last.p) return { left: last.left + (rel - last.p) * (PEEK + GAP), size: last.size };
        for (let j = 0; j < k.length - 1; j++) {
          if (rel <= k[j + 1].p) {
            const t = (rel - k[j].p) / (k[j + 1].p - k[j].p);
            return {
              left: k[j].left + (k[j + 1].left - k[j].left) * t,
              size: k[j].size + (k[j + 1].size - k[j].size) * t
            };
          }
        }
        return { left: last.left, size: last.size };
      }

      items.forEach((item, i) => {
        const rel = i - progress;
        const a = sample(normalKeylines, rel);
        const b = sample(endKeylines, rel);
        const left = a.left + (b.left - a.left) * blend;
        const size = a.size + (b.size - a.size) * blend;

        const x = left + size / 2 - L / 2;
        const visL = Math.max(left, 0);
        const visR = Math.min(left + size, W);

        if (visR - visL <= 0.5) {
          item.style.visibility = 'hidden';
          return;
        }
        item.style.visibility = 'visible';
        item.style.transform = 'translate3d(' + x + 'px,0,0)';
        item.style.clipPath =
          'inset(0 ' + (x + L - visR) + 'px 0 ' + (visL - x) + 'px round ' + RADIUS + 'px)';
      });

      const atStart = s <= 2 || progress <= 0.02;
      const atEnd = maxScroll <= 0 || s >= maxScroll - 2 || progress >= N - 1.02;
      setBtnDisabled(prevBtn, atStart);
      setBtnDisabled(nextBtn, atEnd);
    }

    let ticking = false;
    scroller.addEventListener('scroll', () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => { ticking = false; render(); });
    }, { passive: true });
    new ResizeObserver(measure).observe(root);

    let isDown = false, startX = 0, startScroll = 0, raf = 0;
    let lastX = 0, lastT = 0, velocity = 0;

    const SETTLE_MS = 320;
    const FLICK_THRESHOLD = 0.35;
    const DRAG_MULTIPLIER = 1.5;

    const nearestIdx = (pos) => {
      let best = 0, bestDist = Infinity;
      for (let i = 0; i < snapTargets.length; i++) {
        const d = Math.abs(pos - snapTargets[i]);
        if (d < bestDist) { bestDist = d; best = i; }
      }
      return best;
    };

    const stopSettle = () => {
      if (!raf) return;
      cancelAnimationFrame(raf);
      raf = 0;
      scroller.style.scrollSnapType = '';
    };

    const animateTo = (target, duration) => {
      const from = scroller.scrollLeft;
      const dist = target - from;
      if (Math.abs(dist) < 1) {
        scroller.scrollLeft = target;
        scroller.style.scrollSnapType = '';
        render();
        return;
      }
      scroller.style.scrollSnapType = 'none';
      const t0 = performance.now();
      const tick = (now) => {
        const t = Math.min((now - t0) / duration, 1);
        scroller.scrollLeft = from + dist * (1 - Math.pow(1 - t, 3));
        render();
        if (t < 1) {
          raf = requestAnimationFrame(tick);
        } else {
          raf = 0;
          scroller.scrollLeft = target;
          scroller.style.scrollSnapType = '';
          render();
        }
      };
      raf = requestAnimationFrame(tick);
    };

    const settle = () => {
      const startIdx = nearestIdx(startScroll);
      const currentPos = scroller.scrollLeft;
      const stale = performance.now() - lastT > 60;
      const v = stale ? 0 : velocity * DRAG_MULTIPLIER;

      let targetIdx;
      if (Math.abs(v) > FLICK_THRESHOLD) {
        targetIdx = startIdx + (v > 0 ? 1 : -1);
      } else {
        const rawIdx = nearestIdx(currentPos);
        targetIdx = Math.min(Math.max(rawIdx, startIdx - 1), startIdx + 1);
      }

      targetIdx = Math.min(Math.max(targetIdx, 0), N - 1);
      const target = snapTargets[targetIdx];
      const speedFactor = Math.min(Math.abs(v) / 2.5, 0.45);
      animateTo(target, SETTLE_MS * (1 - speedFactor));
    };

    scroller.addEventListener('mousedown', (e) => {
      stopSettle();
      isDown = true;
      startX = lastX = e.pageX;
      lastT = performance.now();
      velocity = 0;
      startScroll = scroller.scrollLeft;
      scroller.classList.add('dragging');
    });

    const endDrag = () => {
      if (!isDown) return;
      isDown = false;
      scroller.style.scrollSnapType = 'none';
      scroller.classList.remove('dragging');
      settle();
    };

    scroller.addEventListener('mouseleave', endDrag);
    window.addEventListener('mouseup', endDrag);

    scroller.addEventListener('mousemove', (e) => {
      if (!isDown) return;
      e.preventDefault();
      const now = performance.now();
      const dt = now - lastT;
      if (dt > 0) {
        const instVel = (lastX - e.pageX) / dt;
        velocity = velocity * 0.4 + instVel * 0.6;
        lastX = e.pageX;
        lastT = now;
      }
      scroller.scrollLeft = startScroll - (e.pageX - startX) * DRAG_MULTIPLIER;
    });

    scroller.addEventListener('wheel', stopSettle, { passive: true });
    scroller.addEventListener('touchstart', stopSettle, { passive: true });
    scroller.addEventListener('keydown', stopSettle);

    const step = (dir) => {
      stopSettle();
      const cur = nearestIdx(scroller.scrollLeft);
      const next = Math.min(Math.max(cur + dir, 0), N - 1);
      if (next === cur) return;
      setBtnDisabled(prevBtn, next === 0);
      setBtnDisabled(nextBtn, next === N - 1);
      animateTo(snapTargets[next], SETTLE_MS);
    };
    if (prevBtn) prevBtn.addEventListener('click', (e) => {
      if (prevBtn.classList.contains('is-disabled')) { e.preventDefault(); return; }
      step(-1);
    });
    if (nextBtn) nextBtn.addEventListener('click', (e) => {
      if (nextBtn.classList.contains('is-disabled')) { e.preventDefault(); return; }
      step(1);
    });
    measure();
  }
})();
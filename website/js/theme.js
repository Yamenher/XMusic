(function () {
  const root = document.documentElement;
  const read = () => { try { return localStorage.getItem('theme'); } catch (e) { return null; } };
  const save = (theme) => { try { localStorage.setItem('theme', theme); } catch (e) { /* storage unavailable :/ */ } };

  const saved = read();
  if (saved) root.setAttribute('data-theme', saved);

  document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.querySelector('.theme-switch');
    if (!toggle) return;
    toggle.selected = root.getAttribute('data-theme') === 'dark';
    toggle.addEventListener('change', () => {
      const theme = toggle.selected ? 'dark' : 'light';
      root.setAttribute('data-theme', theme);
      save(theme);
    });
  });
})();

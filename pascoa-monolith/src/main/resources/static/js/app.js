document.addEventListener('click', function (e) {
  var el = e.target.closest('[data-confirm]:not(form)');
  if (el && !confirm(el.dataset.confirm)) e.preventDefault();
});
document.addEventListener('submit', function (e) {
  var f = e.target;
  if (f.dataset && f.dataset.confirm && !confirm(f.dataset.confirm)) e.preventDefault();
});
if ('serviceWorker' in navigator) {
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .catch(function (err) { console.warn('SW register failed:', err); });
  });
}

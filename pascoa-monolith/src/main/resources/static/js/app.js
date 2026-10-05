document.addEventListener('click', function (e) {
  var el = e.target.closest('[data-confirm]:not(form)');
  if (el && !confirm(el.dataset.confirm)) e.preventDefault();
});
document.addEventListener('submit', function (e) {
  var f = e.target;
  if (f.dataset && f.dataset.confirm && !confirm(f.dataset.confirm)) e.preventDefault();
});
document.addEventListener('change', function (e) {
  var el = e.target;
  if (el.dataset && el.dataset.autosubmit !== undefined && el.form) el.form.submit();
  else if (el.dataset && el.dataset.navegar && el.value) window.location.href = el.dataset.navegar + encodeURIComponent(el.value);
});
if ('serviceWorker' in navigator) {
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .catch(function (err) { console.warn('SW register failed:', err); });
  });
}

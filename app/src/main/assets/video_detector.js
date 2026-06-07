(function () {
  if (window.__wc_loaded) return;
  window.__wc_loaded = true;

  function report(url) {
    if (!url || typeof url !== 'string') return;
    if (url.startsWith('blob:') || url.startsWith('data:')) return;
    if (!url.startsWith('http')) return;
    try { Android.onVideoFound(url); } catch (e) {}
  }

  // Scan existing <video> elements
  function scanVideos() {
    document.querySelectorAll('video').forEach(function (v) {
      var src = v.currentSrc || v.src;
      if (src) report(src);
      v.addEventListener('loadstart', function () { report(v.currentSrc || v.src); });
      v.addEventListener('play',      function () { report(v.currentSrc || v.src); });
    });
  }

  // Watch for new <video> nodes added dynamically (SPA sites)
  new MutationObserver(scanVideos)
    .observe(document.documentElement, { childList: true, subtree: true });

  // Intercept XHR — catches HLS manifest (.m3u8) requests from video.js, hls.js, etc.
  var origOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (method, url) {
    if (typeof url === 'string') {
      var abs = url.startsWith('http') ? url : location.origin + (url.startsWith('/') ? '' : '/') + url;
      if (/\.(m3u8|mpd)(\?|$)/i.test(url)) report(abs);
    }
    return origOpen.apply(this, arguments);
  };

  // Intercept fetch — same purpose as XHR intercept
  var origFetch = window.fetch;
  window.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : (input && input.url) || '';
    if (url) {
      var abs = url.startsWith('http') ? url : location.origin + (url.startsWith('/') ? '' : '/') + url;
      if (/\.(m3u8|mpd)(\?|$)/i.test(url)) report(abs);
    }
    return origFetch.apply(this, arguments);
  };

  scanVideos();
})();

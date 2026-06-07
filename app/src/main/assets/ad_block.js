(function () {
  if (window.__adBlockLoaded) return;
  window.__adBlockLoaded = true;

  // ── 1. Block popup / popunder windows ───────────────────────────────────────
  window.open = function () { return null; };

  // Prevent redirect on visibility change (common popunder trick)
  document.addEventListener('visibilitychange', function (e) { e.stopImmediatePropagation(); }, true);

  // ── 2. CSS — hide ad-styled elements ────────────────────────────────────────
  var css = `
    [id*="adsense"],[id*="adnxs"],[id*="advert"],[id*="-ads"],[id*="ads-"],
    [class*="adsense"],[class*="adnxs"],[class*="advert"],[class*="-ads"],[class*="ads-"],
    [id*="banner-ad"],[id*="ad-banner"],[class*="banner-ad"],[class*="ad-banner"],
    [id*="popup"],[class*="popup-ad"],[class*="ad-popup"],
    [id*="interstitial"],[class*="interstitial"],
    [id*="overlay"],[class*="ad-overlay"],[class*="overlay-ad"],
    iframe[src*="doubleclick"],iframe[src*="googlesyndication"],
    iframe[src*="adnxs"],iframe[src*="outbrain"],iframe[src*="taboola"],
    iframe[src*="propellerads"],iframe[src*="exoclick"],iframe[src*="popcash"],
    iframe[src*="popads"],iframe[src*="adsterra"],iframe[src*="trafficjunky"],
    div[class*="sponsor"],div[id*="sponsor"],
    #ad, #ads, .ad, .ads, .advertisement, .ad-container, .ad-wrapper,
    .ad-slot, .ad-unit, .adbox, .adbanner { display: none !important; }
  `;
  var style = document.createElement('style');
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);

  // ── 3. Remove overlay / full-screen ad elements dynamically ─────────────────
  function removeOverlays() {
    document.querySelectorAll('*').forEach(function (el) {
      try {
        var s = window.getComputedStyle(el);
        var isFixed = (s.position === 'fixed' || s.position === 'absolute');
        var isHighZ = parseInt(s.zIndex, 10) >= 1000;
        var isLarge = el.offsetWidth > window.innerWidth * 0.5 &&
                      el.offsetHeight > window.innerHeight * 0.3;
        var hasAdKeyword = /ad|ads|banner|popup|overlay|sponsor|promo/i
          .test((el.id || '') + ' ' + (el.className || ''));

        if (isFixed && isHighZ && isLarge && hasAdKeyword) {
          el.remove();
        }
      } catch (e) {}
    });

    // Restore body scroll that some ad scripts lock
    document.body && (document.body.style.overflow = '');
    document.documentElement && (document.documentElement.style.overflow = '');
  }

  removeOverlays();
  setInterval(removeOverlays, 2500);
  new MutationObserver(removeOverlays)
    .observe(document.documentElement, { childList: true, subtree: true });
})();

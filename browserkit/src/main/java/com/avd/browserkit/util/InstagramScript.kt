package com.avd.browserkit.util

/**
 * Instagram Reels download UI.
 * SPA reel swipes recycle &lt;video&gt; nodes — must re-bind buttons and hook history.
 */
object InstagramScript {

    val jsCode: String = """
    (function() {
      function reportMedia(url) {
        if (!url || url.indexOf('http') !== 0) return;
        if (url.indexOf('blob:') === 0) return;
        try { BrowserKitDetector.onMediaUrl(url); } catch (e) {}
      }

      function reportClick(url) {
        var u = url || '';
        if (u.indexOf('blob:') === 0) u = '';
        if (!u || u.indexOf('http') !== 0) {
          u = window.__avdIgLatestCdn || location.href || '';
        }
        if (!u || u.indexOf('http') !== 0) return;
        try { BrowserKitDetector.onVideoClicked(u); } catch (e1) {
          try { AndroidInterface.onVideoClicked(u); } catch (e2) {}
        }
      }

      function videoSrc(v) {
        if (!v) return null;
        var src = v.currentSrc || v.src || '';
        if (src && src.indexOf('http') === 0) return src;
        var sources = v.querySelectorAll('source');
        for (var i = 0; i < sources.length; i++) {
          if (sources[i].src && sources[i].src.indexOf('http') === 0) return sources[i].src;
        }
        return null;
      }

      function ensureFloatingBtn() {
        var btn = document.getElementById('avd-ig-float-dl');
        if (btn) return btn;
        btn = document.createElement('button');
        btn.id = 'avd-ig-float-dl';
        btn.type = 'button';
        btn.setAttribute('data-avd-ig-dl', '1');
        btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="15" height="22" viewBox="0 0 8 12"><path d="M3.954,9.939C4.023,10.008 4.114,10.042 4.205,10.042C4.295,10.042 4.386,10.008 4.456,9.939L7.306,7.089C7.407,6.987 7.437,6.835 7.382,6.702C7.328,6.569 7.198,6.483 7.055,6.483H5.984V3.204C5.984,3.008 5.826,2.85 5.63,2.85H2.78C2.584,2.85 2.425,3.008 2.425,3.204V6.483H1.355C1.211,6.483 1.082,6.569 1.027,6.702C0.972,6.835 1.002,6.987 1.104,7.089L3.954,9.939Z" fill="#fff"/><path d="M2.78,2.133H5.63C5.825,2.133 5.984,1.975 5.984,1.779C5.984,1.583 5.825,1.424 5.63,1.424H2.78C2.584,1.424 2.425,1.583 2.425,1.779C2.425,1.975 2.584,2.133 2.78,2.133Z" fill="#fff"/><path d="M2.78,0.71H5.63C5.825,0.71 5.984,0.551 5.984,0.355C5.984,0.159 5.825,0 5.63,0H2.78C2.584,0 2.425,0.159 2.425,0.355C2.425,0.551 2.584,0.71 2.78,0.71Z" fill="#fff"/><path d="M1,11L7.5,11A0.5,0.5 0,0 1,8 11.5L8,11.5A0.5,0.5 0,0 1,7.5 12L1,12A0.5,0.5 0,0 1,0.5 11.5L0.5,11.5A0.5,0.5 0,0 1,1 11z" fill="#fff"/></svg>';
        btn.style.cssText = 'position:fixed;right:14px;top:18%;width:48px;height:48px;border-radius:50%;background:#E1306C;border:2px solid #fff;z-index:2147483647;padding:0;display:flex;align-items:center;justify-content:center;pointer-events:auto;box-shadow:0 2px 8px rgba(0,0,0,0.35);';
        btn.onclick = function(ev) {
          ev.stopPropagation();
          ev.preventDefault();
          var now = Date.now();
          if (window.__avdIgClickAt && (now - window.__avdIgClickAt) < 1200) return false;
          window.__avdIgClickAt = now;
          var v = findActiveVideo();
          var src = videoSrc(v);
          reportClick(src || window.__avdIgLatestCdn || location.href);
          return false;
        };
        (document.body || document.documentElement).appendChild(btn);
        return btn;
      }

      function findActiveVideo() {
        var videos = document.querySelectorAll('video');
        var best = null;
        var bestScore = -1;
        for (var i = 0; i < videos.length; i++) {
          var v = videos[i];
          var r = v.getBoundingClientRect();
          var visible = Math.max(0, Math.min(r.bottom, window.innerHeight) - Math.max(r.top, 0));
          var score = visible;
          if (!v.paused) score += 10000;
          if (score > bestScore) {
            bestScore = score;
            best = v;
          }
        }
        return best;
      }

      function addBtnOnVideo(video) {
        if (!video || video.nodeType !== 1) return;
        var existing = video.querySelector && video.querySelector('button[data-avd-ig-dl="1"]');
        if (existing) return;
        // Recycled node may keep old flag without button — clear and recreate.
        if (video.getAttribute('data-avd-ig-btn') === '1' && !existing) {
          video.removeAttribute('data-avd-ig-btn');
        }
        if (video.getAttribute('data-avd-ig-btn') === '1') return;
        video.setAttribute('data-avd-ig-btn', '1');
        var host = video.parentElement || video;
        try {
          if (window.getComputedStyle(host).position === 'static') {
            host.style.position = 'relative';
          }
          host.style.overflow = 'visible';
        } catch (e) {}
        var button = document.createElement('button');
        button.type = 'button';
        button.setAttribute('data-avd-ig-dl', '1');
        button.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="15" height="22" viewBox="0 0 8 12"><path d="M3.954,9.939C4.023,10.008 4.114,10.042 4.205,10.042C4.295,10.042 4.386,10.008 4.456,9.939L7.306,7.089C7.407,6.987 7.437,6.835 7.382,6.702C7.328,6.569 7.198,6.483 7.055,6.483H5.984V3.204C5.984,3.008 5.826,2.85 5.63,2.85H2.78C2.584,2.85 2.425,3.008 2.425,3.204V6.483H1.355C1.211,6.483 1.082,6.569 1.027,6.702C0.972,6.835 1.002,6.987 1.104,7.089L3.954,9.939Z" fill="#fff"/><path d="M2.78,2.133H5.63C5.825,2.133 5.984,1.975 5.984,1.779C5.984,1.583 5.825,1.424 5.63,1.424H2.78C2.584,1.424 2.425,1.583 2.425,1.779C2.425,1.975 2.584,2.133 2.78,2.133Z" fill="#fff"/><path d="M2.78,0.71H5.63C5.825,0.71 5.984,0.551 5.984,0.355C5.984,0.159 5.825,0 5.63,0H2.78C2.584,0 2.425,0.159 2.425,0.355C2.425,0.551 2.584,0.71 2.78,0.71Z" fill="#fff"/><path d="M1,11L7.5,11A0.5,0.5 0,0 1,8 11.5L8,11.5A0.5,0.5 0,0 1,7.5 12L1,12A0.5,0.5 0,0 1,0.5 11.5L0.5,11.5A0.5,0.5 0,0 1,1 11z" fill="#fff"/></svg>';
        button.style.cssText = 'position:absolute;right:10px;top:12px;width:42px;height:42px;border-radius:50%;background:#E1306C;border:2px solid #fff;z-index:2147483646;padding:0;display:flex;align-items:center;justify-content:center;pointer-events:auto;';
        button.onclick = function(ev) {
          ev.stopPropagation();
          ev.preventDefault();
          var src = videoSrc(video);
          reportClick(src || window.__avdIgLatestCdn || location.href);
        };
        try { host.appendChild(button); } catch (e2) {}
        try {
          video.addEventListener('play', function() {
            var s = videoSrc(video);
            if (s) reportMedia(s);
          }, true);
        } catch (e3) {}
      }

      function scan() {
        ensureFloatingBtn();
        var videos = document.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
          var v = videos[i];
          var s = videoSrc(v);
          if (s) reportMedia(s);
          addBtnOnVideo(v);
        }
        var active = findActiveVideo();
        var activeSrc = videoSrc(active);
        if (activeSrc) {
          window.__avdIgLatestCdn = activeSrc;
          reportMedia(activeSrc);
        }
        return videos.length;
      }

      function hookHistory() {
        if (window.__avdIgHistHooked) return;
        window.__avdIgHistHooked = true;
        var wrap = function(type) {
          var orig = history[type];
          if (!orig) return;
          history[type] = function() {
            var ret = orig.apply(this, arguments);
            setTimeout(function() { scan(); }, 200);
            setTimeout(function() { scan(); }, 800);
            return ret;
          };
        };
        wrap('pushState');
        wrap('replaceState');
        window.addEventListener('popstate', function() {
          setTimeout(function() { scan(); }, 200);
        });
      }

      hookHistory();
      if (!window.__avdIgObs) {
        window.__avdIgObs = new MutationObserver(function() { scan(); });
        var root = document.body || document.documentElement;
        if (root) window.__avdIgObs.observe(root, { childList: true, subtree: true });
      }
      if (!window.__avdIgTimer) {
        window.__avdIgTimer = setInterval(scan, 900);
      }
      return scan();
    })();
    """.trimIndent()
}

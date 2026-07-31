package com.avd.browserkit.util

/**
 * Xilli FaceBookScript: green download button on every [div[data-video-id]].
 * Click resolves [data-video-url] (or nested video) → AndroidInterface.onVideoClicked.
 */
object FaceBookScript {

    val jsCode: String = """
    (function() {
      function decodeUrl(raw) {
        if (!raw) return '';
        var u = String(raw).replace(/&amp;/g, '&').replace(/\\\//g, '/').trim();
        if (u.indexOf('//') === 0) u = 'https:' + u;
        return u;
      }

      function unescapeFb(u) {
        if (!u) return '';
        return String(u)
          .replace(/\\\//g, '/')
          .replace(/\\u0026/g, '&')
          .replace(/\\u003A/g, ':')
          .replace(/\\u0025/g, '%')
          .replace(/&amp;/g, '&');
      }

      function isProgressiveCdn(u) {
        if (!u || u.indexOf('http') !== 0) return false;
        var l = u.toLowerCase();
        if (l.indexOf('/m78/') >= 0 || l.indexOf('/m412/') >= 0 || l.indexOf('/m366/') >= 0) {
          return false;
        }
        return l.indexOf('.mp4') >= 0 || l.indexOf('.webm') >= 0 || l.indexOf('.m4v') >= 0;
      }

      // Real progressive URLs embedded in FB page JSON (not CMAF /m78 moof stubs).
      function findPlayableFromPage() {
        try {
          var html = document.documentElement && document.documentElement.innerHTML;
          if (!html || html.length < 200) return '';
          var keys = [
            'browser_native_hd_url',
            'browser_native_sd_url',
            'playable_url_quality_hd',
            'playable_url',
            'hd_src_no_ratelimit',
            'sd_src_no_ratelimit',
            'hd_src',
            'sd_src'
          ];
          for (var k = 0; k < keys.length; k++) {
            var re = new RegExp('"' + keys[k] + '"\\s*:\\s*"([^"]+)"', 'g');
            var m;
            while ((m = re.exec(html)) !== null) {
              var u = decodeUrl(unescapeFb(m[1]));
              if (isProgressiveCdn(u) && (u.indexOf('fbcdn') >= 0 || u.indexOf('fbsbx') >= 0)) {
                return u;
              }
            }
          }
        } catch (e) {}
        return '';
      }

      // Xilli: closest div[data-video-url]. Modern FB may omit → JSON / intercept CDN.
      function resolveVideoUrl(element) {
        if (!element) return '';
        var url = '';
        if (element.closest) {
          var closest = element.closest('div[data-video-url]');
          if (closest) url = closest.getAttribute('data-video-url') || '';
        }
        if (!url && element.getAttribute) {
          url = element.getAttribute('data-video-url') || '';
        }
        url = decodeUrl(url);
        if (url.indexOf('blob:') === 0) url = '';
        if (url && !isProgressiveCdn(url) && url.indexOf('facebook.com') < 0) url = '';
        if (!url) url = findPlayableFromPage();
        if (!url && window.__avdFbLatestCdn) {
          var cdn = decodeUrl(String(window.__avdFbLatestCdn));
          if (isProgressiveCdn(cdn)) url = cdn;
        }
        return url;
      }

      function notifyAndroid(url, debug) {
        // One bridge only — BrowserKitDetector and AndroidInterface are the same object.
        try { BrowserKitDetector.onFbDebug(debug || ''); } catch (e0) {}
        try {
          BrowserKitDetector.onVideoClicked(url || '');
        } catch (e1) {
          try { AndroidInterface.onVideoClicked(url || ''); } catch (e2) {}
        }
      }

      function bindClick(button, element) {
        function handle(event) {
          if (event) {
            event.stopPropagation();
            event.preventDefault();
            if (event.stopImmediatePropagation) event.stopImmediatePropagation();
          }
          // Debounce: touchend+click would enqueue twice without this.
          var now = Date.now();
          if (window.__avdFbClickAt && (now - window.__avdFbClickAt) < 1500) {
            return false;
          }
          window.__avdFbClickAt = now;
          if (button.getAttribute('data-avd-busy') === '1') return false;
          button.setAttribute('data-avd-busy', '1');
          setTimeout(function() { button.removeAttribute('data-avd-busy'); }, 1500);

          var videoUrl = button.getAttribute('data-avd-url') || '';
          if (!videoUrl || videoUrl.indexOf('http') !== 0) {
            videoUrl = resolveVideoUrl(element);
          }
          if (videoUrl) button.setAttribute('data-avd-url', videoUrl);
          var videoId = element.getAttribute ? (element.getAttribute('data-video-id') || '') : '';
          var hasAttr = element.getAttribute && element.getAttribute('data-video-url');
          var debug = 'click hasAttr=' + (!!hasAttr) +
            ' urlLen=' + (videoUrl ? videoUrl.length : 0) +
            ' id=' + videoId +
            ' cdn=' + (!!window.__avdFbLatestCdn);
          // Empty URL → Android uses page URL / intercepted CDN (pass id as hint).
          if (!videoUrl && videoId) {
            notifyAndroid('fbvid:' + videoId, debug);
          } else {
            notifyAndroid(videoUrl, debug);
          }
          return false;
        }
        // click only — touchend+click both fire on Android WebView (double enqueue).
        button.addEventListener('click', handle, true);
      }

      function addDownloadButton(element) {
        if (!element || element.nodeType !== 1) return;
        var existing = null;
        for (var c = 0; c < element.children.length; c++) {
          var child = element.children[c];
          if (child && child.getAttribute && child.getAttribute('data-avd-fb-dl') === '1') {
            existing = child;
            break;
          }
        }
        if (existing) {
          var refreshed = resolveVideoUrl(element);
          if (refreshed) existing.setAttribute('data-avd-url', refreshed);
          return;
        }
        if (element.getAttribute('data-has-button') === 'true') {
          // Button may have been removed by FB re-render — clear flag and recreate
          if (!element.querySelector('button[data-avd-fb-dl]')) {
            element.removeAttribute('data-has-button');
          } else {
            return;
          }
        }
        element.setAttribute('data-has-button', 'true');

        var button = document.createElement('button');
        button.type = 'button';
        button.setAttribute('data-avd-fb-dl', '1');
        var initialUrl = resolveVideoUrl(element);
        if (initialUrl) button.setAttribute('data-avd-url', initialUrl);

        var iconDiv = document.createElement('div');
        iconDiv.style.cssText = 'width:100%;height:100%;display:flex;align-items:center;justify-content:center;pointer-events:none;';
        iconDiv.innerHTML =
          '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 8 12" width="15" height="26">' +
          '<path d="M3.954,9.939C4.023,10.008 4.114,10.042 4.205,10.042C4.295,10.042 4.386,10.008 4.456,9.939L7.306,7.089C7.407,6.987 7.437,6.835 7.382,6.702C7.328,6.569 7.198,6.483 7.055,6.483H5.984V3.204C5.984,3.008 5.826,2.85 5.63,2.85H2.78C2.584,2.85 2.425,3.008 2.425,3.204V6.483H1.355C1.211,6.483 1.082,6.569 1.027,6.702C0.972,6.835 1.002,6.987 1.104,7.089L3.954,9.939Z" fill="#ffffff"/>' +
          '<path d="M2.78,2.133H5.63C5.825,2.133 5.984,1.975 5.984,1.779C5.984,1.583 5.825,1.424 5.63,1.424H2.78C2.584,1.424 2.425,1.583 2.425,1.779C2.425,1.975 2.584,2.133 2.78,2.133Z" fill="#ffffff"/>' +
          '<path d="M2.78,0.71H5.63C5.825,0.71 5.984,0.551 5.984,0.355C5.984,0.159 5.825,0 5.63,0H2.78C2.584,0 2.425,0.159 2.425,0.355C2.425,0.551 2.584,0.71 2.78,0.71Z" fill="#ffffff"/>' +
          '<path d="M1,11L7.5,11A0.5,0.5 0,0 1,8 11.5L8,11.5A0.5,0.5 0,0 1,7.5 12L1,12A0.5,0.5 0,0 1,0.5 11.5L0.5,11.5A0.5,0.5 0,0 1,1 11z" fill="#ffffff"/>' +
          '</svg>';

        button.style.cssText = [
          'position:absolute',
          'right:10px',
          'top:12px',
          'width:48px',
          'height:48px',
          'border-radius:50%',
          'background-color:#2AB079',
          'border:2px solid #ffffff',
          'z-index:2147483647',
          'cursor:pointer',
          'padding:0',
          'pointer-events:auto',
          'display:flex',
          'align-items:center',
          'justify-content:center',
          'opacity:0.95',
          '-webkit-tap-highlight-color:transparent'
        ].join(';');

        button.appendChild(iconDiv);
        bindClick(button, element);
        try {
          element.style.position = 'relative';
          element.style.overflow = 'visible';
        } catch (e) {}
        element.appendChild(button);
      }

      function scanAll() {
        try {
          var list = document.querySelectorAll('div[data-video-id]');
          var withUrl = 0;
          for (var i = 0; i < list.length; i++) {
            addDownloadButton(list[i]);
            var u = resolveVideoUrl(list[i]);
            if (u) {
              withUrl++;
              // Keep button URL fresh — FB fills data-video-url after play/scroll
              var btn = null;
              for (var c = 0; c < list[i].children.length; c++) {
                if (list[i].children[c].getAttribute &&
                    list[i].children[c].getAttribute('data-avd-fb-dl') === '1') {
                  btn = list[i].children[c];
                  break;
                }
              }
              if (btn) btn.setAttribute('data-avd-url', u);
            }
          }
          return list.length + ':' + withUrl;
        } catch (e) {
          return '-1';
        }
      }

      function ensureObserver() {
        var root = document.body || document.documentElement;
        if (!root) return;
        if (window.__avdFbScriptObserver) return;
        window.__avdFbScriptObserver = new MutationObserver(function(mutations) {
          for (var m = 0; m < mutations.length; m++) {
            var nodes = mutations[m].addedNodes;
            for (var n = 0; n < nodes.length; n++) {
              var node = nodes[n];
              if (!node || node.nodeType !== 1) continue;
              if (node.matches && node.matches('div[data-video-id]')) {
                addDownloadButton(node);
              } else if (node.querySelectorAll) {
                var found = node.querySelectorAll('div[data-video-id]');
                for (var j = 0; j < found.length; j++) addDownloadButton(found[j]);
              }
            }
          }
        });
        window.__avdFbScriptObserver.observe(root, { childList: true, subtree: true });
      }

      function hookHistory() {
        if (window.__avdFbHistHooked) return;
        window.__avdFbHistHooked = true;
        var wrap = function(type) {
          var orig = history[type];
          if (!orig) return;
          history[type] = function() {
            var ret = orig.apply(this, arguments);
            setTimeout(function() { scanAll(); }, 250);
            setTimeout(function() { scanAll(); }, 900);
            return ret;
          };
        };
        wrap('pushState');
        wrap('replaceState');
        window.addEventListener('popstate', function() {
          setTimeout(function() { scanAll(); }, 250);
        });
      }

      hookHistory();
      ensureObserver();
      var count = scanAll();
      if (!window.__avdFbScanTimer) {
        window.__avdFbScanTimer = setInterval(function() {
          ensureObserver();
          scanAll();
        }, 1000);
      }
      return count;
    })();
    """.trimIndent()
}

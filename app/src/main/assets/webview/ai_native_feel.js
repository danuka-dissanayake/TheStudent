/* ---------------------------------------------------------------------------
 * TheStudent AI Assistant — native shell shim
 *
 * Injected by AiWebSession into every trusted page load, after the native
 * stylesheet. Everything is guarded and idempotent: the script can run twice
 * (onPageStarted + onPageFinished) with no effect the second time.
 *
 * What it does, and why each part exists:
 *
 *  - theme        → pulls the app accent / colour scheme so the page can match it
 *  - haptics      → light tap feedback on anything button-shaped
 *  - viewport     → keeps the composer above the keyboard instead of under it
 *  - back         → lets the page close its own drawer/modal before the app exits
 *  - links        → sends external http(s) links to the real browser
 *  - ready        → tells the app the first real frame is up, so the native
 *                   skeleton can be removed at the right moment
 * ------------------------------------------------------------------------- */
(function () {
    'use strict';

    if (window.__tsNativeFeetInstalled) return;
    window.__tsNativeFeetInstalled = true;

    var root = document.documentElement;
    var bridge = window.TheStudentAndroid || null;

    function call(name, a, b) {
        try {
            if (bridge && typeof bridge[name] === 'function') return bridge[name](a, b);
        } catch (e) {
            /* bridge detached (page navigated away) — ignore */
        }
        return undefined;
    }

    /* ── Shell marker + theme ─────────────────────────────────────────────── */

    root.classList.add('ts-android-app');

    (function applyTheme() {
        var theme = {};
        try {
            theme = JSON.parse(call('getTheme') || '{}') || {};
        } catch (e) {
            theme = {};
        }
        if (theme.accent) root.style.setProperty('--ts-accent', theme.accent);
        if (theme.mode) root.style.setProperty('color-scheme', theme.mode);
        if (typeof theme.fontScale === 'number' && theme.fontScale > 0) {
            root.setAttribute('data-ts-font-scale', String(theme.fontScale));
        }
    })();

    /* ── Haptics ──────────────────────────────────────────────────────────── */

    function haptic() {
        call('haptic');
    }

    function interactiveTarget(node) {
        if (!node || !node.closest) return null;
        return node.closest('button,[role="button"],[role="tab"],[data-haptic],a[href]');
    }

    document.addEventListener('click', function (event) {
        var el = interactiveTarget(event.target);
        if (!el) return;
        if (el.hasAttribute('data-haptic-off') || el.disabled) return;
        haptic();
    }, true);

    /* ── Keyboard / visual viewport ───────────────────────────────────────── */

    var viewport = window.visualViewport;
    var keyboardOpen = false;

    function syncViewport() {
        if (!viewport) return;
        var opened = (window.innerHeight - viewport.height) > 120;
        if (opened === keyboardOpen) return;
        keyboardOpen = opened;
        root.classList.toggle('ts-kb-open', opened);
    }

    if (viewport) {
        viewport.addEventListener('resize', syncViewport);
        viewport.addEventListener('scroll', syncViewport);
    }

    /* A focused input sitting under the keyboard is the classic web tell. Only
       nudge when it is actually clipped, so normal chat scrolling is untouched. */
    document.addEventListener('focusin', function (event) {
        var el = event.target;
        if (!el || (el.tagName !== 'TEXTAREA' && el.tagName !== 'INPUT')) return;
        setTimeout(function () {
            try {
                var visible = viewport ? viewport.height : window.innerHeight;
                var rect = el.getBoundingClientRect();
                if (rect.bottom > visible - 8 || rect.top < 8) {
                    el.scrollIntoView({ block: 'center' });
                }
            } catch (e) {
                /* element removed before the timeout — nothing to do */
            }
        }, 180);
    }, true);

    /* ── Back handling ────────────────────────────────────────────────────── */

    /* Only things the page itself can close count: dialogs, menus and anything
       the web app explicitly tags. A permanent desktop-style sidebar must never
       swallow the back press, or the student could not leave the screen. */
    var OVERLAY_SELECTOR = [
        '[aria-modal="true"]',
        '[role="dialog"][data-state="open"]',
        '[role="menu"][data-state="open"]',
        '[data-ts-drawer="open"]',
        '[data-ts-modal="open"]'
    ].join(',');

    function overlayOpen() {
        try {
            return !!document.querySelector(OVERLAY_SELECTOR);
        } catch (e) {
            return false;
        }
    }

    var lastReported = null;
    var reportScheduled = false;

    function reportBack(force) {
        var open = overlayOpen();
        if (!force && open === lastReported) return;
        lastReported = open;
        call('notifyRoute', open ? 'open' : 'idle');
    }

    var raf = window.requestAnimationFrame || function (fn) { return setTimeout(fn, 60); };

    function scheduleBackReport() {
        if (reportScheduled) return;
        reportScheduled = true;
        raf(function () {
            reportScheduled = false;
            reportBack(false);
        });
    }

    function requestBack() {
        if (!overlayOpen()) return false;

        // 1. an explicit close affordance, 2. a backdrop click, 3. Escape.
        var closer = document.querySelector(
            '[data-ts-close],[aria-modal="true"] [aria-label*="lose" i],' +
            '[role="dialog"] [aria-label*="lose" i]'
        );
        if (closer) {
            closer.click();
            reportBack(true);
            return true;
        }

        var escape = {
            key: 'Escape',
            code: 'Escape',
            keyCode: 27,
            which: 27,
            bubbles: true,
            cancelable: true
        };
        try { document.dispatchEvent(new KeyboardEvent('keydown', escape)); } catch (e) { /* older engines */ }
        try { document.dispatchEvent(new KeyboardEvent('keyup', escape)); } catch (e) { /* older engines */ }
        reportBack(true);
        return true;
    }

    window.__tsNative = {
        requestBack: requestBack,
        notifyBack: function () { reportBack(true); },
        haptic: haptic
    };

    /* Track DOM churn cheaply: the browser batches mutations, and we throttle the
       re-evaluation to one animation frame so a streaming answer cannot spam the
       bridge. */
    try {
        var observer = new MutationObserver(scheduleBackReport);
        observer.observe(root, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['data-state', 'aria-hidden', 'aria-modal', 'data-ts-drawer', 'data-ts-modal']
        });
    } catch (e) {
        /* no MutationObserver — back still works through the explicit API */
    }

    /* ── Links ────────────────────────────────────────────────────────────── */

    /* target="_blank" either does nothing or does something unpredictable in a
       WebView. Route same-origin links in place, everything else to the browser. */
    document.addEventListener('click', function (event) {
        var anchor = event.target && event.target.closest ? event.target.closest('a[href]') : null;
        if (!anchor) return;

        var href = anchor.href;
        if (!href) return;

        if (href.indexOf(location.origin) === 0) {
            if (anchor.target === '_blank') {
                event.preventDefault();
                location.href = href;
            }
            return;
        }

        if (/^https?:/i.test(href)) {
            event.preventDefault();
            call('openExternal', href);
        }
    }, true);

    /* ── Ready signal ─────────────────────────────────────────────────────── */

    var readySent = false;

    function signalReady() {
        if (readySent) return;
        readySent = true;
        call('notifyReady');
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        setTimeout(signalReady, 0);
    } else {
        document.addEventListener('DOMContentLoaded', function () {
            setTimeout(signalReady, 0);
        }, { once: true });
        // Never leave the native skeleton up because an event was missed.
        setTimeout(signalReady, 1500);
    }

    syncViewport();
    reportBack(true);
})();

/* ---------------------------------------------------------------------------
 * TheStudent AI Assistant — speech bridge polyfill
 *
 * Android's System WebView ships **no** Web Speech API. `window.webkitSpeechRecognition`
 * is undefined and `window.speechSynthesis` does not exist, so any page that uses
 * them either throws or silently does nothing — which is exactly why the website's
 * "talk" mode died inside the app while working fine in Chrome.
 *
 * This script re-creates those two interfaces on top of the native bridge
 * (window.TheStudentAndroid), which is backed by the platform SpeechRecognizer and
 * TextToSpeech engines (see AiSpeechRecognition / AiTextToSpeech).
 *
 * Existing page code keeps working unchanged:
 *     const rec = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
 *     rec.lang = 'en-US';
 *     rec.onresult = e => console.log(e.results[0][0].transcript);
 *     rec.start();
 *
 * Idempotent: injected on every page load, safe to run twice.
 * ------------------------------------------------------------------------- */
(function () {
    'use strict';

    function getBridge() {
        try {
            return window.TheStudentAndroid || null;
        } catch (e) {
            return null;
        }
    }

    // If the interface is not attached yet, bail out and let the next injection
    // (onPageCommitVisible / onPageFinished) install us.
    var bridge = getBridge();
    if (!bridge) return;
    if (window.__tsSpeechInstalled) return;
    window.__tsSpeechInstalled = true;

    /* ── native → JS plumbing ───────────────────────────────────────────────── */

    var sinks = [];

    function broadcast(type, payload) {
        var data = payload || {};
        for (var i = sinks.length - 1; i >= 0; i--) {
            try {
                sinks[i](type, data);
            } catch (e) {
                /* one consumer must never break the others */
            }
        }
    }

    window.__tsSpeech = { onNativeEvent: broadcast };

    /* ── SpeechRecognition ──────────────────────────────────────────────────── */

    var recognitionAvailable = false;
    try {
        recognitionAvailable = !!bridge.isSpeechRecognitionAvailable();
    } catch (e) {
        recognitionAvailable = false;
    }

    function SpeechResultEvent(payload) {
        var alternative = {
            transcript: payload.transcript || '',
            confidence: typeof payload.confidence === 'number' ? payload.confidence : 0
        };
        var result = [alternative];
        result.isFinal = !!payload.isFinal;
        var results = [result];
        this.type = 'result';
        this.resultIndex = 0;
        this.results = results;
        this.result = result;
        this.isFinal = !!payload.isFinal;
    }

    function Recognition() {
        var self = this;
        this.lang = 'en-US';
        this.continuous = false;
        this.interimResults = false;
        this.maxAlternatives = 1;
        this.grammars = null;
        this.serviceURI = null;

        this.onstart = null;
        this.onend = null;
        this.onerror = null;
        this.onresult = null;
        this.onnomatch = null;
        this.onaudiostart = null;
        this.onaudioend = null;
        this.onsoundstart = null;
        this.onsoundend = null;
        this.onspeechstart = null;
        this.onspeechend = null;

        this._handlers = {};
        this._running = false;
        this._attached = false;
        this._sink = function (type, payload) { self._handle(type, payload); };
    }

    Recognition.prototype.addEventListener = function (name, handler) {
        if (typeof handler !== 'function') return;
        (this._handlers[name] = this._handlers[name] || []).push(handler);
    };

    Recognition.prototype.removeEventListener = function (name, handler) {
        var list = this._handlers[name];
        if (!list) return;
        var index = list.indexOf(handler);
        if (index >= 0) list.splice(index, 1);
    };

    Recognition.prototype.dispatchEvent = function (event) {
        var name = event && event.type;
        if (!name) return true;
        return this._fire(name, event);
    };

    Recognition.prototype._fire = function (name, event) {
        var eventObject = event || { type: name };
        if (!eventObject.type) eventObject.type = name;

        var direct = this['on' + name];
        if (typeof direct === 'function') {
            try {
                direct.call(this, eventObject);
            } catch (e) {
                setTimeout(function () { throw e; }, 0);
            }
        }

        var list = this._handlers[name];
        if (list) {
            var copy = list.slice();
            for (var i = 0; i < copy.length; i++) {
                try {
                    copy[i].call(this, eventObject);
                } catch (e) {
                    setTimeout(function () { throw e; }, 0);
                }
            }
        }
        return true;
    };

    Recognition.prototype._attach = function () {
        if (this._attached) return;
        this._attached = true;
        sinks.push(this._sink);
    };

    Recognition.prototype._detach = function () {
        if (!this._attached) return;
        this._attached = false;
        var index = sinks.indexOf(this._sink);
        if (index >= 0) sinks.splice(index, 1);
    };

    Recognition.prototype._finish = function () {
        this._running = false;
        this._detach();
    };

    Recognition.prototype._handle = function (type, payload) {
        switch (type) {
            case 'start':
                this._fire('start');
                break;
            case 'audiostart':
                this._fire('audiostart');
                break;
            case 'speechstart':
                this._fire('soundstart');
                this._fire('speechstart');
                break;
            case 'speechend':
                this._fire('speechend');
                this._fire('soundend');
                break;
            case 'result':
                if (!this.interimResults && !payload.isFinal) return;
                this._fire('result', new SpeechResultEvent(payload));
                break;
            case 'nomatch':
                this._fire('nomatch', { type: 'nomatch' });
                break;
            case 'error':
                this._fire('error', {
                    type: 'error',
                    error: payload.error || 'service-not-allowed',
                    message: payload.message || ''
                });
                this._finish();
                this._fire('end');
                break;
            case 'end':
                this._finish();
                this._fire('audioend');
                this._fire('end');
                break;
            default:
                break;
        }
    };

    Recognition.prototype.start = function () {
        if (this._running) {
            throw new Error('InvalidStateError: recognition has already started');
        }
        this._running = true;
        this._attach();

        var started = false;
        try {
            started = !!bridge.startSpeechRecognition(this.lang || 'en-US', !!this.continuous);
        } catch (e) {
            started = false;
        }

        if (!started) {
            this._finish();
            this._fire('error', {
                type: 'error',
                error: 'service-not-allowed',
                message: 'Speech recognition is not available on this device'
            });
            this._fire('end');
        }
    };

    Recognition.prototype.stop = function () {
        try {
            bridge.stopSpeechRecognition();
        } catch (e) {
            /* bridge gone */
        }
    };

    Recognition.prototype.abort = function () {
        this._finish();
        try {
            bridge.cancelSpeechRecognition();
        } catch (e) {
            /* bridge gone */
        }
        this._fire('end');
    };

    if (recognitionAvailable) {
        if (!window.SpeechRecognition) window.SpeechRecognition = Recognition;
        if (!window.webkitSpeechRecognition) window.webkitSpeechRecognition = Recognition;
    }

    /* ── speechSynthesis ────────────────────────────────────────────────────── */

    if (!window.speechSynthesis) {
        var currentUtterance = null;

        function Utterance(text) {
            this.text = text == null ? '' : String(text);
            this.lang = 'en-US';
            this.rate = 1;
            this.pitch = 1;
            this.volume = 1;
            this.voice = null;
            this.onstart = null;
            this.onend = null;
            this.onerror = null;
            this.onpause = null;
            this.onresume = null;
            this.onboundary = null;
        }

        if (!window.SpeechSynthesisUtterance) {
            window.SpeechSynthesisUtterance = Utterance;
        }

        var emitUtterance = function (utterance, name, extra) {
            if (!utterance) return;
            var event = { type: name, utterance: utterance, charIndex: 0 };
            if (extra) {
                for (var key in extra) {
                    if (Object.prototype.hasOwnProperty.call(extra, key)) event[key] = extra[key];
                }
            }
            var handler = utterance['on' + name];
            if (typeof handler === 'function') {
                try {
                    handler.call(utterance, event);
                } catch (e) {
                    /* page handler error must not break the queue */
                }
            }
        };

        var synthesis = {
            speaking: false,
            pending: false,
            paused: false,
            speak: function (utterance) {
                if (!utterance) return;
                currentUtterance = utterance;
                this.speaking = true;
                try {
                    bridge.speakText(utterance.text || '', utterance.lang || 'en-US');
                } catch (e) {
                    this.speaking = false;
                    emitUtterance(utterance, 'error', { error: 'synthesis-unavailable' });
                }
            },
            cancel: function () {
                this.speaking = false;
                currentUtterance = null;
                try {
                    bridge.stopSpeaking();
                } catch (e) {
                    /* bridge gone */
                }
            },
            pause: function () {
                this.paused = true;
                try {
                    bridge.stopSpeaking();
                } catch (e) {
                    /* bridge gone */
                }
            },
            resume: function () {
                this.paused = false;
            },
            getVoices: function () {
                return [];
            },
            addEventListener: function () {},
            removeEventListener: function () {},
            dispatchEvent: function () {
                return true;
            }
        };

        window.speechSynthesis = synthesis;

        sinks.push(function (type, payload) {
            var utterance = currentUtterance;
            if (type === 'ttsstart') {
                emitUtterance(utterance, 'start');
            } else if (type === 'ttsend') {
                synthesis.speaking = false;
                emitUtterance(utterance, 'end');
            } else if (type === 'ttserror') {
                synthesis.speaking = false;
                emitUtterance(utterance, 'error', { error: 'synthesis-failed' });
            }
        });
    }
})();

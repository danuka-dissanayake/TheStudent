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
    window.__tsNative = window.__tsSpeech;

    // The app calls this one function for every native event: speech, TTS and
    // voice recording alike. It is a no-op if the shim never installed.
    window.__tsDispatch = function (type, payload) {
        broadcast(type, payload);
    };

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

    /* ── Voice messages (native recorder) ──────────────────────────────────────
     *
     * The page's own recorder is getUserMedia + MediaRecorder, which is the right
     * thing to use when the WebView supports it. When it does not — or when the
     * page's capability probe ran before the microphone was ever granted — this
     * gives it a guaranteed path that produces an ordinary Blob:
     *
     *     const rec = window.TheStudentVoiceRecorder;
     *     if (rec.isAvailable()) {
     *         await rec.start();          // records AAC in an .m4a container
     *         const note = await rec.stop();   // { blob, mimeType, durationMs, size }
     *         form.append('audio', note.blob, 'voice.m4a');
     *     }
     */

    var recorder = null;

    function resetRecorder() {
        if (!recorder) return;
        if (recorder.timer) clearTimeout(recorder.timer);
        recorder = null;
    }

    function base64ToBytes(b64) {
        var binary = atob(b64);
        var length = binary.length;
        var bytes = new Uint8Array(length);
        for (var i = 0; i < length; i++) bytes[i] = binary.charCodeAt(i);
        return bytes;
    }

    sinks.push(function (type, payload) {
        if (type === 'recordstart') {
            if (recorder && recorder.startResolve) {
                recorder.startResolve();
                recorder.startResolve = null;
                recorder.startReject = null;
                if (recorder.timer) {
                    clearTimeout(recorder.timer);
                    recorder.timer = null;
                }
            }
            return;
        }

        if (type === 'recordchunk') {
            if (recorder) recorder.chunks[payload.index || 0] = payload.data || '';
            return;
        }

        if (type === 'recordend') {
            var pending = recorder;
            resetRecorder();
            if (!pending || !pending.stopResolve) return;
            if (payload && payload.aborted) {
                pending.stopReject(new Error('Recording was aborted'));
                return;
            }
            try {
                var parts = [];
                for (var i = 0; i < pending.chunks.length; i++) {
                    if (pending.chunks[i]) parts.push(base64ToBytes(pending.chunks[i]));
                }
                var blob = new Blob(parts, { type: (payload && payload.mime) || 'audio/mp4' });
                pending.stopResolve({
                    blob: blob,
                    mimeType: blob.type,
                    durationMs: (payload && payload.durationMs) || 0,
                    size: blob.size
                });
            } catch (err) {
                pending.stopReject(err);
            }
            return;
        }

        if (type === 'recorderror') {
            var failed = recorder;
            resetRecorder();
            if (!failed) return;
            var error = new Error((payload && payload.message) || 'Recording failed');
            error.name = (payload && payload.error) || 'RecordingError';
            if (failed.stopReject) failed.stopReject(error);
            if (failed.startReject) failed.startReject(error);
        }
    });

    window.TheStudentVoiceRecorder = {
        /** True when the native recorder can be used on this device. */
        isAvailable: function () {
            try {
                return !!(bridge.isAudioRecordingAvailable && bridge.isAudioRecordingAvailable());
            } catch (e) {
                return false;
            }
        },

        isRecording: function () {
            return !!recorder;
        },

        /** Resolves once the recorder is really capturing (after any mic prompt). */
        start: function () {
            if (recorder) return Promise.reject(new Error('A recording is already running'));

            var started = false;
            try {
                started = !!bridge.startAudioRecording();
            } catch (e) {
                started = false;
            }
            if (!started) {
                return Promise.reject(new Error('Recording is not available on this device'));
            }

            return new Promise(function (resolve, reject) {
                recorder = {
                    chunks: [],
                    startResolve: resolve,
                    startReject: reject,
                    stopResolve: null,
                    stopReject: null,
                    timer: setTimeout(function () {
                        // A permission dialog can sit for a while; do not leave the
                        // page's UI spinning if the native event never lands.
                        if (recorder && recorder.startResolve) {
                            recorder.startResolve();
                            recorder.startResolve = null;
                        }
                    }, 15000)
                };
            });
        },

        /** Stops and resolves with { blob, mimeType, durationMs, size }. */
        stop: function () {
            if (!recorder) return Promise.reject(new Error('There is no recording to stop'));

            var active = recorder;
            var promise = new Promise(function (resolve, reject) {
                active.stopResolve = resolve;
                active.stopReject = reject;
            });

            if (active.timer) clearTimeout(active.timer);
            active.timer = setTimeout(function () {
                var hung = recorder;
                resetRecorder();
                if (hung && hung.stopReject) hung.stopReject(new Error('Recording did not finish'));
            }, 8000);

            try {
                bridge.stopAudioRecording();
            } catch (e) {
                var failed = recorder;
                resetRecorder();
                if (failed && failed.stopReject) failed.stopReject(e);
            }
            return promise;
        },

        /** Throws the recording away without producing any audio. */
        cancel: function () {
            var pending = recorder;
            resetRecorder();
            try {
                bridge.cancelAudioRecording();
            } catch (e) {
                /* bridge gone */
            }
            var error = new Error('Recording was cancelled');
            if (pending && pending.startReject) pending.startReject(error);
            if (pending && pending.stopReject) pending.stopReject(error);
        }
    };

    /* ── Diagnostics ───────────────────────────────────────────────────────────
     *
     * `TheStudentDiagnostics.media()` answers "why won't this page record?" in one
     * line, from chrome://inspect against a debug build.
     */

    window.TheStudentDiagnostics = {
        media: function () {
            var report = {
                secureContext: !!window.isSecureContext,
                mediaDevices: !!navigator.mediaDevices,
                getUserMedia: !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia),
                mediaRecorder: typeof window.MediaRecorder !== 'undefined',
                audioContext: !!(window.AudioContext || window.webkitAudioContext),
                speechRecognition: !!(window.SpeechRecognition || window.webkitSpeechRecognition),
                speechSynthesis: !!window.speechSynthesis,
                nativeRecorder: false,
                audioPermission: false,
                userAgent: navigator.userAgent
            };
            try {
                report.nativeRecorder = !!(bridge.isAudioRecordingAvailable && bridge.isAudioRecordingAvailable());
                report.audioPermission = !!(bridge.hasAudioPermission && bridge.hasAudioPermission());
            } catch (e) {
                /* bridge gone */
            }
            try {
                report.mediaRecorderTypes = [];
                if (window.MediaRecorder && window.MediaRecorder.isTypeSupported) {
                    ['audio/webm', 'audio/webm;codecs=opus', 'audio/mp4', 'audio/ogg'].forEach(function (type) {
                        if (window.MediaRecorder.isTypeSupported(type)) report.mediaRecorderTypes.push(type);
                    });
                }
            } catch (e) {
                report.mediaRecorderTypes = [];
            }
            return report;
        }
    };
})();

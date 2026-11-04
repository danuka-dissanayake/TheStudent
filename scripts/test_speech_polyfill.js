#!/usr/bin/env node
/*
 * Regression test for assets/webview/ai_speech.js — the Web Speech API polyfill
 * that gives the AI Assistant screen speech recognition and speech synthesis
 * inside Android's WebView (which ships neither).
 *
 * No device needed: the bridge is stubbed and the native events are fired by hand,
 * which is exactly how they arrive from AiSpeechRecognition / AiTextToSpeech.
 *
 * Run:  node scripts/test_speech_polyfill.js
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const calls = [];
const sandbox = { console, setTimeout, clearTimeout, atob, btoa, Blob };
sandbox.window = sandbox;
// The shim reads these the way it would in a page. MediaRecorder is deliberately
// absent: that is the WebView case this fallback exists for.
sandbox.navigator = { userAgent: 'TheStudentTest/1.0' };
sandbox.isSecureContext = true;
sandbox.TheStudentAndroid = {
  isSpeechRecognitionAvailable: () => true,
  startSpeechRecognition: (lang, cont) => { calls.push(['start', lang, cont]); return true; },
  stopSpeechRecognition: () => calls.push(['stop']),
  cancelSpeechRecognition: () => calls.push(['cancel']),
  speakText: (t, l) => calls.push(['speak', t, l]),
  stopSpeaking: () => calls.push(['stopSpeaking']),
  isAudioRecordingAvailable: () => true,
  startAudioRecording: () => { calls.push(['recStart']); return true; },
  stopAudioRecording: () => calls.push(['recStop']),
  cancelAudioRecording: () => calls.push(['recCancel']),
  hasAudioPermission: () => true
};
vm.createContext(sandbox);
vm.runInContext(
  fs.readFileSync(path.join(__dirname, '..', 'app/src/main/assets/webview/ai_speech.js'), 'utf8'),
  sandbox
);

// ── SpeechRecognition surface ───────────────────────────────────────────────
const SR = sandbox.window.webkitSpeechRecognition || sandbox.window.SpeechRecognition;
if (!SR) throw new Error('FAIL: webkitSpeechRecognition was not installed');
if (sandbox.window.SpeechRecognition !== SR) throw new Error('FAIL: SpeechRecognition alias missing');

const rec = new SR();
rec.lang = 'si-LK';
rec.continuous = false;
rec.interimResults = true;

const events = [];
['start', 'audiostart', 'speechstart', 'soundstart', 'speechend', 'soundend', 'result', 'end', 'error', 'nomatch']
  .forEach((name) => rec.addEventListener(name, () => events.push(name)));
rec.onresult = (e) => {
  events.push('onresult:' + e.results[0][0].transcript + ':' + e.results[0].isFinal);
};

rec.start();
if (calls[0][0] !== 'start' || calls[0][1] !== 'si-LK') {
  throw new Error('FAIL: start was not forwarded with the page language: ' + JSON.stringify(calls));
}

// the native side drives everything from here
const fire = (type, payload) => sandbox.window.__tsSpeech.onNativeEvent(type, payload);
fire('start', {});
fire('speechstart', {});
fire('result', { transcript: 'hello bro', isFinal: false, confidence: 0.4 });
fire('result', { transcript: 'hello bro how are you', isFinal: true, confidence: 0.94 });
fire('end', {});

if (!events.includes('onresult:hello bro how are you:true')) {
  throw new Error('FAIL: final result was not delivered. events=' + JSON.stringify(events));
}
if (events.filter((e) => e === 'end').length !== 1) {
  throw new Error('FAIL: end should fire exactly once. events=' + JSON.stringify(events));
}

// ── error path ──────────────────────────────────────────────────────────────
const rec2 = new SR();
const errs = [];
rec2.onerror = (e) => errs.push('onerror:' + e.error);
rec2.onend = () => errs.push('onend');
rec2.start();
fire('error', { error: 'no-speech', message: 'nothing heard' });
if (!errs.includes('onerror:no-speech') || !errs.includes('onend')) {
  throw new Error('FAIL: error path. errs=' + JSON.stringify(errs));
}

// ── abort detaches the recogniser from the native event stream ──────────────
rec2.start();
rec2.abort();
fire('result', { transcript: 'should be ignored', isFinal: true });
if (errs.filter((e) => e === 'onerror:no-speech').length !== 1) {
  throw new Error('FAIL: an aborted recogniser kept receiving events');
}

// ── speechSynthesis surface ─────────────────────────────────────────────────
const synth = sandbox.window.speechSynthesis;
if (!synth) throw new Error('FAIL: speechSynthesis was not installed');

const utterance = new sandbox.window.SpeechSynthesisUtterance('Good morning students');
utterance.lang = 'en-GB';
let started = false;
let ended = false;
utterance.onstart = () => { started = true; };
utterance.onend = () => { ended = true; };

synth.speak(utterance);
const speakCall = calls.find((c) => c[0] === 'speak');
if (!speakCall || speakCall[1] !== 'Good morning students' || speakCall[2] !== 'en-GB') {
  throw new Error('FAIL: speak was not forwarded: ' + JSON.stringify(speakCall));
}
fire('ttsstart', {});
fire('ttsend', {});
if (!started || !ended) throw new Error('FAIL: utterance start/end events missing');
if (synth.speaking) throw new Error('FAIL: the speaking flag was left stuck');

// ── voice recorder: chunked base64 assembly into a Blob ────────────────────
(async function testRecorder() {
  const voice = sandbox.window.TheStudentVoiceRecorder;
  if (!voice) throw new Error('FAIL: TheStudentVoiceRecorder was not installed');
  if (!voice.isAvailable()) throw new Error('FAIL: native recorder reported unavailable');

  // 3000 known bytes, delivered in two 4-aligned base64 chunks like the native side
  const payload = Buffer.alloc(3000);
  for (let i = 0; i < payload.length; i++) payload[i] = i % 256;
  const encoded = payload.toString('base64');
  const cut = 2000 - (2000 % 4);

  const started = voice.start();
  fire('recordstart', {});
  await started;

  const stopped = voice.stop();
  fire('recordchunk', { index: 0, data: encoded.slice(0, cut) });
  fire('recordchunk', { index: 1, data: encoded.slice(cut) });
  fire('recordend', { chunks: 2, mime: 'audio/mp4', durationMs: 4200, size: 3000 });

  const note = await stopped;
  if (note.mimeType !== 'audio/mp4') throw new Error('FAIL: wrong mime ' + note.mimeType);
  if (note.durationMs !== 4200) throw new Error('FAIL: wrong duration ' + note.durationMs);
  if (note.size !== 3000) throw new Error('FAIL: wrong size ' + note.size);

  const bytes = new Uint8Array(await note.blob.arrayBuffer());
  for (let i = 0; i < bytes.length; i++) {
    if (bytes[i] !== payload[i]) throw new Error('FAIL: blob corrupted at byte ' + i);
  }
  if (voice.isRecording()) throw new Error('FAIL: still marked as recording');

  // a denied microphone must reject, not hang
  const deniedStart = voice.start();
  fire('recordstart', {});
  await deniedStart;
  const denied = voice.stop();
  fire('recorderror', { error: 'not-allowed', message: 'Microphone permission was denied' });
  let rejected = false;
  try {
    await denied;
  } catch (e) {
    rejected = /denied/.test(e.message);
  }
  if (!rejected) throw new Error('FAIL: a denied recording did not reject');

  // diagnostics must answer the "why can't I record?" question
  const report = sandbox.window.TheStudentDiagnostics.media();
  if (typeof report.getUserMedia !== 'boolean' || report.nativeRecorder !== true) {
    throw new Error('FAIL: diagnostics incomplete: ' + JSON.stringify(report));
  }

  console.log('ALL SPEECH POLYFILL TESTS PASSED');
  console.log('recognition events:', events.join(' | '));
  console.log('voice note assembled:', note.size, 'bytes,', note.durationMs + 'ms,', note.mimeType);
})();

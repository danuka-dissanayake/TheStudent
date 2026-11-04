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
const sandbox = { console, setTimeout, clearTimeout };
sandbox.window = sandbox;
sandbox.TheStudentAndroid = {
  isSpeechRecognitionAvailable: () => true,
  startSpeechRecognition: (lang, cont) => { calls.push(['start', lang, cont]); return true; },
  stopSpeechRecognition: () => calls.push(['stop']),
  cancelSpeechRecognition: () => calls.push(['cancel']),
  speakText: (t, l) => calls.push(['speak', t, l]),
  stopSpeaking: () => calls.push(['stopSpeaking'])
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

console.log('ALL SPEECH POLYFILL TESTS PASSED');
console.log('recognition events:', events.join(' | '));

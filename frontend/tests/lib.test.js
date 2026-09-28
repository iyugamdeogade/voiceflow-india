// Run with:  node --test frontend/tests/lib.test.js
const test = require("node:test");
const assert = require("node:assert/strict");
const VF = require("../lib.js");

test("formatTime pads minutes and seconds", () => {
  assert.equal(VF.formatTime(0), "00:00");
  assert.equal(VF.formatTime(65.9), "01:05");
  assert.equal(VF.formatTime(3599), "59:59");
  assert.equal(VF.formatTime(-4), "00:00");
});

test("countWords handles empty, spaces, newlines and Devanagari", () => {
  assert.equal(VF.countWords(""), 0);
  assert.equal(VF.countWords("   \n "), 0);
  assert.equal(VF.countWords("hello   world\nagain"), 3);
  assert.equal(VF.countWords("मैं घर जा रहा हूँ"), 5);
  assert.equal(VF.countWords(null), 0);
});

test("countChars counts visible characters, not UTF-16 units", () => {
  assert.equal(VF.countChars("abc"), 3);
  assert.equal(VF.countChars("a😀b"), 3);
});

test("pickMimeType returns the first supported type, or empty string", () => {
  assert.equal(VF.pickMimeType((t) => t === "audio/webm"), "audio/webm");
  assert.equal(VF.pickMimeType(() => true), VF.MIME_CANDIDATES[0]);
  assert.equal(VF.pickMimeType(() => false), "");
  assert.equal(VF.pickMimeType((t) => { if (t.startsWith("audio/webm")) throw new Error("boom"); return t === "audio/mp4"; }), "audio/mp4");
});

test("extensionForMime ignores codec parameters", () => {
  assert.equal(VF.extensionForMime("audio/webm;codecs=opus"), "webm");
  assert.equal(VF.extensionForMime("audio/mp4"), "mp4");
  assert.equal(VF.extensionForMime("audio/ogg; codecs=opus"), "ogg");
  assert.equal(VF.extensionForMime(""), "webm");
});

test("micErrorCode maps browser errors", () => {
  assert.equal(VF.micErrorCode({ name: "NotAllowedError" }), "MIC_DENIED");
  assert.equal(VF.micErrorCode({ name: "NotFoundError" }), "NO_MIC");
  assert.equal(VF.micErrorCode({ name: "NotReadableError" }), "MIC_BUSY");
  assert.equal(VF.micErrorCode({ name: "Weird" }), "MIC_ERROR");
  assert.equal(VF.micErrorCode(null), "MIC_ERROR");
});

test("messageForError prefers server text for provider errors and friendly text for client errors", () => {
  assert.match(VF.messageForError("MIC_DENIED"), /Microphone access was blocked/);
  assert.equal(VF.messageForError("SPEECH_API_KEY_MISSING", "Set SPEECH_API_KEY"), "Set SPEECH_API_KEY");
  assert.equal(VF.messageForError("AUDIO_TOO_LARGE", "Record under 25 MB"), "Record under 25 MB");
  assert.match(VF.messageForError("UNKNOWN"), /Something went wrong/);
});

test("previewText collapses whitespace and truncates", () => {
  assert.equal(VF.previewText("a   b\n c", 50), "a b c");
  assert.equal(VF.previewText("abcdefghij", 5), "abcde\u2026");
});

test("languageLabel", () => {
  assert.equal(VF.languageLabel("hi"), "Hindi");
  assert.equal(VF.languageLabel("zz"), "zz");
});

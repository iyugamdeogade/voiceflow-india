/* Small pure helpers, kept separate from app.js so they can be unit-tested with Node. */
(function (root) {
  "use strict";

  const MIME_CANDIDATES = [
    "audio/webm;codecs=opus",
    "audio/webm",
    "audio/ogg;codecs=opus",
    "audio/mp4",
  ];

  const LANGUAGES = { en: "English", hi: "Hindi", mr: "Marathi" };

  /** Picks the first recording format the browser really supports (never assumes one). */
  function pickMimeType(isSupported) {
    for (const type of MIME_CANDIDATES) {
      try {
        if (isSupported(type)) return type;
      } catch (_) {
        /* ignore and try the next one */
      }
    }
    return "";
  }

  function extensionForMime(mime) {
    const base = String(mime || "").split(";")[0].trim().toLowerCase();
    if (base === "audio/webm" || base === "video/webm") return "webm";
    if (base === "audio/ogg") return "ogg";
    if (base === "audio/mp4" || base === "audio/x-m4a") return "mp4";
    if (base === "audio/mpeg" || base === "audio/mp3") return "mp3";
    if (base === "audio/wav" || base === "audio/x-wav" || base === "audio/wave") return "wav";
    if (base === "audio/flac" || base === "audio/x-flac") return "flac";
    return "webm";
  }

  function formatTime(totalSeconds) {
    const s = Math.max(0, Math.floor(totalSeconds));
    const mm = String(Math.floor(s / 60)).padStart(2, "0");
    const ss = String(s % 60).padStart(2, "0");
    return mm + ":" + ss;
  }

  function countWords(text) {
    const t = String(text || "").trim();
    return t ? t.split(/\s+/).length : 0;
  }

  /** Counts characters as people see them (an emoji or Devanagari letter is not split in two). */
  function countChars(text) {
    return Array.from(String(text || "")).length;
  }

  function previewText(text, max) {
    const flat = String(text || "").replace(/\s+/g, " ").trim();
    const chars = Array.from(flat);
    return chars.length > max ? chars.slice(0, max).join("") + "\u2026" : flat;
  }

  function languageLabel(code) {
    return LANGUAGES[code] || code;
  }

  /** Maps a getUserMedia error to one of our error codes. */
  function micErrorCode(err) {
    const name = err && err.name;
    if (name === "NotAllowedError" || name === "SecurityError" || name === "PermissionDeniedError") return "MIC_DENIED";
    if (name === "NotFoundError" || name === "DevicesNotFoundError" || name === "OverconstrainedError") return "NO_MIC";
    if (name === "NotReadableError" || name === "TrackStartError" || name === "AbortError") return "MIC_BUSY";
    return "MIC_ERROR";
  }

  const MESSAGES = {
    UNSUPPORTED_BROWSER: "This browser cannot record audio. Try the latest Chrome, Edge, Firefox or Safari.",
    MIC_DENIED: "Microphone access was blocked. Click the lock icon in the address bar, allow the microphone, then try again.",
    NO_MIC: "No microphone was found. Connect one and try again.",
    MIC_BUSY: "The microphone is being used by another app. Close it and try again.",
    MIC_ERROR: "The microphone could not be started. Check your device settings and try again.",
    RECORDING_EMPTY: "Nothing was recorded. Check your microphone and try again.",
    RECORDING_TOO_SHORT: "That recording was too short. Hold on for at least one second while you speak.",
    NETWORK_OFFLINE: "You appear to be offline. Check your internet connection and try again.",
    SERVER_UNREACHABLE: "Cannot reach the VoiceFlow backend. Make sure it is running and the address in config.js is correct.",
    AUDIO_TOO_LARGE: "The recording is too large. Record a shorter clip.",
    AUDIO_UNSUPPORTED_FORMAT: "The server does not accept this audio format.",
    HISTORY_DISABLED: "History is turned off because the backend is running in demo mode (no database).",
    DATABASE_UNAVAILABLE: "The database cannot be reached. Check that MySQL is running.",
    INTERNAL_ERROR: "Something went wrong on the server. Please try again.",
  };

  /** Prefers our own wording, then the server's message, then a generic fallback. */
  function messageForError(code, serverMessage) {
    if (MESSAGES[code] && !(serverMessage && code.startsWith("AUDIO_"))) return MESSAGES[code];
    if (serverMessage) return serverMessage;
    if (MESSAGES[code]) return MESSAGES[code];
    return "Something went wrong. Please try again.";
  }

  const api = {
    MIME_CANDIDATES, LANGUAGES, pickMimeType, extensionForMime, formatTime,
    countWords, countChars, previewText, languageLabel, micErrorCode, messageForError,
  };

  if (typeof module === "object" && module.exports) module.exports = api;
  else root.VF = api;
})(typeof self !== "undefined" ? self : this);

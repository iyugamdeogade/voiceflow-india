/* VoiceFlow India frontend. Plain JavaScript, no framework. Pure helpers live in lib.js (window.VF). */
(function () {
  "use strict";

  const API_BASE = (window.VOICEFLOW_API_BASE || "http://localhost:8080").replace(/\/$/, "");
  const MIN_SECONDS = 1;
  const SETTINGS_KEY = "voiceflow.settings";
  const HISTORY_PAGE_SIZE = 10;
  const DEFAULT_SETTINGS = {
    defaultLanguage: "en", fixWhitespace: true, fixPunctuation: true,
    capitalize: true, removeFillers: false, autoCleanup: false,
  };

  const $ = (id) => document.getElementById(id);
  const el = {
    banner: $("banner"), toast: $("toast"),
    detectedLanguage: $("detectedLanguage"), micBtn: $("micBtn"), timer: $("timer"), status: $("status"), statusText: $("statusText"),
    pauseBtn: $("pauseBtn"), stopBtn: $("stopBtn"), cancelBtn: $("cancelBtn"), fileBtn: $("fileBtn"), fileInput: $("fileInput"),
    editor: $("editor"), counts: $("counts"), copyBtn: $("copyBtn"), cleanBtn: $("cleanBtn"), undoBtn: $("undoBtn"),
    clearBtn: $("clearBtn"), saveBtn: $("saveBtn"), originalBox: $("originalBox"), originalText: $("originalText"), restoreBtn: $("restoreBtn"),
    historyList: $("historyList"), historyEmpty: $("historyEmpty"), historyError: $("historyError"), moreBtn: $("moreBtn"), clearHistoryBtn: $("clearHistoryBtn"),
    setWhitespace: $("setWhitespace"), setPunctuation: $("setPunctuation"), setCapitalize: $("setCapitalize"),
    setFillers: $("setFillers"), setAuto: $("setAuto"), saveSettingsBtn: $("saveSettingsBtn"), settingsNote: $("settingsNote"),
  };

  const state = {
    status: "idle",            // idle | recording | paused | processing | error
    recorder: null, stream: null, chunks: [], mimeType: "",
    startedAt: 0, accumulated: 0, timerId: null,
    discard: false, controller: null,
    originalText: "",          // what the speech service returned (never edited)
    savedText: "",             // editor text at last save (to detect unsaved changes)
    historyId: null,           // set once the current text is saved (or loaded from history)
    undoText: null,            // text before the last cleanup
    settings: { ...DEFAULT_SETTINGS },
    historyPage: 0, historyTotal: 0, historyLoaded: 0,
    detectedLanguage: "auto",
  };

  class ApiError extends Error {
    constructor(code, serverMessage) {
      super(VF.messageForError(code, serverMessage));
      this.code = code;
    }
  }

  /* ------------------------------------------------------------------ helpers */

  function toast(message) {
    el.toast.textContent = message;
    el.toast.hidden = false;
    clearTimeout(toast.t);
    toast.t = setTimeout(() => { el.toast.hidden = true; }, 4500);
  }

  function showBanner(message, isError) {
    el.banner.textContent = message;
    el.banner.classList.toggle("error", !!isError);
    el.banner.hidden = !message;
  }

  function setStatus(status, text) {
    state.status = status;
    if (status === "idle") state.accumulated = 0;   // timer goes back to 00:00
    el.status.dataset.state = status;
    const defaults = {
      idle: "Idle. Press the microphone to start.",
      recording: "Recording. Press stop when you are done.",
      paused: "Paused.",
      processing: "Uploading and transcribing. This can take a few seconds.",
      error: "Something went wrong.",
    };
    el.statusText.textContent = text || defaults[status];
    render();
  }

  function fail(err) {
    const message = err instanceof ApiError || err.code ? err.message : VF.messageForError("UNKNOWN");
    setStatus("error", message);
  }

  function elapsedSeconds() {
    return (state.accumulated + (state.status === "recording" ? Date.now() - state.startedAt : 0)) / 1000;
  }

  function render() {
    const s = state.status;
    const recording = s === "recording" || s === "paused";
    el.micBtn.dataset.recording = String(recording);
    el.micBtn.disabled = s === "processing";
    el.micBtn.setAttribute("aria-label", recording ? "Stop recording" : "Start recording");
    const voiceShell = document.querySelector(".voice-shell");
    if (voiceShell) {
      voiceShell.classList.toggle("is-heard", s === "idle" && state.detectedLanguage !== "auto");
    }
    el.pauseBtn.hidden = !recording;
    el.pauseBtn.textContent = s === "paused" ? "Resume" : "Pause";
    el.stopBtn.hidden = !recording;
    el.cancelBtn.hidden = !(recording || s === "processing");
    el.cancelBtn.textContent = s === "processing" ? "Cancel transcription" : "Discard recording";
    el.fileBtn.disabled = recording || s === "processing";
    el.timer.textContent = VF.formatTime(elapsedSeconds());
  }

  function updateCounts() {
    const t = el.editor.value;
    el.counts.textContent = VF.countWords(t) + " words · " + VF.countChars(t) + " characters";
  }

  function updateDetectedLanguageLabel(language) {
    const langCode = (language || state.detectedLanguage || "auto").toLowerCase();
    state.detectedLanguage = langCode;
    const label = langCode === "auto" || !langCode ? "Auto-detecting" : VF.languageLabel(langCode);
    if (el.detectedLanguage) el.detectedLanguage.textContent = "Detected: " + label;
    const voiceShell = document.querySelector(".voice-shell");
    if (voiceShell) {
      voiceShell.classList.toggle("is-heard", langCode !== "auto" && langCode !== "");
    }
  }

  function hasUnsavedText() {
    return el.editor.value.trim() !== "" && el.editor.value !== state.savedText;
  }

  function showOriginal() {
    const has = state.originalText.trim() !== "";
    el.originalBox.hidden = !has;
    el.originalText.textContent = state.originalText;
  }

  /* ------------------------------------------------------------------ backend calls */

  async function api(path, options) {
    let res;
    try {
      res = await fetch(API_BASE + path, options);
    } catch (e) {
      if (e && e.name === "AbortError") throw e;
      throw new ApiError(navigator.onLine === false ? "NETWORK_OFFLINE" : "SERVER_UNREACHABLE");
    }
    let json = null;
    try { json = await res.json(); } catch (_) { /* not JSON */ }
    if (!res.ok || !json || json.success !== true) {
      throw new ApiError((json && json.error && json.error.code) || "HTTP_" + res.status, json && json.error && json.error.message);
    }
    return json.data;
  }

  const jsonRequest = (method, body) => ({
    method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  });

  function cleanupOptions() {
    const s = state.settings;
    return { fixWhitespace: s.fixWhitespace, fixPunctuation: s.fixPunctuation, capitalize: s.capitalize, removeFillers: s.removeFillers };
  }

  /* ------------------------------------------------------------------ recording */

  async function startRecording() {
    if (state.status !== "idle" && state.status !== "error") return;
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || typeof MediaRecorder === "undefined") {
      return fail(new ApiError("UNSUPPORTED_BROWSER"));
    }
    try {
      // The browser asks for permission here, only after the user pressed the button.
      state.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
      return fail(new ApiError(VF.micErrorCode(e)));
    }
    try {
      state.mimeType = VF.pickMimeType((t) => MediaRecorder.isTypeSupported(t));
      state.recorder = state.mimeType ? new MediaRecorder(state.stream, { mimeType: state.mimeType }) : new MediaRecorder(state.stream);
    } catch (e) {
      releaseMic();
      return fail(new ApiError("UNSUPPORTED_BROWSER"));
    }
    state.chunks = [];
    state.discard = false;
    state.accumulated = 0;
    state.recorder.ondataavailable = (ev) => { if (ev.data && ev.data.size > 0) state.chunks.push(ev.data); };
    state.recorder.onstop = onRecorderStopped;
    state.recorder.onerror = () => { releaseMic(); stopTimer(); fail(new ApiError("MIC_ERROR")); };
    state.recorder.start();
    state.startedAt = Date.now();
    setStatus("recording");
    state.timerId = setInterval(() => { el.timer.textContent = VF.formatTime(elapsedSeconds()); }, 250);
  }

  function togglePause() {
    if (!state.recorder) return;
    if (state.status === "recording" && state.recorder.state === "recording") {
      state.recorder.pause();
      state.accumulated += Date.now() - state.startedAt;
      setStatus("paused");
    } else if (state.status === "paused" && state.recorder.state === "paused") {
      state.recorder.resume();
      state.startedAt = Date.now();
      setStatus("recording");
    }
  }

  function stopRecording() {
    if (!state.recorder || state.recorder.state === "inactive") return;
    if (state.status === "recording") state.accumulated += Date.now() - state.startedAt;
    state.recorder.stop();   // triggers onRecorderStopped
  }

  function discardRecording() {
    state.discard = true;
    if (state.recorder && state.recorder.state !== "inactive") {
      state.recorder.stop();
    }
  }

  function stopTimer() {
    clearInterval(state.timerId);
    state.timerId = null;
  }

  function releaseMic() {
    if (state.stream) state.stream.getTracks().forEach((t) => t.stop());
    state.stream = null;
  }

  async function onRecorderStopped() {
    const seconds = state.accumulated / 1000;
    const type = (state.recorder && state.recorder.mimeType) || state.mimeType || "audio/webm";
    stopTimer();
    releaseMic();
    const blob = new Blob(state.chunks, { type });
    state.chunks = [];
    state.recorder = null;
    if (state.discard) {
      state.accumulated = 0;
      setStatus("idle");
      return;
    }
    if (blob.size === 0) return fail(new ApiError("RECORDING_EMPTY"));
    if (seconds < MIN_SECONDS) return fail(new ApiError("RECORDING_TOO_SHORT"));
    await transcribe(blob, "recording." + VF.extensionForMime(type));
  }

  /* ------------------------------------------------------------------ transcription */

  async function transcribe(blob, filename) {
    if (state.controller) return;   // block duplicate submissions
    state.controller = new AbortController();
    setStatus("processing");
    try {
      const languageValue = state.detectedLanguage && state.detectedLanguage !== "auto" ? state.detectedLanguage : "";
      const form = new FormData();
      form.append("audio", blob, filename);
      form.append("language", languageValue);
      form.append("cleanup", "false");
      const data = await api("/api/transcriptions", { method: "POST", body: form, signal: state.controller.signal });
      await applyTranscript(data);
      setStatus("idle");
    } catch (e) {
      if (e && e.name === "AbortError") {
        setStatus("idle", "Cancelled. Nothing was added.");
      } else {
        fail(e);
      }
    } finally {
      state.controller = null;
      render();
    }
  }

  async function applyTranscript(data) {
    if (data.status === "NO_SPEECH_DETECTED" || !data.originalTranscript) {
      toast("No speech was detected. Try again closer to the microphone.");
      return;
    }
    updateDetectedLanguageLabel(data.language || state.detectedLanguage || "auto");
    const raw = data.originalTranscript;
    const before = el.editor.value;
    const join = (a, b) => (a && !/\s$/.test(a) ? a + " " : a) + b;

    state.originalText = state.originalText ? state.originalText + " " + raw : raw;
    showOriginal();
    el.editor.value = join(before, raw);
    state.undoText = null;
    el.undoBtn.hidden = true;

    if (state.settings.autoCleanup) {
      try {
        const cleaned = await api("/api/transcriptions/cleanup", jsonRequest("POST", { text: raw, options: cleanupOptions() }));
        if (cleaned.cleanedText !== raw) {
          state.undoText = el.editor.value;                 // undo restores the raw transcript
          el.editor.value = join(before, cleaned.cleanedText);
          el.undoBtn.hidden = false;
        }
      } catch (_) {
        toast("Transcribed, but automatic clean up failed. You can press Clean up.");
      }
    }
    updateCounts();
    el.editor.focus();
  }

  /* ------------------------------------------------------------------ editor actions */

  async function copyText() {
    const text = el.editor.value;
    if (!text) return toast("There is nothing to copy yet.");
    try {
      await navigator.clipboard.writeText(text);
    } catch (_) {
      el.editor.select();
      if (!document.execCommand || !document.execCommand("copy")) return toast("Copy failed. Select the text and press Ctrl+C.");
    }
    toast("Copied to clipboard.");
  }

  function clearEditor() {
    if (hasUnsavedText() && !confirm("Clear the transcript? Your changes have not been saved.")) return;
    el.editor.value = "";
    state.originalText = "";
    state.savedText = "";
    state.historyId = null;
    state.undoText = null;
    el.undoBtn.hidden = true;
    showOriginal();
    updateCounts();
    el.editor.focus();
  }

  async function cleanUp() {
    const text = el.editor.value;
    if (!text.trim()) return toast("Type or record something first.");
    el.cleanBtn.disabled = true;
    try {
      const data = await api("/api/transcriptions/cleanup", jsonRequest("POST", { text, options: cleanupOptions() }));
      if (data.cleanedText === text) return toast("Nothing needed cleaning.");
      state.undoText = text;
      el.editor.value = data.cleanedText;
      el.undoBtn.hidden = false;
      updateCounts();
      toast("Cleaned up: " + data.operations.join("; ") + ".");
    } catch (e) {
      toast(e.message);
    } finally {
      el.cleanBtn.disabled = false;
    }
  }

  function undoCleanup() {
    if (state.undoText === null) return;
    el.editor.value = state.undoText;
    state.undoText = null;
    el.undoBtn.hidden = true;
    updateCounts();
  }

  async function saveTranscript() {
    const text = el.editor.value.trim();
    if (!text) return toast("There is nothing to save yet.");
    el.saveBtn.disabled = true;
    try {
      if (state.historyId) {
        await api("/api/history/" + state.historyId, jsonRequest("PUT", { editedText: text }));
      } else {
        const saved = await api("/api/history", jsonRequest("POST", {
          language: state.detectedLanguage && state.detectedLanguage !== "auto" ? state.detectedLanguage : "en",
          originalText: state.originalText.trim() || text,
          editedText: text,
        }));
        state.historyId = saved.id;
      }
      state.savedText = el.editor.value;
      toast("Saved to history.");
    } catch (e) {
      toast(e.message);
    } finally {
      el.saveBtn.disabled = false;
    }
  }

  /* ------------------------------------------------------------------ history view */

  function historyItemNode(item) {
    const li = document.createElement("li");
    li.className = "history-item";

    const meta = document.createElement("div");
    meta.className = "history-meta";
    const badge = document.createElement("span");
    badge.className = "badge";
    badge.textContent = VF.languageLabel(item.language);
    const time = document.createElement("time");
    time.dateTime = item.createdAt;
    time.textContent = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(item.createdAt));
    meta.append(badge, time);

    const text = document.createElement("p");
    text.className = "history-text";
    text.textContent = item.editedText;

    const row = document.createElement("div");
    row.className = "btn-row";
    const open = button("Open in editor", () => openInEditor(item));
    const copy = button("Copy", async () => {
      try { await navigator.clipboard.writeText(item.editedText); toast("Copied to clipboard."); }
      catch (_) { toast("Copy failed. Open it in the editor and copy from there."); }
    });
    const del = button("Delete", async () => {
      if (!confirm("Delete this saved transcription?")) return;
      try {
        await api("/api/history/" + item.id, { method: "DELETE" });
        if (state.historyId === item.id) state.historyId = null;
        li.remove();
        state.historyTotal -= 1;
        state.historyLoaded -= 1;
        refreshHistoryChrome();
        toast("Deleted.");
      } catch (e) { toast(e.message); }
    });
    del.classList.add("danger");
    row.append(open, copy, del);

    li.append(meta, text, row);
    return li;
  }

  function button(label, onClick) {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "btn small";
    b.textContent = label;
    b.addEventListener("click", onClick);
    return b;
  }

  function openInEditor(item) {
    if (hasUnsavedText() && !confirm("Replace the text in the editor? Your changes have not been saved.")) return;
    el.editor.value = item.editedText;
    state.originalText = item.originalText;
    state.savedText = item.editedText;
    state.historyId = item.id;
    state.undoText = null;
    el.undoBtn.hidden = true;
    updateDetectedLanguageLabel(item.language || "auto");
    showOriginal();
    updateCounts();
    location.hash = "#home";
  }

  function refreshHistoryChrome() {
    el.historyEmpty.hidden = state.historyTotal > 0;
    el.moreBtn.hidden = state.historyLoaded >= state.historyTotal;
    el.clearHistoryBtn.hidden = state.historyTotal === 0;
  }

  async function loadHistory(reset) {
    if (reset) {
      state.historyPage = 0; state.historyLoaded = 0; state.historyTotal = 0;
      el.historyList.textContent = "";
    }
    el.historyError.hidden = true;
    try {
      const data = await api("/api/history?page=" + state.historyPage + "&size=" + HISTORY_PAGE_SIZE);
      data.items.forEach((item) => el.historyList.append(historyItemNode(item)));
      state.historyTotal = data.totalItems;
      state.historyLoaded += data.items.length;
      state.historyPage += 1;
      refreshHistoryChrome();
    } catch (e) {
      el.historyError.textContent = e.message;
      el.historyError.hidden = false;
      el.historyEmpty.hidden = true;
      el.moreBtn.hidden = true;
      el.clearHistoryBtn.hidden = true;
    }
  }

  async function clearHistory() {
    if (!confirm("Delete ALL saved transcriptions? This cannot be undone.")) return;
    try {
      await api("/api/history", { method: "DELETE" });
      state.historyId = null;
      loadHistory(true);
      toast("History cleared.");
    } catch (e) { toast(e.message); }
  }

  /* ------------------------------------------------------------------ settings */

  function readLocalSettings() {
    try {
      const raw = localStorage.getItem(SETTINGS_KEY);
      return raw ? { ...DEFAULT_SETTINGS, ...JSON.parse(raw) } : null;
    } catch (_) { return null; }
  }

  function writeLocalSettings(s) {
    try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(s)); } catch (_) { /* private mode */ }
  }

  function applySettingsToForm() {
    const s = state.settings;
    el.setWhitespace.checked = s.fixWhitespace;
    el.setPunctuation.checked = s.fixPunctuation;
    el.setCapitalize.checked = s.capitalize;
    el.setFillers.checked = s.removeFillers;
    el.setAuto.checked = s.autoCleanup;
  }

  async function loadSettings() {
    let settings = null;
    try { settings = await api("/api/settings"); } catch (_) { /* demo mode or offline: use browser copy */ }
    state.settings = { ...DEFAULT_SETTINGS, ...(settings || readLocalSettings() || {}) };
    if (settings) writeLocalSettings(state.settings);
    applySettingsToForm();
    updateDetectedLanguageLabel("auto");
  }

  async function saveSettings() {
    state.settings = {
      defaultLanguage: state.settings.defaultLanguage || "en",
      fixWhitespace: el.setWhitespace.checked,
      fixPunctuation: el.setPunctuation.checked,
      capitalize: el.setCapitalize.checked,
      removeFillers: el.setFillers.checked,
      autoCleanup: el.setAuto.checked,
    };
    writeLocalSettings(state.settings);
    try {
      await api("/api/settings", jsonRequest("PUT", state.settings));
      el.settingsNote.textContent = "Settings saved.";
    } catch (e) {
      el.settingsNote.textContent = e.code === "HISTORY_DISABLED"
        ? "Settings saved in this browser only (the backend is in demo mode)."
        : "Saved in this browser only. " + e.message;
    }
  }

  /* ------------------------------------------------------------------ navigation + health */

  function route() {
    const view = ["home", "history", "settings"].includes(location.hash.slice(1)) ? location.hash.slice(1) : "home";
    ["home", "history", "settings"].forEach((v) => { $("view-" + v).hidden = v !== view; });
    document.querySelectorAll("[data-nav]").forEach((a) => {
      if (a.dataset.nav === view) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current");
    });
    if (view === "history") loadHistory(true);
    window.scrollTo(0, 0);
  }

  async function checkBackend() {
    try {
      const h = await api("/api/health");
      if (!h.speechProviderConfigured) {
        showBanner("The speech service is not configured yet, so transcription will fail. Set SPEECH_API_KEY on the backend (see README) and restart it.", false);
      } else if (h.historyMode === "demo") {
        showBanner("Demo mode: transcription works, but saving history is turned off.", false);
      } else if (!h.historyAvailable) {
        showBanner("The database cannot be reached, so history is unavailable. Transcription still works.", true);
      } else {
        showBanner("");
      }
    } catch (e) {
      showBanner(e.message, true);
    }
  }

  /* ------------------------------------------------------------------ wiring */

  el.micBtn.addEventListener("click", () => {
    if (state.status === "recording" || state.status === "paused") stopRecording(); else startRecording();
  });
  el.pauseBtn.addEventListener("click", togglePause);
  el.stopBtn.addEventListener("click", stopRecording);
  el.cancelBtn.addEventListener("click", () => {
    if (state.status === "processing" && state.controller) state.controller.abort(); else discardRecording();
  });
  el.fileBtn.addEventListener("click", () => el.fileInput.click());
  el.fileInput.addEventListener("change", () => {
    const file = el.fileInput.files && el.fileInput.files[0];
    el.fileInput.value = "";
    if (file) transcribe(file, file.name);
  });

  el.editor.addEventListener("input", () => {
    updateCounts();
    state.undoText = null;       // undo is only offered right after a clean up
    el.undoBtn.hidden = true;
  });
  el.copyBtn.addEventListener("click", copyText);
  el.cleanBtn.addEventListener("click", cleanUp);
  el.undoBtn.addEventListener("click", undoCleanup);
  el.clearBtn.addEventListener("click", clearEditor);
  el.saveBtn.addEventListener("click", saveTranscript);
  el.restoreBtn.addEventListener("click", () => {
    el.editor.value = state.originalText;
    state.undoText = null;
    el.undoBtn.hidden = true;
    updateCounts();
  });

  el.moreBtn.addEventListener("click", () => loadHistory(false));
  el.clearHistoryBtn.addEventListener("click", clearHistory);
  el.saveSettingsBtn.addEventListener("click", saveSettings);

  window.addEventListener("hashchange", route);
  window.addEventListener("pagehide", releaseMic);       // stop the microphone if the page is closed
  window.addEventListener("beforeunload", releaseMic);

  /* ------------------------------------------------------------------ start */
  updateDetectedLanguageLabel("auto");
  setStatus("idle");
  updateCounts();
  route();
  checkBackend();
  loadSettings();
})();

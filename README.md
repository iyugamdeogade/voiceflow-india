# VoiceFlow India

A voice-to-text web app for turning spoken phrases into editable transcripts. The frontend is plain JavaScript; the backend is Spring Boot and sends audio to an OpenAI-compatible transcription API.

## Requirements

- Java 17
- Maven 3.6 or newer
- Node.js (only needed for the frontend test command and `npx` static server)
- A speech transcription API key for real transcription
- MySQL for persistent history and settings (optional when using demo mode)

## Run locally

Open two terminals from the project folder.

### 1. Start the backend

Demo mode disables the database-backed history and settings. It still needs a valid speech API key to transcribe audio.

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:SPEECH_API_KEY = "your-api-key"
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

For persistent history, omit `SPRING_PROFILES_ACTIVE=demo`, configure MySQL using the environment variables below, and ensure the `voiceflow` database exists. The app creates its tables at startup.

### 2. Serve the frontend

```powershell
cd frontend
npx --yes http-server . -p 5500
```

Open `http://localhost:5500`. The frontend calls `http://localhost:8080` by default; change `frontend/config.js` if the backend uses a different address.

## Configuration

The backend reads these environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPEECH_API_KEY` | empty | Credential for the transcription service; required for transcription |
| `SPEECH_API_ENDPOINT` | `https://api.openai.com/v1/audio/transcriptions` | OpenAI-compatible transcription endpoint |
| `SPEECH_MODEL` | `whisper-1` | Model name accepted by the configured endpoint |
| `SPEECH_TIMEOUT` | `60s` | Provider request timeout |
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `MAX_AUDIO_MB` | `25` | Maximum audio upload size |
| `DB_URL` | local MySQL `voiceflow` database | JDBC connection URL |
| `DB_USERNAME` | `voiceflow` | MySQL username |
| `DB_PASSWORD` | empty | MySQL password |
| `CORS_ALLOWED_ORIGINS` | local frontend URLs on port 5500 | Allowed browser origins |

Keep API keys and passwords in environment variables or an ignored local `.env` file. Never commit credentials.

## Features

- Record audio from a browser microphone or upload an audio file.
- Edit, copy, clean up, and save transcript text.
- View and manage saved transcript history when using the database-backed profile.
- Animated voice and phrase UI; no language dropdown is shown.

## Current language-detection limitation

The frontend displays an “Auto-detecting” label, but the backend currently defaults requests without a selected language to English. It does not yet receive or display a reliably detected language from the provider. Do not rely on automatic language detection until that backend/provider flow is implemented. The backend's supported explicit language codes are English (`en`), Hindi (`hi`), and Marathi (`mr`).

## Tests

From the project folder:

```powershell
node --test frontend/tests/lib.test.js
```

From `backend`:

```powershell
mvn test
```

## Privacy and deployment

Audio is sent to the configured speech provider for transcription. Saved transcripts and settings are stored in the configured database. This MVP has no user authentication, so do not expose it publicly or use it for sensitive data without adding appropriate access controls and reviewing the provider's data-handling terms.
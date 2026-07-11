# Arena0077 - Native Android Client for Arena.ai

<div align="center">

![Arena0077](https://img.shields.io/badge/Arena-0077-6E56EC?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-4285F4?style=for-the-badge)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-3DDC97?style=for-the-badge)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-6E56EC?style=for-the-badge)

**A production-grade native Android client for [arena.ai](https://arena.ai/) — the official AI ranking & LLM leaderboard.**

Real API integration. Real authentication. Real streaming. No mock data.

</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Building the App](#building-the-app)
- [Simulation Mode](#simulation-mode)
- [Configuration](#configuration)
- [Testing](#testing)
- [Roadmap](#roadmap)

---

## 🎯 Overview

Arena0077 is a **native Android** client that connects directly to arena.ai's production backend. It mirrors every feature of the web app:

- 💬 **Battle Mode** — Two anonymous AI models compete side-by-side
- 🎯 **Side Mode** — Side-by-side comparison with model names visible
- 🤖 **Direct Chat** — Chat with a single specific model
- 🧠 **Agent Mode** — Autonomous multi-step task execution
- 🎨 **Image Generation** — DALL-E, Midjourney, Stable Diffusion
- 🎬 **Video Generation** — Sora, Runway, Pika
- 💻 **WebDev Mode** — Design to Code, dashboard, game, fullstack app
- 🏆 **Leaderboard** — AI model rankings across multiple categories
- 🔐 **Real Authentication** — Email/password via Supabase Auth

### Why Hybrid Architecture?

Arena.ai uses **reCAPTCHA Enterprise** (V2 + V3) to protect chat endpoints. Pure HTTP calls return `403 Forbidden`. The only reliable way to satisfy reCAPTCHA from a native app is via a WebView that runs arena.ai's own JavaScript.

**Solution:**
- **Native Kotlin/Compose UI** for navigation, sidebar, settings, leaderboard
- **Hidden WebView** as a "chat engine" that handles reCAPTCHA automatically
- **JavaScript bridge** for two-way communication between native and WebView

This is the same approach used by production apps like Instagram Lite, Twitter Lite, and many banking apps.

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Compose UI (Native)                          │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Sidebar  │  │  Chat    │  │Leaderboard│  │ Settings │       │
│  └────┬─────┘  └────┬─────┘  └──────────┘  └──────────┘       │
│       │              │                                          │
│       └──────────────┴──────► ChatViewModel ◄────┐              │
│                                  │                │              │
└──────────────────────────────────┼────────────────┘              │
                                   │                                │
┌──────────────────────────────────▼────────────────┐             │
│           ArenaRepository (singleton)              │             │
│   ┌────────────────┐    ┌──────────────────────┐  │             │
│   │ AuthManager    │    │  ChatWebViewEngine   │◄─┘             │
│   │ (Supabase JWT) │    │  (reCAPTCHA-aware)   │                │
│   └────────────────┘    └──────────┬───────────┘                │
│                                   │                              │
│   ┌───────────────────────────────▼─────────────┐               │
│   │  JsBridge (JavaScriptInterface)             │               │
│   │  - onAuthStateChanged()                     │               │
│   │  - onMessageReceived()                      │               │
│   │  - onStreamChunk()                          │               │
│   │  - onConversationCreated()                  │               │
│   │  - onHistoryLoaded()                        │               │
│   │  - onError()                                │               │
│   └─────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │   arena.ai (WebView)        │
                    │   https://arena.ai/         │
                    │                             │
                    │   - Handles reCAPTCHA       │
                    │     Enterprise V2 + V3      │
                    │   - Streams chat responses  │
                    │   - Renders markdown        │
                    │   - Loads images            │
                    └─────────────────────────────┘
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2024.12) |
| DI | Hilt 2.53 |
| Networking | OkHttp 4.12 + Retrofit 2.11 |
| Serialization | kotlinx.serialization 1.7 |
| Image Loading | Coil 2.7 |
| WebView | androidx.webkit 1.12 |
| Persistence | DataStore Preferences |
| Navigation | Navigation Compose 2.8 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## 📡 API Reference

All endpoints were extracted from arena.ai's production JavaScript bundles.

### Authentication (Supabase Auth)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/nextjs-api/sign-up` | Create anonymous user |
| POST | `/nextjs-api/sign-in/email` | Email + password login |
| POST | `/nextjs-api/sign-in/google` | Google OAuth |
| POST | `/nextjs-api/sign-out` | Logout |
| POST | `/nextjs-api/sign-up/magic-link` | Magic link sign-up |
| POST | `/nextjs-api/resend-verification` | Resend email verification |
| POST | `/nextjs-api/reset-password/request` | Request password reset |
| POST | `/nextjs-api/reset-password/confirm` | Confirm reset |
| POST | `/nextjs-api/reset-password/change` | Change password |

### User

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/me` | Current authenticated user |

### History

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/history/unified` | List recent conversations |

### Chat Streaming (reCAPTCHA-protected)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/nextjs-api/stream/create-evaluation` | Start new chat |
| POST | `/nextjs-api/stream/post-to-evaluation/{id}` | Send followup |
| POST | `/nextjs-api/stream/stop/{id}/messages/{msgId}` | Stop generation |
| POST | `/nextjs-api/stream/rerun/{id}` | Rerun a message |
| POST | `/nextjs-api/stream/resample/{id}` | Resample a response |
| POST | `/nextjs-api/stream/skip-direct-battle/{id}` | Skip direct battle |
| POST | `/nextjs-api/stream/retry-evaluation-session-message/{id}/messages/{msgId}` | Retry message |
| POST | `/nextjs-api/stream/resume-webdev/{id}` | Resume web dev |
| POST | `/nextjs-api/stream/resume-video-workflow/{id}` | Resume video |

### Modality & Voting

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/nextjs-api/auto-modality` | Auto-detect prompt modality |
| POST | `/nextjs-api/factuality/verify` | Factuality check |
| POST | `/api/vote` | Vote for better model |
| GET | `/api/evaluation/webdev/{id}/stream-credentials` | Web dev stream credentials |

### Media

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/nextjs-api/proxy/media?url={url}` | Proxy external media |

### Authentication Details

Arena.ai uses **Supabase Auth** (project: `lmarena.supabase.co`).

- Auth cookie: `arena-auth-prod-v1` (base64-encoded JSON)
- Token type: JWT Bearer
- Refresh token: opaque
- Token expiry: 3600 seconds (1 hour)

### reCAPTCHA Enterprise

- V3 (invisible) site key: `6LeTGMcsAAAAALuIlkVwIxaAuZA8VledA6d3Nnb0`
- V2 (challenge) site key: `6Le3_cYsAAAAAGwWOK2RLDgNI15Bh8C0yLBOL1yL`

Chat endpoints require either:
- `recaptchaV3Token` (preferred, invisible)
- `recaptchaV2Token` (fallback, when V3 fails)

The WebView handles this automatically. Pure HTTP calls will return `403`.

---

## 📁 Project Structure

```
arena0077/
├── app/
│   ├── build.gradle.kts              # App module config
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/arena0077/app/
│       │   │   ├── ArenaApplication.kt       # @HiltAndroidApp
│       │   │   ├── MainActivity.kt           # Single-activity host
│       │   │   ├── data/
│       │   │   │   ├── api/ArenaApi.kt       # Retrofit interface
│       │   │   │   ├── models/               # Data models
│       │   │   │   │   ├── ArenaUser.kt
│       │   │   │   │   ├── Modality.kt
│       │   │   │   │   ├── Conversation.kt
│       │   │   │   │   ├── AIModel.kt
│       │   │   │   │   └── Requests.kt
│       │   │   │   ├── auth/
│       │   │   │   │   ├── AuthManager.kt
│       │   │   │   │   └── AuthStorage.kt
│       │   │   │   ├── chat/
│       │   │   │   │   ├── ArenaRepository.kt
│       │   │   │   │   └── ChatRepository.kt
│       │   │   │   └── local/
│       │   │   │       └── SessionInterceptor.kt
│       │   │   ├── di/
│       │   │   │   ├── AppModule.kt
│       │   │   │   └── NetworkModule.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/                # Compose theme
│       │   │   │   ├── navigation/Routes.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── chat/
│       │   │   │   │   │   ├── ChatScreen.kt
│       │   │   │   │   │   └── ChatViewModel.kt
│       │   │   │   │   ├── login/LoginScreen.kt
│       │   │   │   │   ├── sidebar/SidebarScreen.kt
│       │   │   │   │   ├── leaderboard/LeaderboardScreen.kt
│       │   │   │   │   └── settings/SettingsScreen.kt
│       │   │   │   └── components/
│       │   │   └── webview/
│       │   │       ├── ArenaWebViewClient.kt # WebView config + JS injection
│       │   │       ├── ChatEvent.kt          # Event sealed class
│       │   │       ├── ChatWebViewEngine.kt  # High-level chat API
│       │   │       └── JsBridge.kt           # @JavascriptInterface
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   ├── colors.xml
│       │       │   └── themes.xml
│       │       ├── xml/
│       │       │   ├── network_security_config.xml
│       │       │   ├── file_paths.xml
│       │       │   ├── backup_rules.xml
│       │       │   └── data_extraction_rules.xml
│       │       └── drawable/, mipmap-anydpi-v26/
│       └── test/                              # Unit tests
├── gradle/
│   └── libs.versions.toml                     # Version catalog
├── simulation/                                # Node.js mock server
│   ├── server.js                              # Simulation server
│   ├── test-simulation.js                     # 17-test suite
│   └── package.json
├── docs/
│   └── API.md                                 # Detailed API reference
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🔨 Building the App

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android SDK 35
- Kotlin 2.1.0

### Build Steps

```bash
# Clone the repo
git clone https://github.com/ahmed007788/arena0077.git
cd arena0077

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Signing the Release APK

Create a `keystore.properties` file:

```properties
storeFile=/path/to/your.keystore
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Then add to `app/build.gradle.kts`:

```kotlin
val keystoreProperties = Properties().apply {
    load(File(rootProject.rootDir, "keystore.properties").inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 🧪 Simulation Mode

A Node.js simulation server that mocks arena.ai's API for offline testing.

### Running the Simulation

```bash
cd simulation
node server.js
# Listening on http://localhost:8787
```

### Running Tests

```bash
cd simulation
node test-simulation.js
```

Test coverage:

| # | Test | Status |
|---|------|--------|
| 1 | Sign-up (anonymous user creation) | ✅ |
| 2 | Sign-in with email + password | ✅ |
| 3 | Get current user (/api/me) | ✅ |
| 4 | Unauthorized access returns 401 | ✅ |
| 5 | Load conversation history | ✅ |
| 6 | Auto-modality detection | ✅ |
| 7 | Create evaluation (battle mode) | ✅ |
| 8 | Post to evaluation (followup) | ✅ |
| 9 | Direct chat mode (single model) | ✅ |
| 10 | Stop streaming | ✅ |
| 11 | Rerun message | ✅ |
| 12 | Image generation modality | ✅ |
| 13 | WebDev modality (Design to Code) | ✅ |
| 14 | Agent mode (multi-step) | ✅ |
| 15 | Vote for model | ✅ |
| 16 | Load leaderboard | ✅ |
| 17 | Sign out | ✅ |

### Pointing the App at the Simulation

For debug builds, override `ARENA_BASE_URL`:

```kotlin
// In app/build.gradle.kts, debug build type:
debug {
    buildConfigField("String", "ARENA_BASE_URL", "\"http://10.0.2.2:8787\"")
}
```

`10.0.2.2` is the Android emulator's alias for the host machine's `localhost`.

---

## ⚙️ Configuration

### BuildConfig Fields

| Field | Default | Description |
|-------|---------|-------------|
| `ARENA_BASE_URL` | `https://arena.ai` | Backend URL |
| `ARENA_SUPABASE_URL` | `https://lmarena.supabase.co` | Supabase project URL |
| `ARENA_AUTH_COOKIE_NAME` | `arena-auth-prod-v1` | Auth cookie name |
| `RECAPTCHA_V3_SITE_KEY` | `6LeTGMcsAAAAA...` | reCAPTCHA V3 site key |
| `RECAPTCHA_V2_SITE_KEY` | `6Le3_cYsAAAAA...` | reCAPTCHA V2 site key |
| `POSTHOG_TOKEN` | `phc_LG7IJbVJ...` | PostHog analytics token |

---

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Simulation Tests

```bash
cd simulation && node test-simulation.js
```

---

## 🗺 Roadmap

- [ ] File upload (images, PDFs, code files)
- [ ] Markdown rendering in chat messages
- [ ] Code syntax highlighting
- [ ] Push notifications for agent task completion
- [ ] Widget for quick chat from home screen
- [ ] Voice input
- [ ] Multi-account support
- [ ] End-to-end encryption for local cache
- [ ] Tablet / foldable layout
- [ ] Material You dynamic theming

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

---

## ⚠️ Disclaimer

This is an unofficial client. Arena.ai is a trademark of LMArena. This project is not affiliated with or endorsed by Arena.ai.

The app uses your own Arena.ai account credentials. You are responsible for complying with Arena.ai's Terms of Service.

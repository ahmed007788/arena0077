# Assets

This directory contains assets bundled with the APK.

## arena-bridge.js

The bridge script is currently embedded as a string in `ArenaWebViewClient.kt`
for simplicity. If it grows larger, move it to `arena-bridge.js` here and load
via `webView.evaluateJavascript(assets.open("arena-bridge.js").bufferedReader().use { it.readText() }, null)`.

The bridge script:
1. Hooks `window.fetch` to intercept arena.ai's chat stream endpoints.
2. Parses SSE events and re-emits them via `AndroidBridge.onStreamChunk()` etc.
3. Polls auth state every 5 seconds and emits `AuthStateChanged`.
4. Exposes `window.__arenaBridge` with command methods (`sendMessage`, `stop`, `vote`, etc.).

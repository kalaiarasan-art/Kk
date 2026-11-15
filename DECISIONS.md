# Doodle Dash — Architectural Decisions (WHY, Not What)

## 1. Native Android + Firebase Firestore Adaptation vs. External Web Socket Server
- **Why**: In the Android build environment, provisioning **Firebase Firestore (Enterprise Edition)** and **Firebase Auth with Google Sign-In via Jetpack Credential Manager** provides a zero-cost ($0/month) real-time synchronization layer directly inside the APK without requiring the owner to maintain or pay for an external Node.js host.
- **Bot Hybrid Engine**: Because players may test on a single phone or emulator before inviting friends, rooms include deterministic AI Bot players that draw multi-stroke vector illustrations and submit timed guesses so a 4-to-8-player match is immediately playable solo or with friends.

## 2. Append-Only Vector Op-Log with 20-Op Bitmap Checkpoints
- **Why**: Sending pixel bitmaps over the network would exhaust the free-tier egress budget in a handful of games. Encoding strokes as Ramer–Douglas–Peucker simplified (`ε ≈ 0.6 px`), 0.25 px quantized delta strings keeps each stroke under a few hundred bytes while ensuring crisp rendering on every screen density.
- **Why 20-Op Checkpoints**: Replaying 500 vector operations on every frame during an undo would drop frame rates below 60 fps. Caching an in-memory `Bitmap` checkpoint every 20 operations bounds undo/redo replay to at most 19 operations.

## 3. Deterministic Scanline Flood Fill & Mulberry32 Seeded PRNG
- **Why**: Standard flood fills on anti-aliased canvases vary across GPU rasterizers. Running the scanline flood fill with tolerance + 1px edge expansion on the drawer's client and broadcasting compact run-length spans (`y,x1,x2`), combined with `mulberry32` seeded by `strokeId` for pencil grain and airbrush spray, guarantees every client sees the exact same pixels.

## 4. Zero-Get Subcollection Security Rules (`memberUids` Denormalization)
- **Why**: Calling `get()` inside `allow list` rules causes O(N) document read charges and rule evaluation latency. By denormalizing bounded `memberUids` (≤ 12 players) onto `/rooms/{roomId}/ops/{opId}` and `/rooms/{roomId}/messages/{messageId}`, security rules enforce strict member isolation in O(1) time with zero extra database reads.

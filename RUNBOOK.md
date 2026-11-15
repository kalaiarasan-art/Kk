# Doodle Dash — Operations & Quota Runbook (`RUNBOOK.md`)

## 1. Free-Tier Quota Limits & Safeguards
- **Firestore Daily Read/Write & Egress Budget**:
  - The in-app `EgressGovernor` tracks payload bytes across every room session against the 5 GB monthly budget.
  - **At 70% budget**: Stroke batching automatically slows from `100ms` to `200ms` and floating live reaction bursts pause to conserve bandwidth.
  - **At 95% budget**: New cloud room creation pauses with a friendly banner while local/bot play continues uninterrupted.
- **What to do if `"Quota exceeded"` appears**:
  - Daily free-tier quotas reset automatically at midnight Pacific Time.
  - You can inspect live usage in the Firebase Console under **Firestore -> Usage**.

## 2. Google Sign-In (`CredentialManager`) Troubleshooting
- If Google Sign-In displays a cancellation message (`GetCredentialCancellationException`):
  1. Ensure the device/emulator has an active Google account signed into Google Play Services.
  2. Verify that the debug keystore SHA-1 and SHA-256 fingerprints match the `certificate_hash` in `app/google-services.json`.

## 3. Local Security Rules Testing
- Start warm emulators once:
  ```bash
  firebase emulators:start --only firestore,auth --project demo-no-project
  ```
- Run Tier 1 JavaScript rules validation (~1s):
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- Run Tier 2 Android Robolectric repository rule tests:
  ```bash
  gradle :app:testDebugUnitTest --tests '*Rule*'
  ```

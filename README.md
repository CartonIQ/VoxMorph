# VoxMorph – Ultimate Voice Changer (v1.1.0)
Package `com.cartoniq.voxmorph` · minSdk 24 · target/compile 34 · Gradle 8.9 · AGP 8.7.3 · Kotlin 2.0.21 · JVM 17
By CartoniQ – Premium offline apps. Built with passion.

Offline-first Android voice effects studio (Jetpack Compose, Material 3).
Flow: **record → stop → offline voice engine → preview → (save to History) → download / share.** Not live voice changing.

## Build status (read this first)
This source was written without Gradle, a Kotlin compiler or network access, so **it has never been compiled or run on a device.**
Expect to fix a few compile errors on the first build, and send the error text to get them fixed. The voice-engine algorithm
(pitch accuracy, formant shift, duration) was checked with a separate numeric prototype, but the Kotlin itself was not compiled.

## Build on Termux (phone only)
```
sh setup.sh            # one time: Gradle 8.9 wrapper, local.properties, aapt2 override for ARM
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

Why `setup.sh`: AGP 8.7.3 needs Gradle 8.9 (a newer global Gradle will not work), the ZIP cannot ship `gradle-wrapper.jar`,
and the aapt2 that AGP downloads is built for x86 and cannot run on ARM Termux, so the script points AGP at Termux's own `aapt2`.
It needs the global `gradle` once (to create the wrapper) and internet for the first build.

## What is new in 1.1
- **Download**: Studio preview, Text-to-Speech preview and every History item have a Download button. Android 10+ saves in one tap to
  `Downloads/VoxMorph`; Android 7-9 opens the system "save as" screen. No storage permission is needed.
- **New voice engine** (`audio/Psola.kt`, `audio/VoiceProcessor.kt`): TD-PSOLA pitch shifting with separate formant (voice tone)
  control, so male/female voices change pitch AND vocal-tract colour instead of sounding like sped-up tape. Robot voice is locked to a
  single note. Added: 60 Hz rumble filter, gentle compressor, cubic-interpolation resampling.
- **Voice tone** slider (Studio adjust dialog and TTS advanced options) and **Apply selected voice to this recording** (try another
  voice without re-recording; the original stays in memory only while the app is open and is never saved).
- **About CartoniQ** page (Settings → About) with developer and contact info (English + Hindi).
- **Full Privacy Policy** page (Settings → Privacy) rewritten to match exactly what the app does (English + Hindi).
- Fix: tapping Stop right after Record could be lost; the recording then ran for up to 60 s.
- Settings "Clear history" now asks for confirmation.

## Features
- 30 profiles from one catalog (`domain/VoiceProfile.kt`): Man 1-10, Woman 1-10, 10 effects. Free: Man 1, Woman 1, Robot.
- Rewarded unlock lasts 10 minutes, granted only in `onUserEarnedReward`, expiry stored as epoch time.
- Text-to-Speech: `synthesizeToFile` → decode → voice engine → preview/save/download/share. Speaker output is never recorded.
- History (Room + files in `filesDir/recordings`), filters, play/pause, download, share, delete, clear all, startup reconcile.
- English + Hindi, in-app language switch, semantics/content descriptions, 48dp targets, adaptive + legacy icons.
- Permissions: only `RECORD_AUDIO`, `INTERNET`, `ACCESS_NETWORK_STATE`.

## Architecture
`domain` (catalog, errors) · `data` (Room, repository, reward store) · `audio` (AudioRecord engine, PSOLA voice engine, TTS, decoder, player) ·
`ads` (UMP + Mobile Ads) · `viewmodel` (single state holder) · `ui` (Compose screens/components) · `util` (share, download, links, locale).

## Known limits / honest notes
- PSOLA works best on a single clean voice. Strong background noise or music lowers pitch-tracking quality; unvoiced parts keep
  natural timing. Gender voices are strong presets, not a guaranteed realistic gender conversion.
- No interstitial ads, no foreground recording service (recording stops when the app is backgrounded).
- Ads use Google TEST IDs (`ads/AdManager.kt`, manifest). Replace before release. Host the privacy policy at a public URL for Play.
- Release build is unsigned and un-minified. Max recording 60 s, min 0.5 s. TTS limit 500 characters (`MainViewModel.TTS_LIMIT`).

# 🎙️ VoxMorph - Ultimate Voice Changer & TTS

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=for-the-badge&logo=kotlin)
![Android](https://img.shields.io/badge/Android-API%2024%2B-green.svg?style=for-the-badge&logo=android)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-orange.svg?style=for-the-badge&logo=jetpack-compose)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)
![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg?style=for-the-badge)

**VoxMorph** is a premium, 100% offline voice changer and Text-to-Speech (TTS) application for Android. Built for content creators, pranksters, and anyone who wants to have fun, VoxMorph features a stunning "Cosmic Purple" Neon Studio UI and a professional-grade audio engine.

Transform your voice into 30 different characters, from realistic men and women to robots and aliens. Perfect for WhatsApp voice notes, Instagram Reels, YouTube videos, and practical jokes.

---

## 🚀 Key Features

### 🎙️ 30 Unique Voice Profiles
VoxMorph includes 30 meticulously crafted voice profiles:
*   **10 Male Voices:** From a natural news anchor to a deep cinematic movie trailer voice.
*   **10 Female Voices:** From a soft, friendly tone to an energetic pop star.
*   **10 Fun Effects:** Robot, Alien, Ghost, Echo, Megaphone, Chipmunk, Monster, Deep Voice, Radio, and Telephone.

### 🎛️ Studio-Quality PSOLA Engine
Unlike basic voice changers that make you sound like a broken robot, VoxMorph uses a custom **PSOLA (Pitch Synchronous Overlap and Add)** algorithm. This shifts the pitch *while preserving the formant* (the natural resonance of your throat), resulting in voices that sound incredibly human and natural.

### 🗣️ Bilingual Text-to-Speech (TTS)
Type any text, and VoxMorph will speak it out loud in your chosen voice! 
*   Full support for **English** and **Hindi** (हिंदी).
*   Adjustable Pitch and Speed sliders.
*   Perfect for creating AI voiceovers for videos.

### 📥 Share & Download
*   **Download:** Save your creations directly to your phone's Downloads folder in `.m4a` (AAC) format.
*   **Share:** One-tap sharing directly to WhatsApp, Instagram, or Telegram. (No more "Unsupported Format" errors).

### 🕒 History & Room DB
All your generated voice notes and TTS audio are saved locally in a Room Database, so you can listen to them, share them, or delete them anytime.

---

## 📖 How to Use VoxMorph

### Method 1: Voice Recording
1. Open the **Studio** tab.
2. Select a voice from the grid (Male, Female, or Effects).
3. Tap the massive **RECORD** button and speak.
4. Tap **STOP**. The PSOLA engine will instantly process your voice.
5. Play the preview, then **Download** or **Share** it.

### Method 2: Text-to-Speech (TTS)
1. Tap the **"Text to Speech"** button in the top right corner.
2. Type or paste your text (English or Hindi).
3. Choose your **Language** (English/Hindi) and **Voice Profile**.
4. Tap **"Generate Speech"**.
5. Your audio file is ready to share!

### 🔓 Unlocking Premium Voices
Some voices are locked behind a crown icon. To unlock them:
1. Tap on a locked voice.
2. Watch a short **Rewarded Video Ad**.
3. All premium voices are unlocked for **10 minutes**!

---

## 🛠️ Tech Stack & Architecture

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM + Clean Architecture
*   **Audio Engine:** Custom Kotlin DSP (`AudioRecord` + `AudioTrack`), PSOLA for Pitch/Formant shifting.
*   **Text-to-Speech:** Android Native `TextToSpeech` (Synthesized to `.m4a` and processed offline).
*   **Local Database:** Room DB (for History).
*   **Monetization:** Google AdMob (Banner, Interstitial, Rewarded) & UMP for EU Consent.
*   **Min SDK:** 24 (Android 7.0) | **Target SDK:** 34 (Android 14)

---

## 🔐 Privacy Policy

VoxMorph is built with privacy as a top priority:
1.  **100% Offline:** The voice changing and TTS engines run entirely on your device using local math. No audio is ever sent to a server.
2.  **No Accounts:** There is no sign-up, no tracking, and no analytics.
3.  **Local Storage:** All recordings are saved locally on your device.
4.  **Ads:** We use Google AdMob to display ads, which may collect your device's advertising ID to serve personalized ads.

---

## 👨‍💻 Developer

**Mohd Arsh (CartoniQ)**
*   GitHub: [@CartonIQ](https://github.com/CartonIQ)
*   Android App Developer | Kotlin | Java | Termux 🚀

## ⚖️ License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

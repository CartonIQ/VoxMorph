#!/data/data/com.termux/files/usr/bin/sh
# VoxMorph one-time setup for Termux. Run:  sh setup.sh        (add --build to also build the APK)
# - creates the Gradle 8.9 wrapper (this project needs AGP 8.7.3 -> Gradle 8.9; a newer global Gradle will not work)
# - writes local.properties (Android SDK path)
# - points AGP at Termux's own aapt2 (the aapt2 AGP downloads is for x86 and cannot run on ARM)
set -e
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/qrapp/android-sdk}}"
if [ ! -d "$SDK" ]; then echo "Android SDK not found at $SDK. Set ANDROID_HOME and run again."; exit 1; fi
if [ ! -f local.properties ]; then echo "sdk.dir=$SDK" > local.properties; echo "wrote local.properties"; fi

if [ ! -f gradle/wrapper/gradle-wrapper.jar ] || [ ! -f gradlew ]; then
  command -v gradle >/dev/null 2>&1 || { echo "Global gradle not found (needed once to create the wrapper)."; exit 1; }
  TMP="$(mktemp -d)"
  echo 'rootProject.name = "wrapper-temp"' > "$TMP/settings.gradle"
  ( cd "$TMP" && gradle wrapper --gradle-version 8.9 --distribution-type bin )
  cp "$TMP/gradlew" "$TMP/gradlew.bat" .
  mkdir -p gradle/wrapper
  cp "$TMP/gradle/wrapper/gradle-wrapper.jar" gradle/wrapper/
  rm -rf "$TMP"
  chmod +x gradlew
  echo "created Gradle 8.9 wrapper"
fi

AAPT2="$(command -v aapt2 || true)"
if [ -n "$AAPT2" ] && ! grep -qs "aapt2FromMavenOverride" gradle.properties "$HOME/.gradle/gradle.properties"; then
  echo "android.aapt2FromMavenOverride=$AAPT2" >> gradle.properties
  echo "added aapt2 override: $AAPT2"
fi

echo "Setup done. Build with:  ./gradlew assembleDebug"
if [ "$1" = "--build" ]; then ./gradlew assembleDebug; fi

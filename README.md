# SwadayaTV

SwadayaTV is a clean, modern and lightweight Android TV Launcher. It replaces the stock home screen with a calm, fast interface focused on your apps and the essentials.

## Features

- Fast, minimal home screen for Android TV and Google TV (Android 8.0 or newer)
- App dock with focus glow and reorder support
- Live system card, Now Playing and running-apps screen
- Phone remote over the local network
- Live globe screen saver
- Prayer times, weather and air quality widgets (planned, step by step)

## Installation

### Build from source

```bash
git clone https://github.com/frhnkrnwn/SwadayaTV.git
cd SwadayaTV/launcher
./gradlew assembleDebug
```

The debug APK will be at `launcher/app/build/outputs/apk/debug/`.

### CI

Every push to `main` builds the debug APK via GitHub Actions (`.github/workflows/build.yml`).

## Development

- Package: `com.swadaya.tv`
- minSdk 26, targetSdk 36, compileSdk 36
- Primary language: Kotlin/Java (Android)

## License

See [LICENSE](LICENSE).

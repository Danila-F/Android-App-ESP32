# Android-App-ESP32

Native Android app for discovering, provisioning and controlling ESP32 devices running the companion `ESP32-WiFi-HTTP-API` firmware.

## Features

- Kotlin + Jetpack Compose UI.
- English UI strings in Android resources for future localization.
- ESP32 discovery through Android NSD / mDNS (`_espctrl._tcp`).
- Manual device adding by URL or IP address.
- Persistent local device storage.
- HTTP JSON API integration:
  - `GET /api/info`
  - `GET /api/state`
  - `POST /api/command`
  - `POST /api/ota`
- Android Device Controls provider through `ControlsProviderService`.
- Firmware upload from the phone to ESP32 over Wi-Fi.
- GitHub Actions debug APK build artifact.

## Requirements

- Android 8.0+ for the app itself.
- Android 11+ for Android Device Controls integration.
- ESP32 device running the companion HTTP API firmware.

## Build locally

```bash
gradle :app:assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions artifact

Every push and pull request runs `.github/workflows/android.yml` and uploads the debug APK as a downloadable artifact named `android-app-debug-apk`.

## Basic app flow

1. Flash and provision the ESP32 firmware.
2. Connect the phone to the same Wi-Fi network as the ESP32.
3. Tap **Scan for devices** or add the ESP32 manually by IP/URL.
4. Save the device token returned by the ESP32 provisioning flow.
5. Use **Refresh**, **Turn on**, **Turn off**, **Toggle** or **Upload firmware**.
6. On Android 11+, add the saved device to the system **Device Controls** UI from Android settings / quick settings.

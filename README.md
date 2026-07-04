# Android-App-ESP32

Native Android app for discovering, provisioning and controlling ESP32 devices running the companion `ESP32-WiFi-HTTP-API` firmware.

## Features

- Kotlin + Jetpack Compose UI.
- English UI strings in Android resources for future localization.
- First-time ESP32 provisioning from inside the app.
- Android 10+ setup Wi-Fi connection prompt through `WifiNetworkSpecifier`.
- ESP32 discovery through Android NSD / mDNS (`_espctrl._tcp`).
- Manual device adding by URL or IP address.
- Persistent local device storage.
- HTTP JSON API integration:
  - `GET /api/info`
  - `GET /api/state`
  - `POST /api/provision`
  - `POST /api/command`
  - `POST /api/ota`
- Android Device Controls provider through `ControlsProviderService`.
- Firmware upload from the phone to ESP32 over Wi-Fi.
- GitHub Actions debug APK build artifact.

## Requirements

- Android 8.0+ for the app itself.
- Android 10+ for automatic setup Wi-Fi connection prompt.
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

## First-time ESP32 setup flow

1. Flash the ESP32 firmware.
2. Power on ESP32. It should broadcast a setup Wi-Fi network like:

```text
ESP32-Setup-xxxxxx
```

Default password:

```text
esp32setup
```

3. Open the Android app.
4. In **First-time ESP32 setup**, enter:
   - ESP32 setup Wi-Fi SSID;
   - ESP32 setup Wi-Fi password;
   - setup API URL, usually `http://192.168.4.1`;
   - home Wi-Fi SSID and password;
   - device name and room.
5. Tap **Connect to setup Wi-Fi**. Android will show a system Wi-Fi connection prompt.
6. After the setup Wi-Fi connection is available, tap **Provision ESP32 and save token**.
7. The app sends `POST /api/provision` automatically and copies the returned device token into the manual add form.
8. ESP32 reboots and joins the home Wi-Fi.
9. Reconnect the phone to the home Wi-Fi if Android does not do it automatically.
10. Tap **Scan for devices**, select the discovered ESP32, then tap **Add device**.

Android does not allow silent Wi-Fi switching without user confirmation. The app therefore can request the connection, but the user still needs to approve Android's system Wi-Fi prompt.

## Basic app flow after setup

1. Connect the phone to the same Wi-Fi network as the ESP32.
2. Tap **Scan for devices** or add the ESP32 manually by IP/URL.
3. Use **Refresh**, **Turn on**, **Turn off**, **Toggle** or **Upload firmware**.
4. On Android 11+, add the saved device to the system **Device Controls** UI from Android settings / quick settings.

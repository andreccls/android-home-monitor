# android-home-monitor (English summary)

> **A sample / reference code project — NOT a production-ready product.** **Nothing here talks to real cameras, Alexa devices, gates or intercoms:**
> every device is simulated behind interfaces. The camera "stream" is a frame drawn by the app. No authentication, fictional data.

A home-monitoring Android app in **Kotlin + Jetpack Compose (Material 3)** following the **Google "Guide to app architecture"**: UI / domain / data layers,
unidirectional data flow (`ViewModel` + `StateFlow`), **Room as the single source of truth** (offline-first), Hilt, Coroutines/Flow, Navigation Compose.
Full documentation is in Portuguese ([README.md](README.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/adr](docs/adr)).

## What you get

- CRUD for **cameras and Alexas** (name, room, type, address/serial, online/offline status) with domain-level validation.
- **Gates**: state machine (closed/opening/open/closing/offline), open/close always behind a confirmation dialog.
- **Intercom**: ringing/answered/missed calls, history, "answer" and "open gate" from the call.
- **Dashboard** with device overview and **alerts** (gate left open too long, device offline, missed call); a **simulated camera view**.
- **Four ports** (`GateDriver`, `IntercomDriver`, `DeviceProbe`, `CameraStream`) with simulators that have latency, random failures and autonomous activity.
  Swapping in real hardware means implementing those interfaces (RTSP/ONVIF, Alexa Smart Home API, MQTT/SIP... listed as next steps, not implemented).
- A **Konsist architecture test** that fails the build if `domain` imports Android, `data` imports `ui`, or `ui` imports `data`.

## Run it

```bash
make build         # debug APK        make test          # 66 JVM unit tests
make lint          # ktlint + Lint    make coverage      # Kover, 80 % gate
make instrumented  # 7 Compose UI tests on an emulator    make run   # install + launch
```

Needs JDK 17+ and the Android SDK (platform 37): set `ANDROID_HOME` or create an untracked `local.properties` with `sdk.dir=...`.

## Measured (2026-10-06)

AGP 9.4.1, Gradle 9.8.0, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, Hilt 2.60.1, built with JDK 25.
**66 unit + 7 instrumented tests green**; line coverage of domain/data/view models **100 %** (544/544, gate 80 %; Compose screens, Hilt/Room generated code and DI
modules are excluded and listed in [docs/TESTING.md](docs/TESTING.md)); ktlint and Android Lint clean with warnings as errors; ran on an API 37 emulator with no crashes.
Instrumented tests are not in CI (they need an emulator). Not exercised: TalkBack on a device, tablet layouts, signed release builds.

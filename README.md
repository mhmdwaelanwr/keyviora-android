# KEYVIORA

**One Touch. Full Control.**

KEYVIORA is a lightweight offline Android accessibility utility with a movable floating control panel, built with Kotlin and designed for people whose physical power/volume keys are damaged. It is currently a **pre-release candidate; not yet tested on a physical HONOR phone**.

[Editable Figma design](https://www.figma.com/design/eg7BsPgq2CYyORfeHb3BuG) · Android Studio source · No analytics or internet permission.

## Features
- Floating overlay, movable by dragging; adjustable appearance, size and opacity, with safe screen bounds and a reset button.
- Media volume up/down and interactive volume slider.
- Lock screen; system power dialog with an extra safety warning.
- Screenshot, flashlight with camera-permission recovery and torch-state tracking, Home, Back, Recents, notifications and Quick Settings.
- Tap, double tap and long press configurable independently.
- Second overlay page for extra quick shortcuts.
- Settings stored locally, no account required.

## Build on GitHub Actions
1. Open **Actions → KEYVIORA — Android CI** and select the latest run or **Run workflow**.
2. A green run means CI completed JVM unit tests, `assembleDebug`, `lintDebug` **and an Android 15 emulator app-launch smoke test**; download the **KEYVIORA-debug-APK** artifact from the run.
3. Extract ZIP to retrieve `app-debug.apk`, then install it on Android; enable **KEYVIORA Controls** under Accessibility services.
4. If CI fails, open the job log. The APK should not be treated as validated until the build succeeds.

*The pipeline does not prove hardware behavior.* See [QA_CHECKLIST.md](QA_CHECKLIST.md) for on-device tests.

## Clone in Android Studio
1. **File → New → Project from Version Control** (or **Get from VCS**).
2. Use `https://github.com/mhmdwaelanwr/keyviora-android.git`.
3. Let Android Studio import the Gradle Kotlin project (Android Gradle Plugin **8.7.3**, Gradle **8.9**, JDK **17**, Android SDK Platform **35**).
4. If Android Studio can't find the Gradle wrapper, use an installed Gradle **8.9** once to run `gradle wrapper --gradle-version 8.9`; this package includes `gradle-wrapper.properties`, but no wrapper JAR. Then use the generated `gradlew`.
5. Run `:app:assembleDebug`, then test on the phone.

**Local terminal (after Gradle 8.9 is installed):**

```bash
git clone https://github.com/mhmdwaelanwr/keyviora-android.git
cd keyviora-android
gradle :app:assembleDebug
```

## Safety and limitations
- **Do not fully power off your phone as a test.** Android cannot boot a fully powered-off device without working hardware/alternate OEM method. The power shortcut opens a warning before Android's system power dialog.
- The floating button cannot wake the screen by itself. Enable HONOR **double tap to wake** or **raise to wake** if available.
- Android security restrictions mean Wi-Fi/Bluetooth/data toggles may open system Settings rather than toggle directly.
- Some OEM screens restrict screenshots. Flashlight access requires CAMERA permission; the app offers a permission prompt when first needed.
- Accessibility controls use global actions; this service does **not** read window contents.
- The app is not an official HONOR product and is not affiliated with HONOR.

## Architecture
- `app/src/main/java/com/mhmdwaelanwr/keyviora/MainActivity.kt` — main UI.
- `TouchAccessibilityService.kt` — floating controls and Android actions.
- `TouchPrefs.kt`, `TouchStyle.kt` — preferences and UI tokens.
- `BubbleBounds.kt` and `BubbleBoundsTest.kt` — overlay bounds logic and regression tests.
- `.github/workflows/android-build.yml` — CI: debug APK and lint report.

See [Brand Design System](design/BRAND.md), [CI/static QA status](QA_REPORT.md) and [HONOR 50 test checklist](QA_CHECKLIST.md).

## License
Copyright © 2026 Mohamed Anwar. **All rights reserved at this stage.** No open-source license has been granted yet. This is intentional until the publishing/distribution decision is made.

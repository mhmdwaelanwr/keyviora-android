# Quality and build status — KEYVIORA 0.4.0-beta

## Verified baseline (main, 2026-11-12)

The previous `main` branch completed GitHub Actions run
[#8](https://github.com/mhmdwaelanwr/keyviora-android/actions/runs/43290522949)
successfully, including Android debug compilation, Android lint, and an Android 15
emulator launch smoke test. This does **not** validate hardware-specific behavior.

## New 0.4.0-beta improvements

- Clamp the floating control to the screen after drag, resize and configuration changes.
- Add a one-tap reset/recovery button under Style, plus a Home restore shortcut.
- Track flashlight changes from Android torch callbacks, and prompt for missing camera permission.
- Correct light-accent button contrast and mirror configured size/opacity in the Style preview.
- Update media volume percentage on slider changes.
- Add `BubbleBoundsTest` unit tests for normal, negative, overflow and tiny-screen coordinates.
- Gate downloadable CI APK on unit tests, compilation and lint.

## QA gates

| Test | Status |
|---|---|
| Prior `main` Android build/lint/emulator launch | PASS (run #8, 2026-11-12) |
| 0.4.0-beta JVM unit tests | PENDING — requires new CI |
| 0.4.0-beta Android build & lint | PENDING — requires new CI |
| 0.4.0-beta emulator launch smoke test | PENDING — requires new CI |
| Physical HONOR device validation | NOT RUN |
| Screen rotation, low-memory restore and multi-window on HONOR | NOT RUN |
| Power menu and flashlight on physical HONOR | NOT RUN |

The new branch has **not** been verified by a completed run at the time of this document update.
Do not mistake a successful emulator launch for working accessibility controls on your phone.

**Safety:** An Android application cannot power on a fully shut-down device.
Avoid shutting down devices with a broken hardware power button.

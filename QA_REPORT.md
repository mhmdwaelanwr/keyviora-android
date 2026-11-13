# KEYVIORA 0.5.0-beta — UX & quality report

## Previous verified build
The previous `main` version passed Android debug compile, lint and Android 15 emulator launch:
[GitHub Actions run #8](https://github.com/mhmdwaelanwr/keyviora-android/actions/runs/43290522949).

## 0.5.0-beta changes under review

**Core reliability (0.4 foundation):**
- Bubble position clamped after drag, resizing and orientation changes; position reset is available in Style.
- Camera permission recovery and flashlight torch-state callback handling.
- More readable colors and a current-value volume readout.
- JVM regression tests for bubble bounds, and CI gating of test/compile/lint.

**Usability (0.5):**
- Clear activation call to action and status-driven first-run explanation.
- Button visibility status accurately shown in the dashboard; obvious restore affordance.
- Gesture configuration description, safe warning before binding Hide, and reset to recommended gestures.
- Settings show distinct *required* (Accessibility) and *optional* (flashlight camera) permissions.
- Floating menu scrolls instead of clipping on compact or landscape displays.
- 48dp minimum floating button and larger close/more touch controls, plus screen reader labels.
- CI captures an emulator screenshot of the Home screen for visual review.

## Validation matrix
| Gate | Status |
|---|---|
| Prior `main` compilation + lint + Android 15 launch | PASS, run #8 |
| 0.5.0-beta unit tests, build and lint | PENDING — check latest PR workflow |
| 0.5.0-beta emulator launch and screenshot | PENDING — check latest PR workflow |
| HONOR real-device functionality | NOT TESTED |
| Compact screen / system large-font / landscape UX | NOT TESTED ON DEVICE |
| Service persistence with HONOR battery management | NOT TESTED ON DEVICE |

**CI isn't a guarantee that a real accessibility overlay works on an OEM device.**
The floating control can't wake a switched-off device and can't restart a fully powered-off phone.

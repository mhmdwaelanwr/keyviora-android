# KEYVIORA 0.7.0-beta — UX & quality report

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

## 0.6.0-beta extension

- Optional snap-to-edge preference (defaults ON); only applies after an actual drag.
- Preserve settings scroll position when repainting the Activity.
- Home volume buttons work without accessibility permissions; global actions still require them.
- `NONE` gesture setting, with defensive fallback for invalid saved action values.
- Confirmation before restoring all application defaults; Android permissions are untouched.
- Edge-snap pure Kotlin unit tests including a screen smaller than the bubble.

**Verification:** The above changes are code changes, not physical-device results. Inspect CI for the exact 0.6.0-beta commit before installing; HONOR/MagicOS background behavior and gesture handling require testing on the target phone.

## 0.7.0-beta: improvements to validate

- Three customizable pinned controls on the More Tools overlay page, with correct metadata shared across UI and service.
- Dashboard and Setup show the actual overlay state and allow recovery if it failed to appear.
- Accessibility click route for TalkBack; regular gestures still require on-device testing.
- Vertical safe-zone clamping prevents the orb sitting over navigation/status bars.
- A NONE gesture does not dismiss the open panel.
- New action catalog, safe-insets and pinned-default unit tests.

**At documentation time, these commits are not certified by a completed CI run or a HONOR-device test.** Do not treat an older green run as proof this version works.

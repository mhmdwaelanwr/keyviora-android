# KEYVIORA 0.8.0-beta — HONOR / Android acceptance checklist

## Onboarding & recovery
- [ ] On first install, Home clearly offers **Enable accessibility**, and tapping it opens system settings.
- [ ] Returning to Home shows service running and floating button visible, without stale OFF status.
- [ ] Hiding the button changes the UI to HIDDEN; Show button restores it in one tap.
- [ ] The button's Reset position action places it at a reachable screen edge.
- [ ] Settings distinguish required Accessibility permission from optional flashlight permission.
- [ ] Camera denial/re-approval does not crash; the user sees useful feedback.

## Usability & accessibility
- [ ] Brand and status fit a narrow HONOR screen without being cut off.
- [ ] Floating menu stays scrollable in landscape and with system large text.
- [ ] Close/menu navigation and action tiles have comfortably sized touch targets.
- [ ] TalkBack announces navigation tab selection, color names and key actions.
- [ ] Preset gestures are explained, custom gestures persist and Reset restores defaults.
- [ ] Assigning Hide warns how to recover the button.
- [ ] Style preview reflects size/opacity/accent; button remains onscreen after resizing.
- [ ] Dragging remains smooth; position persists after screen rotation and service reconnect.

## New 0.6 ergonomics
- [ ] Turn Snap to edge on; drag near the left and right sides and confirm it snaps within reachable bounds.
- [ ] Turn Snap to edge off; confirm free placement persists after releasing the bubble.
- [ ] Change Style options while scrolled halfway down; the app must not jump to the top.
- [ ] Before enabling Accessibility, the Home volume shortcuts change media volume.
- [ ] Assign No action to a gesture; tapping/holding that gesture must do nothing.
- [ ] Reset all settings prompts for confirmation and restores a hidden floating bubble.
- [ ] On Android 15, Home content and bottom navigation do not overlap status or gesture navigation bars.

## 0.7 pinned shortcuts and recovery
- [ ] In Gestures, edit each of three pinned tools and verify the new icons and labels appear on the first row in More tools.
- [ ] Reset pinned tools to defaults; verify Wi-Fi, Notices and App return.
- [ ] If floating window creation fails, Home must not claim the bubble is ACTIVE, and restore must retry safely.
- [ ] Turn on TalkBack, navigate to the bubble and activate it using the accessibility click gesture.
- [ ] Confirm the bubble stays clear of top status bar and bottom gesture navigation zone when dragged or rotated.
- [ ] Assign None to a gesture and invoke it with the menu open; menu must stay open.
- [ ] Ensure device screen reader announcements and menu control labels remain understandable.

## 0.8 quick-grid and touch feedback
- [ ] Customize each of six primary quick-control tiles from Gestures; verify every selection appears in the floating menu.
- [ ] Assign NONE to a quick tile; pressing it must not trigger a system action.
- [ ] Restore the six recommended buttons and confirm independently pinned More Tools shortcuts remain as configured.
- [ ] Turn touch feedback off; gestures and overlay buttons must stop invoking haptic feedback.
- [ ] Turn touch feedback on; verify the device's system vibration preference is still honored.
- [ ] On compact and landscape displays, all six actions remain reachable by scrolling.
- [ ] Run the exact GitHub 0.8 commit's unit tests, debug APK build, lint and emulator launch.

## Function & safety
- [ ] Lock screen works; HONOR double-tap-to-wake (if supported) wakes it again.
- [ ] Volume buttons and sliders affect media sound; displayed percentages update.
- [ ] Screenshot works only where Android allows it.
- [ ] Flashlight toggles correctly even when Android's torch was toggled externally.
- [ ] Power first shows a warning and Cancel never opens the Android power menu.
- [ ] Home, Back, Recents, Notifications, Quick Settings and Wi-Fi/Bluetooth shortcuts work.
- [ ] No Internet permission, no analytics, no collection of app-window content.
- [ ] Long-term service stability with HONOR battery optimization restrictions reviewed.

**Never test a full shutdown on a device with a broken physical power button.**

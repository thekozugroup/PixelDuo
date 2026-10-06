# Pixel Duo 1.0.0 verification

Built and installed on October 6, 2026.

- Signed optimized release: `PixelDuo-1.0.0.apk`, 1,752,958 bytes.
- SHA-256: `27eab4bbf0926d72771e21f69e13cd7f98f13fa1f2b407cd9c77284ec197030c`.
- APK Signature Scheme v2 verified. Package `org.pixelduo`, version 1.0.0, minimum API 33, target API 36.
- Clean Gradle build: `testReleaseUnitTest lintRelease assembleRelease`, successful. One unit regression check, zero failures/errors. Lint: zero errors, 13 warnings for dependency versions and code/style suggestions.
- Pixel Fold API 36 emulator: public hinge events create the real glass shader accessibility overlay. Exactly one switch. Input passes through the overlay. Toggle Off unregisters the sensor and removes the overlay. A settled hinge releases the overlay and image. Screen Off unregisters the sensor. Wake resumes it. Disabling/enabling system animations and Battery Saver correctly pauses/resumes it.
- UI inspected on the cover display (1080 × 2092), inner display (2208 × 1840), rotated inner display (1840 × 2208), dark/light themes and 200% text. The single switch remains reachable with scrolling at large text sizes.
- Lucide fold-horizontal UI icon and custom square with three equally spaced center dashes are included. Adaptive launcher icon includes a monochrome variant.
- Installed via explicitly targeted ADB on the connected Pixel 11 Pro Fold, Android API 37. Package installation succeeded; version 1.0.0 verified. Google exposes a public wake-up hinge sensor; the app accepts wake-up and non-wake-up sensors.
- App permissions: only its own AndroidX signature-protected receiver permission. Accessibility access is granted separately through Android settings. No Internet, wake lock, overlay permission, root or Samsung dependency.

The accessibility permission on the physical phone requires the user's first-run setup. Physical folding, screen handoff timing, 120 Hz frame pacing and battery energy use are not claimed as measured. Android controls display handoff and screen locking. The transition uses one temporary image rather than continuous screen capture. Secure content refusal uses live shading.

The emulator smoke check remains runnable in `scripts/check_pixel_emulator.py` and explicitly rejects physical phone targets.

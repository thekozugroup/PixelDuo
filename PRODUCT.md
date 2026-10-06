# Pixel Duo

Adapt https://github.com/joeconsorti/duo-fold-live for the Pixel Fold series, including Pixel 11 Pro Fold. Deliver an APK the user can install. The animation follows the physical hinge. Use Material Design 3 Expressive and native Pixel conventions. Keep the interface minimal, simple and intuitive, with one on/off toggle and necessary one-time Android permission setup. Remove Samsung-specific features and dependencies. Optimize battery use and smooth animation.

Platform: Android, API 33 and newer, covering every Pixel Fold generation. Audience: Pixel Fold owners. Visual commitment: native Pixel, official Material 3 Expressive, dynamic wallpaper colors, system typography, no custom visual brand or settings menu. Surface mode: Operate. The closed and unfolded layouts must fit their available space and system font size.

Acceptance: signed release APK; real public hinge sensor; no root, Shizuku or vendor firmware; hardware rendering paced by display frames while moving; no active render loop when settled; sensor off when screen off or toggle off; secure content capture failures degrade safely; overlay cannot intercept input; first-run setup explains the screenshot capability; tests, lint and emulator validation; retain upstream licensing. Do not claim physical Pixel 11 testing, a battery benchmark, universal smoothness or design certification without evidence.

Implementation scope audit: matches the user's requested devices, animation, interface and installable artifact. Local iteration and a deterministic Gradle verification gate follow the Babysitter library pattern. Emulator and signature verification are explicit final stages. No requested scope is removed.

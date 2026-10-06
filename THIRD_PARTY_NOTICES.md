# Third-party notices

Pixel Duo retains Duo Fold Live's MIT animation shader, adapted from the MIT iPhone Duo reference by jadon7. The Pixel version uses Android's public hinge sensor and accessibility APIs. Shizuku, HiddenApiBypass and Samsung firmware integrations are removed from the application and dependencies. Their historical license files are retained from upstream.

The UI uses Lucide's `fold-horizontal` vector under the ISC License. Its full license is bundled in `app/src/main/assets/licenses/lucide.txt`. The square-and-three-dashes launcher artwork is specific to this adaptation.

This repository preserves the upstream notices in `app/src/main/assets/licenses/` and `upstream/`. Shizuku and HiddenApiBypass entries below document the original project; neither is used by PixelDuo.

| Component | Source | License |
| --- | --- | --- |
| Upstream animation engine, originally derived from Duo Launcher | https://github.com/McCal-Codes/folio/tree/v0.6.0 | MIT; Duo Launcher contributors |
| iPhone Duo shader reference and adapted animation | https://github.com/chuspeeism/iphone-duo | MIT; jadon7 |
| FoldUO portions credited in the existing distribution | `app/src/main/assets/licenses/FOLDUO-MIT.txt` | MIT; bunkaich |
| Shizuku API and provider | https://github.com/RikkaApps/Shizuku-API | Apache-2.0 |
| HiddenApiBypass 6.1 | https://github.com/LSPosed/AndroidHiddenApiBypass | Apache-2.0 |
| AndroidX, Compose, Kotlin, coroutines, Gradle | See upstream notices and dependency graph | Apache-2.0 and their preserved upstream notices |

`reference/iphone-duo/main.js` preserves the MIT reference source used for the shader adaptation. No Samsung wallpaper APKs or videos, Apple device imagery, user photos, device logs, or signing keys are included.

The upstream Folio notices describe Folio as it existed upstream. PixelDuo uses only the adapted shader subset, with public Android screen capture and fold sensing. The iPhone Duo adaptation is code-derived.

# Privacy

PixelDuo uses an Android accessibility service to capture a temporary screen image and display a touch-through fold animation. Captured pixels are processed locally, held in memory during the transition, and released when the animation ends. Images are never saved or uploaded. Android may refuse capture of protected content.

The service does not request access to accessibility node contents. The app has no network permission, analytics, account, advertising or diagnostic upload feature. It stores only the on/off preference in app-private settings.

Turn off Fold animation to stop sensing and rendering. Accessibility access can also be revoked in Android Settings → Accessibility → Downloaded apps → Pixel Duo. The animation pauses while the screen is off, Battery Saver is on, or system animations are disabled.

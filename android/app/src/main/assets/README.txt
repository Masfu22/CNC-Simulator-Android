The Android APK workflow copies the built upstream browser renderer into assets/www before Gradle packaging.
Do not add a hand-written placeholder index.html here; build it from the root project with npm run build:only.

# Step Counter (Android, Kotlin, Jetpack Compose, Room)

## Build
1. Install Android Studio (latest). Open it -> Open -> choose this StepCounter folder.
2. Wait for "Gradle sync" to finish (first time downloads about 1 GB; needs internet).
3. Plug in your phone (USB debugging on) -> press the green Run button.
   Or: Build -> Build APK(s) -> copy app-debug.apk to the phone and install it.

## How counting works
- Uses TYPE_STEP_COUNTER (cumulative since boot). A baseline is kept in Room, only the
  difference is added to the day the steps happened on. Nothing is reset at midnight.
- Reboot detected with Settings.Global.BOOT_COUNT (plus elapsedRealtime check).
- Foreground "health" service with 60 s sensor batching + 15-min WorkManager snapshot + boot receiver.
- Steps taken before the app was first opened cannot be known; counting starts on first launch.

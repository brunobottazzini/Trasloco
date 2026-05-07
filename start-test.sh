#! /bin/bash

# Espresso requires animations disabled on the target device/emulator.
# Restore with `1` after testing if you prefer animations on day-to-day use.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

./gradlew connectedAndroidTest

#Single class test
#./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bottazzini.trasloco.RotationStateTest

#Single test case
#./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bottazzini.trasloco.RotationStateTest#gameState_survivesActivityRecreation

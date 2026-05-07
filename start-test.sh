#! /bin/bash

./gradlew connectedAndroidTest

#Single class test
#./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bottazzini.trasloco.RotationStateTest

#Single test case
#./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bottazzini.trasloco.RotationStateTest#gameState_survivesActivityRecreation

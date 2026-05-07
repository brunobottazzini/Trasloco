package com.bottazzini.trasloco

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeNavigationTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
    }

    @Test
    fun mainActivity_displaysAllMenuButtons() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.buttonStart)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonShowRecords)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonSetting)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonTopRightRules)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainActivity_navigatesToRules() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.buttonTopRightRules)).perform(click())
            onView(withId(R.id.textViewRulesScreenTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonGotIt)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainActivity_navigatesToRecords() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.buttonShowRecords)).perform(click())
            onView(withId(R.id.textViewRecordsTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainActivity_navigatesToSettings() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.buttonSetting)).perform(click())
            onView(withId(R.id.switchFastDeal)).check(matches(isDisplayed()))
            onView(withId(R.id.radioGroupCardBack)).check(matches(isDisplayed()))
            onView(withId(R.id.radioGroupBackgroundRow1)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainActivity_navigatesToGame() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.buttonStart)).perform(click())
            onView(withId(R.id.gameConstraintLayout)).check(matches(isDisplayed()))
            onView(withId(R.id.textViewGameTimer)).check(matches(isDisplayed()))
            onView(withId(R.id.resetButton)).check(matches(isDisplayed()))
        }
    }
}

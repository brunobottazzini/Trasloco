package com.bottazzini.trasloco

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RulesActivityTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        ActivityScenario.launch(MainActivity::class.java).close()
    }

    @Test
    fun rulesActivity_displaysTitleAndButton() {
        ActivityScenario.launch(RulesActivity::class.java).use {
            onView(withId(R.id.textViewRulesScreenTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.linearLayoutRulesContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonGotIt)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun rulesActivity_gotItButtonFinishesActivity() {
        val scenario = ActivityScenario.launch(RulesActivity::class.java)
        onView(withId(R.id.buttonGotIt)).perform(click())
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }
}

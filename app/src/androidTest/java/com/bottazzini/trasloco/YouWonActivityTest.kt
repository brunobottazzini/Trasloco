package com.bottazzini.trasloco

import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YouWonActivityTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        // Populate default settings (background config used by YouWonActivity)
        ActivityScenario.launch(MainActivity::class.java).close()
        // Pre-populate records as if a game was just won
        TestHelpers.seedRecords(
            bestTimeMillis = 150_000L,    // best 02:30
            currentTimeMillis = 145_000L, // last game 02:25 (new record)
            isNewTime = true,
            bestConsecutive = 5L,
            currentConsecutive = 3L,
            isNewConsecutive = false
        )
    }

    @Test
    fun youWonActivity_displaysAllRequiredViews() {
        ActivityScenario.launch(YouWonActivity::class.java).use {
            onView(withId(R.id.textViewYouWon)).check(matches(isDisplayed()))
            onView(withId(R.id.imageViewPartyGif)).check(matches(isDisplayed()))
            onView(withId(R.id.textViewTimeTaken)).check(matches(isDisplayed()))
            onView(withId(R.id.textConcurrentWin)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonNewGameYouWon)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonMenuYouWon)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonExitYouWon)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun youWonActivity_displaysNonEmptyTimeAndConsecutiveText() {
        ActivityScenario.launch(YouWonActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val time = activity.findViewById<TextView>(R.id.textViewTimeTaken).text.toString()
                val consec =
                    activity.findViewById<TextView>(R.id.textConcurrentWin).text.toString()
                assertTrue(
                    "Time text should be populated from seeded records",
                    time.isNotEmpty()
                )
                assertTrue(
                    "Consecutive text should be populated from seeded records",
                    consec.isNotEmpty()
                )
            }
        }
    }

    @Test
    fun youWonActivity_timeTextSurvivesRecreation() {
        ActivityScenario.launch(YouWonActivity::class.java).use { scenario ->
            var timeBefore = ""
            scenario.onActivity { activity ->
                timeBefore =
                    activity.findViewById<TextView>(R.id.textViewTimeTaken).text.toString()
            }
            assertTrue("Time should be set before rotation", timeBefore.isNotEmpty())

            scenario.recreate()

            scenario.onActivity { activity ->
                val timeAfter =
                    activity.findViewById<TextView>(R.id.textViewTimeTaken).text.toString()
                assertEquals(
                    "Time text must survive rotation (regression check: previously DB was reset to 0)",
                    timeBefore,
                    timeAfter
                )
            }
        }
    }

    @Test
    fun youWonActivity_consecutiveTextSurvivesRecreation() {
        ActivityScenario.launch(YouWonActivity::class.java).use { scenario ->
            var before = ""
            scenario.onActivity { activity ->
                before = activity.findViewById<TextView>(R.id.textConcurrentWin).text.toString()
            }
            assertTrue("Consecutive text should be set before rotation", before.isNotEmpty())

            scenario.recreate()

            scenario.onActivity { activity ->
                val after =
                    activity.findViewById<TextView>(R.id.textConcurrentWin).text.toString()
                assertEquals(before, after)
            }
        }
    }

    @Test
    fun youWonActivity_buttonsClickableAfterRecreation() {
        ActivityScenario.launch(YouWonActivity::class.java).use { scenario ->
            scenario.recreate()
            onView(withId(R.id.buttonNewGameYouWon)).check(matches(isClickable()))
            onView(withId(R.id.buttonMenuYouWon)).check(matches(isClickable()))
            onView(withId(R.id.buttonExitYouWon)).check(matches(isClickable()))
        }
    }

    @Test
    fun youWonActivity_gifUrlSurvivesRecreation() {
        ActivityScenario.launch(YouWonActivity::class.java).use { scenario ->
            var urlBefore: String? = null
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity).get(YouWonViewModel::class.java)
                urlBefore = vm.gifUrl
            }
            assertNotNull(
                "ViewModel must hold a GIF URL after first load",
                urlBefore
            )

            scenario.recreate()

            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity).get(YouWonViewModel::class.java)
                assertEquals(
                    "GIF URL must survive rotation (no random reload)",
                    urlBefore,
                    vm.gifUrl
                )
            }
        }
    }
}

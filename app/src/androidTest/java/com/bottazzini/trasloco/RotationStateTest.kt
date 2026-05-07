package com.bottazzini.trasloco

import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RotationStateTest {

    private val gridIds = intArrayOf(
        R.id.subDeck11, R.id.subDeck12, R.id.subDeck13, R.id.subDeck14,
        R.id.subDeck21, R.id.subDeck22, R.id.subDeck23, R.id.subDeck24,
        R.id.subDeck31, R.id.subDeck32, R.id.subDeck33, R.id.subDeck34,
        R.id.subDeck41, R.id.subDeck42, R.id.subDeck43, R.id.subDeck44
    )

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        ActivityScenario.launch(MainActivity::class.java).close()
    }

    @Test
    fun gameState_survivesActivityRecreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            val tagsBefore = HashMap<Int, String>()
            scenario.onActivity { activity ->
                gridIds.forEach { id ->
                    tagsBefore[id] = activity.findViewById<ImageView>(id).tag as String
                }
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                gridIds.forEach { id ->
                    val tagAfter = activity.findViewById<ImageView>(id).tag as String
                    assertEquals(
                        "Card at slot $id should be the same after recreate (ViewModel restore)",
                        tagsBefore[id],
                        tagAfter
                    )
                }
            }
        }
    }

    @Test
    fun selection_survivesActivityRecreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            onView(withId(R.id.subDeck11)).perform(click())

            scenario.onActivity { activity ->
                assertNotEquals(
                    "Selection foreground should be set before recreate",
                    null,
                    activity.findViewById<ImageView>(R.id.subDeck11).foreground
                )
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertNotEquals(
                    "Selection foreground should still be set after recreate",
                    null,
                    activity.findViewById<ImageView>(R.id.subDeck11).foreground
                )
            }
        }
    }

    @Test
    fun subDeckPick_survivesActivityRecreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            onView(withId(R.id.subDeck1)).perform(click())

            val tagAfterPick = arrayOf("")
            scenario.onActivity { activity ->
                tagAfterPick[0] = activity.findViewById<ImageView>(R.id.subDeck12).tag as String
            }
            assertNotEquals(
                "Sub-deck pick should fill subDeck12",
                "zero",
                tagAfterPick[0]
            )

            scenario.recreate()

            scenario.onActivity { activity ->
                val tagAfterRecreate =
                    activity.findViewById<ImageView>(R.id.subDeck12).tag as String
                assertEquals(
                    "subDeck12 should keep the picked card after recreate",
                    tagAfterPick[0],
                    tagAfterRecreate
                )
            }
        }
    }

    @Test
    fun gameTimer_doesNotResetAfterRecreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // Wait for at least one timer tick (timer updates every 1000ms)
            Thread.sleep(1_500)
            var beforeRotation = ""
            scenario.onActivity { activity ->
                beforeRotation =
                    activity.findViewById<TextView>(R.id.textViewGameTimer).text.toString()
            }
            assertNotEquals(
                "Timer should have ticked at least once before rotation",
                "00:00",
                beforeRotation
            )

            scenario.recreate()
            Thread.sleep(1_500) // give timer time to resume and tick after recreate

            scenario.onActivity { activity ->
                val afterRotation =
                    activity.findViewById<TextView>(R.id.textViewGameTimer).text.toString()
                assertNotEquals(
                    "Timer must not reset to 00:00 after rotation (ViewModel preserves gameStartTimeMillis)",
                    "00:00",
                    afterRotation
                )
            }
        }
    }

    @Test
    fun multipleRecreations_doNotCorruptState() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            val initialTags = HashMap<Int, String>()
            scenario.onActivity { activity ->
                gridIds.forEach { id ->
                    initialTags[id] = activity.findViewById<ImageView>(id).tag as String
                }
            }

            repeat(3) { scenario.recreate() }

            scenario.onActivity { activity ->
                gridIds.forEach { id ->
                    assertEquals(
                        "Slot $id should match initial state after 3 recreations",
                        initialTags[id],
                        activity.findViewById<ImageView>(id).tag as String
                    )
                }
            }
        }
    }
}

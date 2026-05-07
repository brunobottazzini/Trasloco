package com.bottazzini.trasloco

import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameInteractionTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        // GameActivity reads settings populated by MainActivity on first launch
        ActivityScenario.launch(MainActivity::class.java).close()
    }

    @Test
    fun gameStartsWithSubDecksAndDealtCards() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                listOf(R.id.subDeck1, R.id.subDeck2, R.id.subDeck3, R.id.subDeck4).forEach { id ->
                    val view = activity.findViewById<ImageView>(id)
                    assertNotNull("subDeck $id should exist", view)
                    assertNotNull("subDeck $id should have a tag", view.tag)
                }
                listOf(
                    R.id.subDeck11, R.id.subDeck21, R.id.subDeck31, R.id.subDeck41
                ).forEach { id ->
                    val view = activity.findViewById<ImageView>(id)
                    val tag = view.tag as String
                    assertNotEquals(
                        "First slot of each row should have a card dealt (not 'zero')",
                        "zero",
                        tag
                    )
                }
            }
        }
    }

    @Test
    fun gameEndDeckSlotsStartEmpty() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                listOf(R.id.subDeck14, R.id.subDeck24, R.id.subDeck34, R.id.subDeck44)
                    .forEach { id ->
                        val view = activity.findViewById<ImageView>(id)
                        assertEquals(
                            "End deck slot $id should be empty (zero) at game start",
                            "zero",
                            view.tag as String
                        )
                    }
            }
        }
    }

    @Test
    fun gameUndoDisabledOnFreshGame() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val resetButton = activity.findViewById<android.widget.Button>(R.id.resetButton)
                assertEquals(false, resetButton.isEnabled)
            }
        }
    }

    @Test
    fun subDeckClick_dealsCardToFirstEmptySlot() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // Slot 12 is empty after first deal — capture state then trigger sub-deck click on row 1
            val initialTag = arrayOf("")
            scenario.onActivity { activity ->
                initialTag[0] = activity.findViewById<ImageView>(R.id.subDeck12).tag as String
            }
            assertEquals("zero", initialTag[0])

            onView(withId(R.id.subDeck1)).perform(click())

            scenario.onActivity { activity ->
                val newTag = activity.findViewById<ImageView>(R.id.subDeck12).tag as String
                assertNotEquals(
                    "After sub-deck click, subDeck12 should hold a card",
                    "zero",
                    newTag
                )
            }
        }
    }

    @Test
    fun cardClick_setsSelectionForeground() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            onView(withId(R.id.subDeck11)).perform(click())
            scenario.onActivity { activity ->
                val view = activity.findViewById<ImageView>(R.id.subDeck11)
                assertNotNull(
                    "Selected card should have a foreground (selection border)",
                    view.foreground
                )
            }
        }
    }

    @Test
    fun secondClickOnEmptySlot_doesNothing() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // subDeck12 is empty (zero), clicking it should not start a selection
            onView(withId(R.id.subDeck12)).perform(click())
            scenario.onActivity { activity ->
                val view = activity.findViewById<ImageView>(R.id.subDeck12)
                assertNull(
                    "Empty slot should not be selectable",
                    view.foreground
                )
            }
        }
    }
}

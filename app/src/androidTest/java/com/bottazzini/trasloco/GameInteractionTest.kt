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
    fun gameTable_isFullyDealtAtStart() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val gameSlots = listOf(
                    R.id.subDeck11, R.id.subDeck12, R.id.subDeck13,
                    R.id.subDeck21, R.id.subDeck22, R.id.subDeck23,
                    R.id.subDeck31, R.id.subDeck32, R.id.subDeck33,
                    R.id.subDeck41, R.id.subDeck42, R.id.subDeck43
                )
                gameSlots.forEach { id ->
                    val tag = activity.findViewById<ImageView>(id).tag as String
                    assertNotEquals(
                        "Slot $id should hold a card after initial deal",
                        "zero",
                        tag
                    )
                }
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
    fun firstClickOnEndDeckSlot_doesNotSelect() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // End deck slots (XX4) start as "zero" and are not selectable when no card is held
            onView(withId(R.id.subDeck14)).perform(click())
            scenario.onActivity { activity ->
                val view = activity.findViewById<ImageView>(R.id.subDeck14)
                assertNull(
                    "End deck (zero) should not be selectable from clean state",
                    view.foreground
                )
                assertEquals(
                    "End deck slot must remain empty",
                    "zero",
                    view.tag as String
                )
            }
        }
    }
}

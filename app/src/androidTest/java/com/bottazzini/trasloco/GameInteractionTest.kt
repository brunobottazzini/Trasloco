package com.bottazzini.trasloco

import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

    @Test
    fun pileCounters_areInvisibleAtGameStart() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val counterIds = listOf(
                    R.id.textView11, R.id.textView12, R.id.textView13,
                    R.id.textView21, R.id.textView22, R.id.textView23,
                    R.id.textView31, R.id.textView32, R.id.textView33,
                    R.id.textView41, R.id.textView42, R.id.textView43
                )
                counterIds.forEach { id ->
                    val tv = activity.findViewById<TextView>(id)
                    assertEquals(
                        "Counter $id must be INVISIBLE at game start (1 card per slot)",
                        View.INVISIBLE,
                        tv.visibility
                    )
                    assertEquals(
                        "Counter $id text must be empty at game start",
                        "",
                        tv.text.toString()
                    )
                }
            }
        }
    }

    @Test
    fun pileCounter_displaysSizeAndBecomesVisibleWhenStackGrows() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // Inject extra cards into the activity's cardTableMap[11].
            // recreate() then triggers onStop -> snapshotToViewModel() (captures the
            // injected stack) -> onCreate -> restoreGameFromViewModel() which calls
            // setNumberOfCards() and updates the badge for slot 11.
            scenario.onActivity { activity ->
                val field = GameActivity::class.java.getDeclaredField("cardTableMap")
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val cardTableMap =
                    field.get(activity) as HashMap<String, ArrayList<String>>
                cardTableMap["11"]?.apply {
                    add("c4")
                    add("c5")
                }
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                val pile = activity.findViewById<TextView>(R.id.textView11)
                assertEquals(
                    "Counter for stack of 3 must be VISIBLE",
                    View.VISIBLE,
                    pile.visibility
                )
                assertEquals(
                    "Counter text must reflect stack size",
                    "3",
                    pile.text.toString()
                )
                assertNotNull(
                    "Counter must have a badge background drawable",
                    pile.background
                )

                // A non-stacked sibling slot must remain hidden
                val singleSlot = activity.findViewById<TextView>(R.id.textView12)
                assertEquals(
                    "Slot with a single card must keep its counter INVISIBLE",
                    View.INVISIBLE,
                    singleSlot.visibility
                )
            }
        }
    }
}

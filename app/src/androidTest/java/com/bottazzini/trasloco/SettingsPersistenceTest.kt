package com.bottazzini.trasloco

import android.widget.RadioButton
import android.widget.Switch
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
class SettingsPersistenceTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        // Settings table is populated by MainActivity on first launch
        ActivityScenario.launch(MainActivity::class.java).close()
    }

    @Test
    fun fastDealToggle_persistsAcrossActivityRestart() {
        var initialChecked = false

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                initialChecked = activity.findViewById<Switch>(R.id.switchFastDeal).isChecked
            }
            onView(withId(R.id.switchFastDeal)).perform(click())
        }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val newChecked = activity.findViewById<Switch>(R.id.switchFastDeal).isChecked
                assertNotEquals(
                    "Fast Deal switch should be flipped after restart",
                    initialChecked,
                    newChecked
                )
            }
        }
    }

    @Test
    fun cardBackSelection_persistsAcrossActivityRestart() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.rbCardBack3)).perform(click())
        }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rb = activity.findViewById<RadioButton>(R.id.rbCardBack3)
                assertEquals(
                    "Selected card back (rbCardBack3) should remain checked after restart",
                    true,
                    rb.isChecked
                )
            }
        }
    }

    @Test
    fun backgroundSelection_persistsAcrossActivityRestart() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.rbTavolo)).perform(click())
        }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rb = activity.findViewById<RadioButton>(R.id.rbTavolo)
                assertEquals(
                    "Selected background (rbTavolo) should remain checked after restart",
                    true,
                    rb.isChecked
                )
            }
        }
    }

    @Test
    fun selectingBackgroundInRow2_clearsRow1Selection() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.rbPanno)).perform(click())
            onView(withId(R.id.rbTavolo)).perform(click())
            scenario.onActivity { activity ->
                val rbPanno = activity.findViewById<RadioButton>(R.id.rbPanno)
                val rbTavolo = activity.findViewById<RadioButton>(R.id.rbTavolo)
                assertEquals("rbPanno (row1) should be cleared", false, rbPanno.isChecked)
                assertEquals("rbTavolo (row2) should be checked", true, rbTavolo.isChecked)
            }
        }
    }

    @Test
    fun cardTypeDefaultIsPiacentine() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rb = activity.findViewById<RadioButton>(R.id.rbPiacentine)
                assertEquals(
                    "On a fresh install, default card type must be Piacentine",
                    true,
                    rb.isChecked
                )
            }
        }
    }

    @Test
    fun cardTypeSelection_persistsAcrossActivityRestart() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.rbFrancesi)).perform(click())
        }

        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val rb = activity.findViewById<RadioButton>(R.id.rbFrancesi)
                assertEquals(
                    "Selected card type (rbFrancesi) should remain checked after restart",
                    true,
                    rb.isChecked
                )
            }
        }
    }
}

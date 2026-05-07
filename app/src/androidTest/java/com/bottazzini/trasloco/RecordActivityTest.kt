package com.bottazzini.trasloco

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordActivityTest {

    @Before
    fun setUp() {
        TestHelpers.resetAppData()
        // Defaults inserted by MainActivity / RecordsHandler.insertDefaultSettings
        ActivityScenario.launch(MainActivity::class.java).close()
    }

    @Test
    fun recordActivity_showsNoRecordsMessageOnFirstLaunch() {
        ActivityScenario.launch(RecordActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val noRecords = activity.findViewById<TextView>(R.id.textViewNoRecords)
                val bestTimeLabel = activity.findViewById<TextView>(R.id.textViewBestTimeLabel)
                val consecutiveLabel =
                    activity.findViewById<TextView>(R.id.textViewConsecutiveWinsLabel)

                assertEquals(
                    "noRecords should be visible on a fresh install",
                    View.VISIBLE,
                    noRecords.visibility
                )
                assertEquals("bestTime label should be hidden", View.GONE, bestTimeLabel.visibility)
                assertEquals(
                    "consecutiveWins label should be hidden",
                    View.GONE,
                    consecutiveLabel.visibility
                )
            }
        }
    }

    @Test
    fun recordActivity_titleIsRendered() {
        ActivityScenario.launch(RecordActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val title = activity.findViewById<TextView>(R.id.textViewRecordsTitle)
                assertEquals(View.VISIBLE, title.visibility)
            }
        }
    }

    @Test
    fun recordActivity_showsRecordsWhenPopulated() {
        TestHelpers.seedRecords(
            bestTimeMillis = 150_000L,
            currentTimeMillis = 145_000L,
            isNewTime = false,
            bestConsecutive = 5L,
            currentConsecutive = 3L,
            isNewConsecutive = false
        )

        ActivityScenario.launch(RecordActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val noRecords = activity.findViewById<TextView>(R.id.textViewNoRecords)
                val bestTimeLabel = activity.findViewById<TextView>(R.id.textViewBestTimeLabel)
                val bestTimeValue = activity.findViewById<TextView>(R.id.textViewBestTimeValue)
                val consecLabel =
                    activity.findViewById<TextView>(R.id.textViewConsecutiveWinsLabel)
                val consecValue =
                    activity.findViewById<TextView>(R.id.textViewConsecutiveWinsValue)

                assertEquals(
                    "noRecords should be hidden when records exist",
                    View.GONE,
                    noRecords.visibility
                )
                assertEquals(View.VISIBLE, bestTimeLabel.visibility)
                assertEquals(View.VISIBLE, bestTimeValue.visibility)
                assertEquals(View.VISIBLE, consecLabel.visibility)
                assertEquals(View.VISIBLE, consecValue.visibility)
                assertTrue(
                    "Best time value should be populated",
                    bestTimeValue.text.toString().isNotEmpty()
                )
                assertEquals(
                    "Consecutive wins should display the seeded value",
                    "5",
                    consecValue.text.toString()
                )
            }
        }
    }

    @Test
    fun recordActivity_showsOnlyConsecutiveWhenTimeMissing() {
        // Best time = -1L means "no record yet" per RecordActivity.loadRecords()
        TestHelpers.seedRecords(
            bestTimeMillis = -1L,
            currentTimeMillis = 0L,
            isNewTime = false,
            bestConsecutive = 4L,
            currentConsecutive = 1L,
            isNewConsecutive = false
        )

        ActivityScenario.launch(RecordActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bestTimeLabel = activity.findViewById<TextView>(R.id.textViewBestTimeLabel)
                val consecLabel =
                    activity.findViewById<TextView>(R.id.textViewConsecutiveWinsLabel)
                assertEquals(
                    "Best time should be hidden when never set",
                    View.GONE,
                    bestTimeLabel.visibility
                )
                assertEquals(
                    "Consecutive should be visible when there are wins",
                    View.VISIBLE,
                    consecLabel.visibility
                )
            }
        }
    }
}

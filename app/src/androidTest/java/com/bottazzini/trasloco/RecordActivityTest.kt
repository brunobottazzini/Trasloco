package com.bottazzini.trasloco

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bottazzini.trasloco.util.TestHelpers
import org.junit.Assert.assertEquals
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
}

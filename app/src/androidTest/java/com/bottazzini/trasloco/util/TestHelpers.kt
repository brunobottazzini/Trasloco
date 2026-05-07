package com.bottazzini.trasloco.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher
import java.io.File

object TestHelpers {

    fun resetAppData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("Trasloco.db")
        val dbDir = File(context.applicationInfo.dataDir, "databases")
        if (dbDir.exists()) {
            dbDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun readImageViewTag(): ViewAction = object : ViewAction {
        var capturedTag: String? = null
        override fun getConstraints(): Matcher<View> = isAssignableFrom(ImageView::class.java)
        override fun getDescription(): String = "read tag from ImageView"
        override fun perform(uiController: UiController, view: View) {
            capturedTag = view.tag as? String
        }
    }
}

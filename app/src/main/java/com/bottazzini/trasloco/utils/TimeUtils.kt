package com.bottazzini.trasloco.utils

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit

class TimeUtils {
    companion object {
        @SuppressLint("DefaultLocale") // Per String.format con Locale
        fun formatTime(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
package com.bottazzini.trasloco.db.columns

import android.provider.BaseColumns

object AchievementsColumns {
    object AchievementEntry : BaseColumns {
        const val TABLE_NAME = "achievements"
        const val COLUMN_ID = "id"
        const val COLUMN_UNLOCKED_AT = "unlocked_at"
    }
}

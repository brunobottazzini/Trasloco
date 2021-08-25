package com.bottazzini.trasloco.db.columns

import android.provider.BaseColumns

object SettingsBaseColumns {
    object SettingEntry : BaseColumns {
        const val TABLE_NAME = "settings"
        const val COLUMN_NAME = "name"
        const val COLUMN_VALUE = "value"
    }
}
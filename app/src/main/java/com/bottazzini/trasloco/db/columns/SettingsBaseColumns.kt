package com.bottazzini.trasloco.db.columns

import android.provider.BaseColumns

object SettingsBaseColumns {
    object SettingEntry : BaseColumns {
        const val TABLE_NAME = "settings"
        const val COLUMN_NAME = "name"
        const val COLUMN_VALUE = "value"
    }
}

object RecordsColumns {
    object RecordEntry : BaseColumns {
        const val TABLE_NAME = "records"
        const val COLUMN_TYPE = "type"
        const val COLUMN_VALUE = "value"
        const val COLUMN_CURRENT_VALUE = "current_value"
        const val COLUMN_NEW = "new"
    }
}
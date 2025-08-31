package com.flux.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN is24HourFormat INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN isAttentionManagerEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN blockedApps TEXT NOT NULL DEFAULT '[]'")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3
)
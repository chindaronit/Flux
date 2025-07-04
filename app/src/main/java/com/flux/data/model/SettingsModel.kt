package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SettingsModel(
    @PrimaryKey
    val settingId: String = "Settings",
    val contrast: Int = 0,
    val isBiometricEnabled: Boolean=false,
    val isGridView: Boolean=true,
    val isCalenderMonthlyView: Boolean=false,
    val isDarkMode: Boolean = false,
    val isAutomaticTheme: Boolean = true,
    val cornerRadius: Int = 32,
    val dynamicTheme: Boolean = false,
    val amoledTheme: Boolean = false,
    val isScreenProtection: Boolean = false
)

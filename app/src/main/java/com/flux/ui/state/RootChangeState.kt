package com.flux.ui.state

sealed interface RootChangeState {
    data object Idle : RootChangeState
    data class Copying(val copied: Int = 0, val total: Int = 0) : RootChangeState
    data class Failed(val reason: String, val failedFiles: List<String> = emptyList()) : RootChangeState
    data object Success : RootChangeState
}
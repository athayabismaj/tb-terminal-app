package com.tbterminal.app.ui.common

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
}

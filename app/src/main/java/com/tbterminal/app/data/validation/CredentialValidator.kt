package com.tbterminal.app.data.validation

object CredentialValidator {
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 72
    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 6

    val PIN_REGEX = Regex("^\\d{$PIN_MIN_LENGTH,$PIN_MAX_LENGTH}$")

    fun isValidPasswordLength(password: String): Boolean =
        password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH

    fun isValidPin(pin: String): Boolean = PIN_REGEX.matches(pin)

    fun validatePassword(
        password: String,
        requiredMessage: String = "Password wajib diisi.",
        minMessage: String = "Password minimal $PASSWORD_MIN_LENGTH karakter.",
        maxMessage: String = "Password maksimal $PASSWORD_MAX_LENGTH karakter."
    ): String? = when {
        password.isBlank() -> requiredMessage
        password.length < PASSWORD_MIN_LENGTH -> minMessage
        password.length > PASSWORD_MAX_LENGTH -> maxMessage
        else -> null
    }

    fun validatePin(
        pin: String,
        requiredMessage: String = "PIN wajib diisi.",
        nonDigitMessage: String = "PIN harus terdiri dari $PIN_MIN_LENGTH sampai $PIN_MAX_LENGTH digit angka.",
        lengthMessage: String = "PIN harus terdiri dari $PIN_MIN_LENGTH sampai $PIN_MAX_LENGTH digit angka."
    ): String? = when {
        pin.isBlank() -> requiredMessage
        !pin.all(Char::isDigit) -> nonDigitMessage
        pin.length !in PIN_MIN_LENGTH..PIN_MAX_LENGTH -> lengthMessage
        else -> null
    }
}

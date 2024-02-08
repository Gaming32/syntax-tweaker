package io.github.gaming32.syntaxtweaker.tweaks.parser

class InvalidTweaksException(
    message: String, val line: Int = -1, val column: Int = -1, cause: Exception? = null
) : IllegalArgumentException(message, cause) {
    internal constructor(message: String, token: Token, cause: Exception? = null)
        : this(message, token.line, token.column, cause)
}

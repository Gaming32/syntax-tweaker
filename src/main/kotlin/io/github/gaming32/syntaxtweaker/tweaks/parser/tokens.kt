package io.github.gaming32.syntaxtweaker.tweaks.parser

import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import java.io.Reader

internal enum class SimpleTokenType(val char: Char) {
    LPAREN('('),
    RPAREN(')'),
    LCURLY('{'),
    RCURLY('}'),
    EQUALS('='),
    COMMA(','),
    SEMI(';');

    override fun toString() = char.toString()

    companion object {
        val BY_CHAR = arrayOfNulls<SimpleTokenType>(entries.maxOf { it.char.code } + 1).also {
            for (token in entries) {
                it[token.char.code] = token
            }
        }
    }
}

internal sealed interface Token {
    val line: Int
    val column: Int
}

internal data class SimpleToken(val type: SimpleTokenType, override val line: Int, override val column: Int) : Token {
    override fun toString() = type.toString()
}

internal data class StringToken(val value: String, override val line: Int, override val column: Int) : Token {
    override fun toString(): String {
        val result = JsonString(value).toString()
        if ('\\' !in result) {
            return result.substring(1, result.length - 1)
        }
        return result
    }
}

internal class Tokenizer(private val reader: Reader) {
    companion object {
        private const val NULL = '\u0000'
    }

    private var peeked = NULL
    private var nextLine = false
    private var line = 1
    private var column = 0

    fun tokenize() = sequence {
        while (true) {
            val char = next()
            if (char == NULL) break
            val simpleToken = SimpleTokenType.BY_CHAR[char.code]
            if (simpleToken != null) {
                yield(SimpleToken(simpleToken, line, column))
                continue
            }
            if (char == '/') {
                when (val commentStart = next()) {
                    '/' -> {
                        while (true) {
                            val innerChar = next()
                            if (innerChar == NULL || innerChar == '\n') break
                        }
                    }
                    '*' -> {
                        while (true) {
                            val innerChar = next()
                            if (innerChar == NULL) {
                                error("Unterminated block comment")
                            }
                            if (innerChar == '*' && next() == '/') break
                        }
                    }
                    else -> error("Expected / to start a comment, found ${stringify(commentStart)}")
                }
                continue
            }
            if (char.isWhitespace()) continue
            val line = line
            val column = column
            if (char == '"') {
                yield(StringToken(readString(), line, column))
                continue
            }
            yield(StringToken(readLiteral(char), line, column))
        }
    }

    private fun readString() = buildString {
        // Read string, based off of org.jetbrains.kotlin.js.parser.sourcemaps.JsonParser.parseString
        while (true) {
            val char = next()

            if (char < ' ') {
                error("Invalid character in string literal: ${stringify(char)}")
            }

            when (char) {
                '"' -> return@buildString
                '\\' -> append(readEscape())
                NULL -> error("EOF in string literal")
                else -> append(char)
            }
        }
    }

    private fun readEscape() = when (val char = next()) {
        '"', '\\', '/' -> char
        'b' -> '\b'
        'n' -> '\n'
        'r' -> '\r'
        'f' -> '\u000c'
        't' -> '\t'
        'u' -> {
            var value = 0
            repeat(4) {
                value *= 16
                value += when (val innerChar = next()) {
                    in '0'..'9' -> innerChar - '0'
                    in 'a'..'f' -> innerChar - 'a' + 10
                    in 'A'..'F' -> innerChar - 'A' + 10
                    else -> error("Invalid unicode escape, hexadecimal char expacted, found ${stringify(innerChar)}")
                }
            }
            value.toChar()
        }
        else -> error("Invalid escape sequence: ${stringify(char)}")
    }

    private fun readLiteral(first: Char) = buildString {
        append(first)
        while (true) {
            val char = peek()
            if (SimpleTokenType.BY_CHAR[char.code] != null) break
            if (char == '/' || char == '"' || char.isWhitespace()) break
            append(next())
        }
    }

    private fun peek(): Char {
        if (peeked != NULL) {
            return peeked
        }
        val read = reader.read()
        if (read == -1) {
            return NULL
        }
        if (read == 0) {
            nullChar()
        }
        peeked = read.toChar()
        return peeked
    }

    private fun next(): Char {
        val read = if (peeked != NULL) {
            val read = peeked
            peeked = NULL
            read
        } else {
            val read = reader.read()
            if (read == -1) {
                return NULL
            }
            if (read == 0) {
                nullChar()
            }
            read.toChar()
        }
        if (nextLine) {
            line++
            column = 1
            nextLine = false
        } else {
            column++
        }
        if (read == '\n') {
            nextLine = true
        }
        return read
    }

    private fun nullChar(): Nothing =
        throw InvalidTweaksException("Null character in tweaks near $line:$column", line, column)

    private fun error(message: String, cause: Exception? = null): Nothing =
        throw InvalidTweaksException("$message at $line:$column", line, column, cause)

    private fun stringify(char: Char): String {
        val text = JsonString(char.toString()).toString()
        return text.substring(1, text.length - 1)
    }
}

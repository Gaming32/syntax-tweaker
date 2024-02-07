package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.data.MemberReference

fun interface TweakParser<T : SyntaxTweak> {
    fun ParseContext.parse(): T
}

data class ParseContext(
    val referenceType: ReferenceType,
    val owner: String,
    val member: MemberReference?,
    val args: List<String> = listOf()
) {
    enum class ReferenceType {
        FIELD, METHOD, CLASS
    }
}

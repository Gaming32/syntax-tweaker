package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.data.MemberReference

fun interface TweakParser<T : SyntaxTweak> {
    data class ParseContext(
        val referenceType: SyntaxTweak.ReferenceType,
        val owner: String,
        val member: MemberReference?,
        val args: List<String> = listOf()
    )

    fun ParseContext.parse(): T
}

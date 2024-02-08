package io.github.gaming32.syntaxtweaker.tweaks.parser

import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak

fun interface TweakParser<T : SyntaxTweak> {
    data class ParseContext(
        val metadata: Map<String, String>,
        val referenceType: SyntaxTweak.ReferenceType,
        val owner: String,
        val member: MemberReference?,
        val args: List<String> = listOf()
    )

    fun ParseContext.parse(): T
}

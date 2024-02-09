package io.github.gaming32.syntaxtweaker.tweaks.registry

import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak

fun interface TweakParser<T : SyntaxTweak> {
    data class ParseContext(
        val metadata: Map<String, String>,
        val referenceType: SyntaxTweak.ReferenceType,
        val owner: String,
        val member: MemberReference?,
        val args: List<String> = listOf(),
        val namedArgs: Map<String, String> = mapOf()
    ) {
        init {
            require(member == null || referenceType == member.referenceType) {
                "ParseContext referenceType ($referenceType) doesn't match member ($member)"
            }
        }
    }

    fun ParseContext.parse(): T
}

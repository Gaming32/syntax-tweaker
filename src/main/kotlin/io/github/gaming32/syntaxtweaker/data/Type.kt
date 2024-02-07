package io.github.gaming32.syntaxtweaker.data

import org.jetbrains.kotlin.com.intellij.psi.PsiArrayType
import org.jetbrains.kotlin.com.intellij.psi.PsiCapturedWildcardType
import org.jetbrains.kotlin.com.intellij.psi.PsiClassType
import org.jetbrains.kotlin.com.intellij.psi.PsiPrimitiveType
import org.jetbrains.kotlin.com.intellij.psi.PsiType
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import kotlin.math.min

sealed interface Type {
    data class ReferenceType(val qualifiedName: String) : Type {
        override fun toString() = qualifiedName
    }

    enum class PrimitiveType(val primitiveName: String) : Type {
        VOID("void"),
        BYTE("byte"),
        BOOLEAN("boolean"),
        SHORT("short"),
        CHAR("char"),
        INT("int"),
        FLOAT("float"),
        LONG("long"),
        DOUBLE("double");

        override fun toString() = primitiveName

        companion object {
            val BY_NAME = entries.associateBy(PrimitiveType::primitiveName)
        }
    }

    data class ArrayType(val dimensions: Int, val elementType: Type) : Type {
        init {
            require(dimensions in 1..255) { "dimensions must be in 1..255 (was $dimensions)" }
            require(elementType != PrimitiveType.VOID) { "elementType may not be void" }
            require(elementType !is ArrayType) { "elementType may not be another ArrayType" }
            require(elementType !is MethodType) { "elementType may not be MethodType" }
        }

        override fun toString() = elementType.toString() + "[]".repeat(dimensions)
    }

    data class MethodType(val returnType: Type?, val parameters: List<Type>) : Type {
        init {
            parameters.forEachIndexed { index, param ->
                require(param != PrimitiveType.VOID) { "param $index cannot be void" }
                require(param !is MethodType) { "param $index cannot be MethodType" }
            }
            require(returnType !is MethodType) { "returnType cannot be MethodType" }
        }

        override fun toString() = buildString {
            returnType?.let(this::append)
            append('(')
            parameters.forEachIndexed { index, param ->
                if (index > 0) {
                    append(',')
                }
                append(param)
            }
            append(')')
        }
    }

    companion object {
        fun String.toSyntaxTweakerType(): Type {
            require(isNotBlank()) { "Type \"$this\" may not be blank" }
            var trimmed = trim()
            val openParen = trimmed.indexOf('(')
            if (openParen != -1) {
                var index = openParen + 1
                val end = trimmed.indexOf(')', index)
                require(end != -1) { "Method type $trimmed has no ending parenthesis" }
                require(end == trimmed.lastIndex) {
                    "Method type $trimmed has trailing data: ${trimmed.substring(end + 1)}"
                }
                val returnType = if (openParen > 0) {
                    trimmed.substring(0, openParen).toSyntaxTweakerType()
                } else {
                    null
                }
                val parameters = buildList {
                    while (index < end) {
                        val paramEnd = min(end, trimmed.indexOfOrNull(',', index) ?: end)
                        add(trimmed.substring(index, paramEnd).toSyntaxTweakerType())
                        index = paramEnd + 1
                    }
                }
                return MethodType(returnType, parameters)
            }
            if (trimmed.endsWith("[]")) {
                var dims = 0
                do {
                    dims++
                    trimmed = trimmed.substring(0, trimmed.length - 2).trimEnd()
                } while (trimmed.endsWith("[]"))
                return ArrayType(dims, trimmed.toSyntaxTweakerType())
            }
            val primitive = PrimitiveType.BY_NAME[trimmed]
            if (primitive != null) {
                return primitive
            }
            return ReferenceType(trimmed)
        }

        fun PsiType.toSyntaxTweakerType(): Type {
            if (this is PsiArrayType) {
                return ArrayType(arrayDimensions, deepComponentType.toSyntaxTweakerType())
            }
            if (this is PsiPrimitiveType) {
                return PrimitiveType.BY_NAME[name]
                    ?: throw IllegalArgumentException("Cannot convert $this to PrimitiveType")
            }
            if (this is PsiClassType) {
                return ReferenceType(name)
            }
            if (this is PsiCapturedWildcardType) {
                return upperBound.toSyntaxTweakerType()
            }
            throw IllegalArgumentException("Cannot convert $this to Type")
        }
    }

    override fun toString(): String
}

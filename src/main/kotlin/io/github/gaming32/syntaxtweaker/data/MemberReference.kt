package io.github.gaming32.syntaxtweaker.data

import io.github.gaming32.syntaxtweaker.data.Type.Companion.toSyntaxTweakerType
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.util.getOrPutUserData
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMember
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod

data class MemberReference(val name: String, val type: Type) {
    companion object {
        private val MEMBER_REFERENCE_KEY = Key.create<MemberReference>(
            "io.github.gaming32.syntaxtweaker.data.memberReference"
        )

        fun PsiMember.toMemberReference() = getOrPutUserData(MEMBER_REFERENCE_KEY) {
            val name = name ?: return@getOrPutUserData null
            val type = when (this) {
                is PsiField -> type.toSyntaxTweakerType()
                is PsiMethod -> Type.MethodType(
                    returnType?.toSyntaxTweakerType(),
                    parameterList.parameters.map { it.type.toSyntaxTweakerType() }
                )
                else -> return@getOrPutUserData null
            }
            MemberReference(name, type)
        }

        operator fun invoke(name: String, type: String) =
            MemberReference(name, type.toSyntaxTweakerType())
    }

    val isMethod get() = type is Type.MethodType

    val referenceType get() =
        if (isMethod) {
            SyntaxTweak.ReferenceType.METHOD
        } else {
            SyntaxTweak.ReferenceType.FIELD
        }

    override fun toString() =
        if (type is Type.MethodType) {
            buildString {
                type.returnType?.let {
                    append(it)
                    append(' ')
                }
                append(name)
                append('(')
                type.parameters.forEachIndexed { index, param ->
                    if (index > 0) {
                        append(", ")
                    }
                    append(param)
                }
                append(')')
            }
        } else {
            "$type $name"
        }
}

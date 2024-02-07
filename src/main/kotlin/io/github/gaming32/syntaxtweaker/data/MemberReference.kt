package io.github.gaming32.syntaxtweaker.data

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMember
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.util.ClassUtil
import io.github.gaming32.syntaxtweaker.data.Descriptor.Companion.toDescriptor
import io.github.gaming32.syntaxtweaker.util.getOrPutUserData

data class MemberReference(val name: String, val descriptor: Descriptor) {
    companion object {
        private val MEMBER_REFERENCE_KEY = Key.create<MemberReference>(
            "io.github.gaming32.syntaxtweaker.data.memberReference"
        )

        fun PsiMember.toMemberReference() = getOrPutUserData(MEMBER_REFERENCE_KEY) {
            val name = name ?: return@getOrPutUserData null
            val descriptor = when (this) {
                is PsiField -> ClassUtil.getBinaryPresentation(type)
                is PsiMethod -> ClassUtil.getAsmMethodSignature(this)
                else -> return@getOrPutUserData null
            }
            MemberReference(name, descriptor.toDescriptor())
        }

        operator fun invoke(name: String, descriptor: String) = MemberReference(name, descriptor.toDescriptor())
    }

    override fun toString() = if (descriptor.isMethod) {
        "$name$descriptor"
    } else {
        "$name:$descriptor"
    }
}

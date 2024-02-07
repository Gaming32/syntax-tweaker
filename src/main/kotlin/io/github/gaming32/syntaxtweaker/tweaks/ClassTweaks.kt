package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.data.MemberReference.Companion.toMemberReference
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf

class ClassTweaks(
    val className: String,
    val classTweaks: TweakList,
    val memberTweaks: Map<MemberReference, TweakList>
) : SyntaxTweak {
    companion object {
        private val SUPPORTED_REFERENCE_TYPES = enumSetOf(
            SyntaxTweak.ReferenceType.CLASS,
            SyntaxTweak.ReferenceType.FIELD,
            SyntaxTweak.ReferenceType.METHOD
        )
    }

    override val supportedReferenceTypes get() = SUPPORTED_REFERENCE_TYPES

    override fun applyClassReference(reference: PsiElement, clazz: PsiClass, target: TweakTarget) {
        if (clazz.qualifiedName != className) return
        classTweaks.applyClassReference(reference, clazz, target)
    }

    override fun applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) {
        if (field.containingClass?.qualifiedName != className) return
        memberTweaks[field.toMemberReference()]?.applyFieldReference(reference, field, target)
    }

    override fun applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) {
        if (method.containingClass?.qualifiedName != className) return
        memberTweaks[method.toMemberReference()]?.applyMethodReference(reference, method, target)
    }
}

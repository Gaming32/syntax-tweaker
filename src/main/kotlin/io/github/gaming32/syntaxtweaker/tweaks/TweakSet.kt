package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.util.fullEnumSet
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMember
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement

data class TweakSet(
    val packages: Map<String, TweakList>,
    val classes: Map<String, ClassTweaks>
) : SyntaxTweak {
    companion object {
        private val SUPPORTED_REFERENCE_TYPES = fullEnumSet<SyntaxTweak.ReferenceType>()
    }

    override val supportedReferenceTypes get() = SUPPORTED_REFERENCE_TYPES

    override fun applyPackageReference(reference: PsiElement, pkg: PsiPackage, target: TweakTarget) {
        packages[pkg.qualifiedName]?.applyPackageReference(reference, pkg, target)
    }

    override fun applyClassReference(reference: PsiElement, clazz: PsiClass, target: TweakTarget) {
        classes[clazz.qualifiedName]?.applyClassReference(reference, clazz, target)
    }

    override fun applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) {
        getMemberClass(field)?.applyFieldReference(reference, field, target)
    }

    override fun applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) {
        getMemberClass(method)?.applyMethodReference(reference, method, target)
    }

    private fun getMemberClass(member: PsiMember) = classes[member.containingClass?.qualifiedName]
}

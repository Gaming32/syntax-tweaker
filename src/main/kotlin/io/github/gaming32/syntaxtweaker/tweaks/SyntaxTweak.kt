package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.TweakTarget
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement

interface SyntaxTweak {
    enum class ReferenceType {
        PACKAGE, CLASS, FIELD, METHOD;

        val pretty = name.lowercase()

        override fun toString() = pretty
    }

    val id: String? get() = null

    val supportedReferenceTypes: Set<ReferenceType>

    fun applyPackageReference(reference: PsiElement, pkg: PsiPackage, target: TweakTarget) =
        notImplemented(ReferenceType.PACKAGE)

    fun applyClassReference(reference: PsiElement, clazz: PsiClass, target: TweakTarget) =
        notImplemented(ReferenceType.CLASS)

    fun applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) =
        notImplemented(ReferenceType.FIELD)

    fun applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) =
        notImplemented(ReferenceType.METHOD)

    fun serializeArgs(): List<String> = listOf()

    fun serializeNamedArgs(): Map<String, String> = mapOf()

    private fun notImplemented(type: ReferenceType) {
        if (type !in supportedReferenceTypes) {
            throw UnsupportedOperationException("$this doesn't support $type reference tweaking")
        }
        throw IllegalStateException("$this claims it supports $type reference tweaking, but it's unimplemented")
    }
}

package io.github.gaming32.syntaxtweaker.tweaks

import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import io.github.gaming32.syntaxtweaker.TweakTarget
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement

interface SyntaxTweak {
    // TODO: Figure out what this should do
    fun applyClassReference(reference: PsiElement, clazz: PsiClass, target: TweakTarget) {
        throw IllegalStateException("$this doesn't support class reference tweaking")
    }

    fun applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) {
        throw IllegalStateException("$this doesn't support field reference tweaking")
    }

    fun applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) {
        throw IllegalStateException("$this doesn't support method reference tweaking")
    }

    fun serialize(): List<String> = listOf()
}

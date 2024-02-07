package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.TweakTarget
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement

typealias TweakList = List<SyntaxTweak>

fun TweakList.applyPackageReference(reference: PsiElement, pkg: PsiPackage, target: TweakTarget) {
    forEach { it.applyPackageReference(reference, pkg, target) }
}

fun TweakList.applyClassReference(reference: PsiElement, clazz: PsiClass, target: TweakTarget) {
    forEach { it.applyClassReference(reference, clazz, target) }
}

fun TweakList.applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) {
    forEach { it.applyFieldReference(reference, field, target) }
}

fun TweakList.applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) {
    forEach { it.applyMethodReference(reference, method, target) }
}

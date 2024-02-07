package io.github.gaming32.syntaxtweaker

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

interface TweakTarget {
    fun replace(element: PsiElement, with: String) = replace(element.textRange, with)

    fun replace(range: TextRange, with: String) = replace(range) { with }

    fun replace(element: PsiElement, with: () -> String) = replace(element.textRange, with)

    fun replace(range: TextRange, with: () -> String)

    fun canReplace(element: PsiElement) = canReplace(element.textRange)

    fun canReplace(range: TextRange): Boolean
}

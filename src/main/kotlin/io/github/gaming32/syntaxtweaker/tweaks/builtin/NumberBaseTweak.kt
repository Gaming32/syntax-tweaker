package io.github.gaming32.syntaxtweaker.tweaks.builtin

import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.data.MemberReference.Companion.toMemberReference
import io.github.gaming32.syntaxtweaker.data.Type
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweakParser
import org.jetbrains.kotlin.com.intellij.lang.jvm.JvmModifier
import org.jetbrains.kotlin.com.intellij.psi.JavaRecursiveElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.JavaTokenType
import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiCodeBlock
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiLiteralExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiLocalVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.com.intellij.psi.PsiPrefixExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class NumberBaseTweak(
    val member: MemberReference,
    val targetBase: NumberBase,
    val targetVariables: Boolean,
    val parameter: Int? = null
) : SyntaxTweak {
    enum class NumberBase(val prefix: String, val altPrefix: String?, val radix: Int, val useMinus: Boolean) {
        HEX("0x", "0X", 16, false),
        BIN("0b", "0B", 2, false),
        OCT("0", null, 8, true),
        DEC("", null, 10, true),
    }

    companion object : TweakParser<NumberBaseTweak> {
        const val ID = "number-base"

        private val SUPPORTED_REFERENCE_TYPES = enumSetOf(
            SyntaxTweak.ReferenceType.FIELD,
            SyntaxTweak.ReferenceType.METHOD
        )

        override fun TweakParser.ParseContext.parse(): NumberBaseTweak {
            if (args.isEmpty()) {
                throw IllegalArgumentException("Missing targetBase")
            }
            val targetBase = NumberBase.valueOf(args[0].uppercase())
            if (args.size < 2) {
                throw IllegalArgumentException("Missing targetVariables")
            }
            val targetVariables = args[1].toBooleanStrict()
            val parameter = when (referenceType) {
                SyntaxTweak.ReferenceType.FIELD -> null
                SyntaxTweak.ReferenceType.METHOD -> {
                    if (args.size < 3) {
                        throw IllegalArgumentException("Parameter index required when targeting a method")
                    }
                    args[2].toInt()
                }
                else -> throw IllegalArgumentException("Only applicable to members")
            }
            member!!
            if (parameter != null && parameter >= (member.type as Type.MethodType).parameters.size) {
                throw IllegalArgumentException(
                    "Parameter $parameter out of bounds for ${member.type}"
                )
            }
            return NumberBaseTweak(member, targetBase, targetVariables, parameter)
        }
    }

    override val supportedReferenceTypes get() = SUPPORTED_REFERENCE_TYPES

    init {
        if (member.type is Type.MethodType) {
            if (parameter == null) {
                throw IllegalArgumentException("$member is a method, but parameter is null")
            }
        } else if (parameter != null) {
            throw IllegalArgumentException("$member is a field, but parameter is $parameter")
        }
    }

    override fun applyFieldReference(reference: PsiQualifiedReferenceElement, field: PsiField, target: TweakTarget) {
        if (field.toMemberReference() != member) return
        val assignment = reference.parent as? PsiAssignmentExpression ?: return
        if (assignment.operationTokenType != JavaTokenType.EQ) return
        applyExpr(assignment.rExpression ?: return, target)
    }

    override fun applyMethodReference(reference: PsiQualifiedReferenceElement, method: PsiMethod, target: TweakTarget) {
        if (method.toMemberReference() != member) return
        val call = reference.parent as? PsiMethodCallExpression ?: return
        applyExpr(call.argumentList.expressions.getOrNull(parameter!!) ?: return, target)
    }

    private fun applyExpr(expr: PsiExpression, target: TweakTarget) {
        if (expr is PsiReferenceExpression) {
            if (!targetVariables) return
            val variable = expr.resolve() as? PsiVariable ?: return
            if (variable !is PsiParameter && variable !is PsiLocalVariable) return
            variable.initializer?.let { applyExpr(it, target) }
            if (variable.hasModifier(JvmModifier.FINAL)) return
            val block = variable.parentsWithSelf.firstIsInstanceOrNull<PsiCodeBlock>() ?: return
            return block.accept(object : JavaRecursiveElementVisitor() {
                override fun visitReferenceExpression(reference: PsiReferenceExpression) {
                    if (reference == expr) {
                        // Prevent infinite recursion
                        return super.visitReferenceExpression(reference)
                    }
                    (reference.parent as? PsiAssignmentExpression)?.let { assignment ->
                        if (assignment.operationTokenType != JavaTokenType.EQ) return@let
                        if (reference.resolve() != variable) return@let
                        applyExpr(assignment.rExpression ?: return@let, target)
                    }
                    super.visitReferenceExpression(reference)
                }
            })
        }
        var realExpr = expr
        var text = realExpr.text
        val value = when (realExpr) {
            is PsiLiteralExpression -> realExpr.value
            is PsiPrefixExpression -> when (realExpr.operationTokenType) {
                JavaTokenType.PLUS -> {
                    realExpr = realExpr.operand ?: return
                    (realExpr as? PsiLiteralExpression)?.value ?: return
                }
                JavaTokenType.MINUS -> {
                    if (realExpr.operand is PsiLiteralExpression) {
                        val operand = realExpr.operand as? PsiLiteralExpression ?: return
                        text = operand.text
                        when (val checkedValue = operand.value) {
                            is Int -> -checkedValue
                            is Long -> -checkedValue
                            else -> return
                        }
                    } else {
                        return
                    }
                }
                else -> return
            }
            else -> return
        }
        if (value !is Int && value !is Long) return
        val initialBase = NumberBase.entries.first {
            text.startsWith(it.prefix) || (it.altPrefix != null && text.startsWith(it.altPrefix))
        }
        if (initialBase == targetBase || !target.canReplace(realExpr)) return
        when (value) {
            is Int -> {
                if (value < 0 && !targetBase.useMinus) {
                    target.replace(realExpr, targetBase.prefix + value.toUInt().toString(targetBase.radix))
                } else {
                    target.replace(realExpr, targetBase.prefix + value.toString(targetBase.radix))
                }
            }
            is Long -> {
                if (value < 0L && !targetBase.useMinus) {
                    target.replace(realExpr, targetBase.prefix + value.toULong().toString(targetBase.radix))
                } else {
                    target.replace(realExpr, targetBase.prefix + value.toString(targetBase.radix))
                }
            }
        }
    }

    override fun serialize() = buildList {
        add(targetBase.name.lowercase())
        if (parameter != null) {
            add(parameter.toString())
        }
    }
}

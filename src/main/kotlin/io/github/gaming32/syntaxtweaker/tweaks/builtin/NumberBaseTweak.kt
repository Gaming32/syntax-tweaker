package io.github.gaming32.syntaxtweaker.tweaks.builtin

import org.jetbrains.kotlin.com.intellij.psi.JavaTokenType
import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiLiteralExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiPrefixExpression
import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.data.MemberReference.Companion.toMemberReference
import io.github.gaming32.syntaxtweaker.tweaks.ParseContext
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.TweakParser
import org.jetbrains.kotlin.com.intellij.psi.PsiQualifiedReferenceElement

class NumberBaseTweak(
    val targetBase: NumberBase,
    val member: MemberReference,
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

        override fun ParseContext.parse(): NumberBaseTweak {
            if (args.isEmpty()) {
                throw IllegalArgumentException("Missing targetBase")
            }
            val base = NumberBase.valueOf(args[0].uppercase())
            val parameter = when (referenceType) {
                ParseContext.ReferenceType.FIELD -> null
                ParseContext.ReferenceType.METHOD -> {
                    if (args.size < 2) {
                        throw IllegalArgumentException("$ID needs a parameter index when targeting a method")
                    }
                    args[1].toInt()
                }
                else -> throw IllegalArgumentException("$ID is only applicable to members")
            }
            if (parameter != null && parameter >= member!!.descriptor.getArgumentCount()) {
                throw IllegalArgumentException(
                    "Parameter $parameter out of bounds for descriptor ${member.descriptor}"
                )
            }
            return NumberBaseTweak(base, member!!, parameter)
        }
    }

    init {
        if (member.descriptor.isMethod) {
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

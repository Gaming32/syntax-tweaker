package io.github.gaming32.syntaxtweaker.util

import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolder
import java.io.Writer
import java.util.*
import kotlin.collections.plus
import kotlin.collections.plus as plusNotNull

inline fun <T : Any> UserDataHolder.getOrPutUserData(key: Key<T>, crossinline calc: () -> T?): T? {
    getUserData(key)?.let { return it }
    val value = calc()
    putUserData(key, value)
    return value
}

inline fun <reified T : Enum<T>> emptyEnumSet(): EnumSet<T> = EnumSet.noneOf(T::class.java)

inline fun <reified T : Enum<T>> fullEnumSet(): EnumSet<T> = EnumSet.allOf(T::class.java)

operator fun <T> List<T>?.plus(other: List<T>) = this?.plusNotNull(other) ?: other

operator fun ClassTweaks?.plus(other: ClassTweaks): ClassTweaks {
    if (this == null) {
        return other
    }
    val mergedMembers = memberTweaks.toMutableMap()
    for ((member, tweaks) in other.memberTweaks) {
        mergedMembers[member] += tweaks
    }
    return ClassTweaks(other.className, classTweaks + other.classTweaks, mergedMembers)
}

operator fun TweakSet?.plus(other: TweakSet): TweakSet {
    if (this == null) {
        return other
    }
    val mergedPackages = packages.toMutableMap()
    for ((pkg, tweaks) in other.packages) {
        mergedPackages[pkg] += tweaks
    }
    val mergedClasses = classes.toMutableMap()
    for ((clazz, tweaks) in other.classes) {
        mergedClasses[clazz] += tweaks
    }
    return TweakSet(packages, classes, metadata + other.metadata)
}

fun Appendable.asWriter() = this as? Writer ?: object : Writer() {
    val currentWrite = object : CharSequence {
        var chars: CharArray? = null

        override val length get() = chars!!.size

        override fun get(index: Int) = chars!![index]

        override fun subSequence(startIndex: Int, endIndex: Int) = String(chars!!, startIndex, endIndex - startIndex)
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        currentWrite.chars = cbuf
        this@asWriter.append(currentWrite, off, off + len)
    }

    override fun write(c: Int) {
        this@asWriter.append(c.toChar())
    }

    override fun write(str: String, off: Int, len: Int) {
        this@asWriter.append(str, off, off + len)
    }

    override fun flush() = Unit

    override fun close() = Unit
}

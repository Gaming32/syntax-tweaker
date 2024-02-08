package io.github.gaming32.syntaxtweaker.util

import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolder
import java.util.*
import kotlin.collections.plus as plusNotNull

inline fun <T : Any> UserDataHolder.getOrPutUserData(key: Key<T>, crossinline calc: () -> T?): T? {
    getUserData(key)?.let { return it }
    val value = calc()
    putUserData(key, value)
    return value
}

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
    return ClassTweaks(
        other.className, classTweaks + other.classTweaks, mergedMembers
    )
}

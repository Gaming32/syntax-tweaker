package io.github.gaming32.syntaxtweaker.util

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolder
import java.util.*

inline fun <T : Any> UserDataHolder.getOrPutUserData(key: Key<T>, crossinline calc: () -> T?): T? {
    getUserData(key)?.let { return it }
    val value = calc()
    putUserData(key, value)
    return value
}

inline fun <reified T : Enum<T>> fullEnumSet(): EnumSet<T> = EnumSet.allOf(T::class.java)

package io.github.gaming32.syntaxtweaker.util

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolder

inline fun <T : Any> UserDataHolder.getOrPutUserData(key: Key<T>, crossinline calc: () -> T?): T? {
    getUserData(key)?.let { return it }
    val value = calc()
    putUserData(key, value)
    return value
}

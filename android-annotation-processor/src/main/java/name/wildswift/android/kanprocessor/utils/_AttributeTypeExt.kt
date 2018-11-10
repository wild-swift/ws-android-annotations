package name.wildswift.android.kanprocessor.utils

import com.squareup.kotlinpoet.asTypeName
import name.wildswift.android.kannotations.AttributeType
import name.wildswift.android.kannotations.AttributeType.*

/**
 * Created by swift
 */
fun AttributeType.fieldClass() = when(this) {
    string -> String::class
    color -> Int::class
    enum_ -> Int::class
}

fun AttributeType.initValue() = when(this) {
    string -> "\"\""
    color -> "0xFFFFFFFF.toInt()"
    enum_ -> "0"
}

fun AttributeType.loadCode(indexRef: String) = when(this) {
    string -> "getString($indexRef)"
    color -> "getColor($indexRef, ${initValue()})"
    enum_ -> "getInteger($indexRef, 0)"
}

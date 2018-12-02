/*
 * Copyright (C) 2018 Wild Swift
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.wildswift.android.kanprocessor.utils

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.type.MirroredTypeException
import kotlin.reflect.KClass

/**
 * Created by swift
 */
fun <T : Annotation> T.safeGetType(run: T.() -> KClass<*>) = try {
    this.run().asTypeName()
} catch (mte: MirroredTypeException) {
    mte.typeMirror.asTypeName()
}

fun TypeName.bundleStoreMethod(key: String, value: String) = when {
    this == Byte::class.asTypeName() -> "putByte(\"$key\", $value)"
    this == Char::class.asTypeName() -> "putChar(\"$key\", $value)"
    this == Short::class.asTypeName() -> "putShort(\"$key\", $value)"
    this == Float::class.asTypeName() -> "putFloat(\"$key\", $value)"
    this == Boolean::class.asTypeName() -> "putBoolean(\"$key\", $value)"
    this == Int::class.asTypeName() -> "putInt(\"$key\", $value)"
    this == Long::class.asTypeName() -> "putLong(\"$key\", $value)"
    this == Double::class.asTypeName() -> "putDouble(\"$key\", $value)"
    this == String::class.asTypeName() -> "putString(\"$key\", $value)"
    this == BooleanArray::class.asTypeName() -> "putBooleanArray(\"$key\", $value)"
    this == ByteArray::class.asTypeName() -> "putByteArray(\"$key\", $value)"
    this == ShortArray::class.asTypeName() -> "putShortArray(\"$key\", $value)"
    this == CharArray::class.asTypeName() -> "putCharArray(\"$key\", $value)"
    this == FloatArray::class.asTypeName() -> "putFloatArray(\"$key\", $value)"
    this == IntArray::class.asTypeName() -> "putIntArray(\"$key\", $value)"
    this == LongArray::class.asTypeName() -> "putLongArray(\"$key\", $value)"
    this == DoubleArray::class.asTypeName() -> "putDoubleArray(\"$key\", $value)"
    this == bundleClass -> "putBundle(\"$key\", $value)"
    else -> "put(\"$key\", $value)"
}
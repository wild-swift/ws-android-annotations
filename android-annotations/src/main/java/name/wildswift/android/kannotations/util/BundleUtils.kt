/*
 * Copyright (C) 2020 Wild Swift
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

package name.wildswift.android.kannotations.util

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
        /**
         * Created by swift
         */
fun Bundle.put(key: String, value: Any?) {
    when (value) {
        is Byte -> putByte(key, value)
        is Char -> putChar(key, value)
        is Short -> putShort(key, value)
        is Float -> putFloat(key, value)
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is Double -> putDouble(key, value)
        is String -> putString(key, value)
        is Bundle -> putBundle(key, value)
        is BooleanArray -> putBooleanArray(key, value)
        is ByteArray -> putByteArray(key, value)
        is ShortArray -> putShortArray(key, value)
        is CharArray -> putCharArray(key, value)
        is FloatArray -> putFloatArray(key, value)
        is IntArray -> putIntArray(key, value)
        is LongArray -> putLongArray(key, value)
        is DoubleArray -> putDoubleArray(key, value)
        is CharSequence -> putCharSequence(key, value)
        is SparseArray<*> -> putSparseParcelableArray(key, value as SparseArray<out Parcelable>?)
        is Array<*> -> if (value.isEmpty()) {
            putParcelableArray(key, arrayOf())
        } else {
            when {
                value.first() is String -> putStringArray(key, value as Array<out String>?)
                value.first() is CharSequence -> putCharSequenceArray(key, value as Array<out CharSequence>?)
                else -> putParcelableArray(key, value as Array<out Parcelable>?)
            }
        }
        is List<*> -> if (value.isEmpty()) {
            putParcelableArrayList(key, arrayListOf())
        } else {
            when {
                value.first() is Int -> putIntegerArrayList(key, ArrayList(value as List<Int>))
                value.first() is String -> putStringArrayList(key, ArrayList(value as List<String>))
                value.first() is CharSequence -> putCharSequenceArrayList(key, ArrayList(value as List<CharSequence>))
                else -> putParcelableArrayList(key, ArrayList(value as List<Parcelable>))
            }
        }
        is Parcelable -> putParcelable(key, value)
        is Serializable -> putSerializable(key, value)
    }
}
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

package name.wildswift.android.kanprocessor.utils

import com.squareup.kotlinpoet.ClassName
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kannotations.ViewProperty

/**
 * Created by swift
 */
fun ViewField.resolveSetter(field: String) =
        if (property != ViewProperty.none) {
            when (property) {
                ViewProperty.none -> ""
                ViewProperty.text -> "setText($field)"
                ViewProperty.visibility -> "visibility = $field"
                ViewProperty.textColor -> "setTextColor($field)"
                ViewProperty.checked -> "isChecked = $field"
                ViewProperty.timePickerHour -> "hour = $field"
                ViewProperty.timePickerMinute -> "minute = $field"
            }
        } else {
            if (childPropertyName.isNotEmpty()) {
                "$childPropertyName = $field"
            } else {
                "$childPropertySetter($field)"
            }
        }

fun ViewField.validateCorrectSetup(): Boolean {
    if (property == ViewProperty.none && safeGetType { type }.let { it is ClassName && it.canonicalName == "java.lang.Object" }) return false
    if (property == ViewProperty.none && resolveDefaultValue().isEmpty()) return false
    if (property == ViewProperty.none && childName.isNotEmpty() && childPropertyName.isEmpty() && childPropertySetter.isEmpty()) return false

    return true
}

fun ViewField.resolveDefaultValue(): String {
    if (defaultValue.isNotBlank()) return defaultValue
    val type = safeGetType { type }
    if (type !is ClassName) return defaultValue
    return when (type.canonicalName) {
        Boolean::class.qualifiedName -> "false"
        Float::class.qualifiedName -> "0.0f"
        Double::class.qualifiedName -> "0.0"
        Int::class.qualifiedName -> "0"
        Long::class.qualifiedName -> "0L"
        Short::class.qualifiedName -> "0"
        Byte::class.qualifiedName -> "0"
        String::class.qualifiedName -> "\"\""
        else -> defaultValue
    }
}

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
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData

/**
 * Created by swift
 */
fun ViewField.validateCorrectSetup(): Boolean {
    if (byProperty == ViewProperty.none && checkIsVoid { type } && checkIsVoid { byDelegate }) return false
    if (checkIsVoid { byDelegate } && (resolveDefaultValue(mapOf()).first).isEmpty()) return false
    if (byProperty == ViewProperty.none && checkIsVoid { byDelegate } && childName.isNotEmpty() && childPropertyName.isEmpty() && childPropertySetter.isEmpty()) return false

    return true
}

fun ViewField.resolveType(typeMapping: Map<String, ViewWithDelegateGenerationData>) =
        when {
            byProperty == ViewProperty.text -> String::class.asTypeName()
            byProperty == ViewProperty.visibility -> Int::class.asTypeName()
            byProperty == ViewProperty.textColor -> Int::class.asTypeName()
            byProperty == ViewProperty.checked -> Boolean::class.asTypeName()
            byProperty == ViewProperty.timePickerHour -> Int::class.asTypeName()
            byProperty == ViewProperty.timePickerMinute -> Int::class.asTypeName()
            byProperty == ViewProperty.imageResource -> Int::class.asTypeName()
            byProperty == ViewProperty.imageDrawable -> drawableClass.copy(nullable = true)
            byProperty == ViewProperty.backgroundResource -> Int::class.asTypeName()
            byProperty == ViewProperty.backgroundColor -> Int::class.asTypeName()
            byProperty == ViewProperty.backgroundDrawable -> drawableClass.copy(nullable = true)
            byProperty == ViewProperty.radioSelect -> INT.copy(nullable = true)
            !checkIsVoid { byDelegate } -> typeMapping[(safeGetType { byDelegate } as? ClassName)?.canonicalName]?.externalModelType
                    ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { byDelegate }}")
            else -> safeGetType { type }
        }

fun ViewField.resolveDefaultValue(typeMapping: Map<String, ViewWithDelegateGenerationData>): Pair<String, TypeName?> {
    if (byProperty != ViewProperty.none) return byProperty.getDefaultValue() to null
    if (!checkIsVoid { byDelegate }) return "%T()" to (typeMapping[(safeGetType { byDelegate } as? ClassName)?.canonicalName]?.externalModelType
            ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { byDelegate }}"))
    if (defaultValue.isNotBlank()) return defaultValue to null
    val type = safeGetType { type }
    if (type !is ClassName) return defaultValue to null
    return when (type.canonicalName) {
        Boolean::class.qualifiedName -> "false" to null
        Float::class.qualifiedName -> "0.0f" to null
        Double::class.qualifiedName -> "0.0" to null
        Int::class.qualifiedName -> "0" to null
        Long::class.qualifiedName -> "0L" to null
        Short::class.qualifiedName -> "0" to null
        Byte::class.qualifiedName -> "0" to null
        String::class.qualifiedName -> "\"\"" to null
        else -> defaultValue to null
    }
}

fun ViewField.resolveSetter(field: String) =
        when {
            byProperty == ViewProperty.text -> "apply·{·if·(text.toString()·!=·$field)·setText($field)·}"
            byProperty == ViewProperty.visibility -> "visibility = $field"
            byProperty == ViewProperty.textColor -> "setTextColor($field)"
            byProperty == ViewProperty.checked -> "isChecked = $field"
            byProperty == ViewProperty.timePickerHour -> "hour = $field"
            byProperty == ViewProperty.timePickerMinute -> "minute = $field"
            byProperty == ViewProperty.imageResource -> "setImageResource($field)"
            byProperty == ViewProperty.imageDrawable -> "setImageDrawable($field)"
            byProperty == ViewProperty.backgroundResource -> "setBackgroundResource($field)"
            byProperty == ViewProperty.backgroundColor -> "setBackgroundColor($field)"
            byProperty == ViewProperty.backgroundDrawable -> "setBackground($field)"
            byProperty == ViewProperty.radioSelect -> "apply·{·if·($field·!=·null)·check($field)·else·clearCheck()·}"
            !checkIsVoid { byDelegate } -> "viewModel = $field"
            childPropertyName.isNotEmpty() -> "$childPropertyName = $field"
            else -> "$childPropertySetter($field)"
        }

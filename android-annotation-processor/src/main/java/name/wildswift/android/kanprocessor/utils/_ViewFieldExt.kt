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

import com.squareup.kotlinpoet.*
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import java.util.*
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Elements

/**
 * Created by swift
 */
fun ViewField.validateCorrectSetup(elements: Elements): Boolean {
    if (byProperty == ViewProperty.none && checkIsVoid { type } && checkIsVoid { byDelegate }) return false
    if (checkIsVoid { byDelegate } && (resolveDefaultValue(mapOf(), elements).first).isEmpty()) return false
    if (byProperty == ViewProperty.none && checkIsVoid { byDelegate } && childName.isNotEmpty() && childPropertyName.isEmpty() && childPropertySetter.isEmpty()) return false

    return true
}

fun ViewField.resolveType(typeMapping: Map<String, ViewWithDelegateGenerationData>) =
        when {
            byProperty == ViewProperty.text -> STRING
            byProperty == ViewProperty.visibility -> INT
            byProperty == ViewProperty.textColor -> INT
            byProperty == ViewProperty.checked -> BOOLEAN
            byProperty == ViewProperty.timePickerHour -> INT
            byProperty == ViewProperty.timePickerMinute -> INT
            byProperty == ViewProperty.imageResource -> INT
            byProperty == ViewProperty.imageDrawable -> drawableClass.copy(nullable = true)
            byProperty == ViewProperty.backgroundResource -> INT
            byProperty == ViewProperty.backgroundColor -> INT
            byProperty == ViewProperty.backgroundDrawable -> drawableClass.copy(nullable = true)
            byProperty == ViewProperty.radioSelect -> INT.copy(nullable = true)
            byProperty == ViewProperty.alpha -> FLOAT
            byProperty == ViewProperty.enable -> BOOLEAN
            byProperty == ViewProperty.selected -> BOOLEAN
            byProperty == ViewProperty.elevation -> FLOAT
            !checkIsVoid { byDelegate } -> typeMapping[(safeGetType { byDelegate } as? ClassName)?.canonicalName]?.externalModelType
                    ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { byDelegate }}")
            else -> safeGetType { type }
        }

fun ViewField.resolveDefaultValue(typeMapping: Map<String, ViewWithDelegateGenerationData>, elements: Elements): Pair<String, TypeName?> {
    if (byProperty != ViewProperty.none) return byProperty.getDefaultValue()
    if (!checkIsVoid { byDelegate }) return "%T()" to (typeMapping[(safeGetType { byDelegate } as? ClassName)?.canonicalName]?.externalModelType
            ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { byDelegate }}"))
    if (defaultValue.isNotBlank()) return defaultValue to null
    val type = safeGetType { type }
    if (type !is ClassName) return defaultValue to null
    val typeElement = elements.getTypeElement(type.canonicalName)
    if (typeElement?.kind == ElementKind.ENUM) {
        typeElement.enclosedElements
                .firstOrNull { it.kind == ElementKind.ENUM_CONSTANT }
                ?.simpleName
                ?.also {
                    return "%T.$it" to type
                }
    }
    return when (type.canonicalName) {
        Boolean::class.qualifiedName -> "false" to null
        Float::class.qualifiedName -> "0.0f" to null
        Double::class.qualifiedName -> "0.0" to null
        Int::class.qualifiedName -> "0" to null
        Long::class.qualifiedName -> "0L" to null
        Short::class.qualifiedName -> "0" to null
        Byte::class.qualifiedName -> "0" to null
        String::class.qualifiedName -> "\"\"" to null
        Date::class.qualifiedName -> "%T()" to Date::class.asClassName()
        else -> defaultValue to null
    }
}

fun ViewField.resolveSetter(childName: String, field: String) =
        when {
            byProperty == ViewProperty.text -> "$childName.apply·{·if·(text.toString()·!=·$field)·setText($field)·}"
            byProperty == ViewProperty.visibility -> "$childName.visibility·=·$field"
            byProperty == ViewProperty.textColor -> "$childName.setTextColor($field)"
            byProperty == ViewProperty.checked -> "$childName.isChecked·=·$field"
            byProperty == ViewProperty.timePickerHour -> "$childName.hour·=·$field"
            byProperty == ViewProperty.timePickerMinute -> "$childName.minute·=·$field"
            byProperty == ViewProperty.imageResource -> "$childName.setImageResource($field)"
            byProperty == ViewProperty.imageDrawable -> "$childName.setImageDrawable($field)"
            byProperty == ViewProperty.backgroundResource -> "$childName.setBackgroundResource($field)"
            byProperty == ViewProperty.backgroundColor -> "$childName.setBackgroundColor($field)"
            byProperty == ViewProperty.backgroundDrawable -> "$childName.setBackground($field)"
            byProperty == ViewProperty.radioSelect -> "$childName.apply·{·if·($field·!=·null)·check($field)·else·clearCheck()·}"
            byProperty == ViewProperty.alpha -> "$childName.alpha·=·$field"
            byProperty == ViewProperty.enable -> "$childName.isEnabled·=·$field"
            byProperty == ViewProperty.selected -> "$childName.isSelected·=·$field"
            byProperty == ViewProperty.elevation -> "if·(android.os.Build.VERSION.SDK_INT·>=·21)·$childName.elevation·=·$field"
            !checkIsVoid { byDelegate } -> "$childName.viewModel·=·$field"
            childPropertyName.isNotEmpty() -> "$childName.$childPropertyName·=·$field"
            else -> "$childName.$childPropertySetter($field)"
        }

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
            }
        } else {
            if (propertyName.isNotEmpty()) {
                "$propertyName = $field"
            } else {
                "$propertySetter($field)"
            }
        }

fun ViewField.validateCorrectSetup(): Boolean {
    if (property == ViewProperty.none && safeGetType { type }.let { it is ClassName && it.canonicalName == "java.lang.Object" }) return false
    if (property == ViewProperty.none && defaultValue.isEmpty()) return false
    if (property == ViewProperty.none && childName.isNotEmpty() && propertyName.isEmpty() && propertySetter.isEmpty()) return false

    return true;
}
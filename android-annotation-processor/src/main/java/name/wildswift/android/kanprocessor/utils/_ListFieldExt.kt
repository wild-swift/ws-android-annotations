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
import com.squareup.kotlinpoet.TypeName
import name.wildswift.android.kannotations.ListViewField
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData

/**
 * Created by swift
 */
fun ListViewField.validateCorrectSetup(): Boolean {
    if (childListView.isNotEmpty() && checkIsVoid { delegateClass } && checkIsVoid { elementType }) return false

    return true
}

fun ListViewField.getModelType(typeMapping: Map<String, ViewWithDelegateGenerationData>): TypeName {
    if (!checkIsVoid { delegateClass }) return typeMapping[(safeGetType { delegateClass } as? ClassName)?.canonicalName]?.externalModelType
            ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { elementType }}")
    return safeGetType { elementType }
}

fun ListViewField.getAdapterViewType(typeMapping: Map<String, ViewWithDelegateGenerationData>): TypeName {
    if (!checkIsVoid { delegateClass }) return typeMapping[(safeGetType { delegateClass } as? ClassName)?.canonicalName]?.generateViewType
            ?: throw IllegalStateException("Can't find model for delegate ${safeGetType { elementType }}")
    return safeGetType { viewForElementClass }
}

fun ListViewField.buildSetViewModelStatement(typeMapping: Map<String, ViewWithDelegateGenerationData>, value: String): String {
    if (!checkIsVoid { delegateClass }) return "viewModel = $value"
    if (modelSetterName.isNotEmpty()) return "$modelSetterName($value)"
    return "$modelFieldName = $value"
}
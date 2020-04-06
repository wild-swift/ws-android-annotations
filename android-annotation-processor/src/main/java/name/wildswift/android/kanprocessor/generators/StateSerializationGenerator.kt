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

package name.wildswift.android.kanprocessor.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import name.wildswift.android.kanprocessor.utils.bundleClass
import name.wildswift.android.kanprocessor.utils.bundleStoreMethod

object StateSerializationGenerator {
    fun buildSerializeStateMethod(internalProperties: List<PropertyData>, internalModelProperty: PropertySpec?): FunSpec =
            FunSpec
                    .builder("getState")
                    .addModifiers(KModifier.PRIVATE)
                    .returns(bundleClass)
                    .addStatement("val result = %T()", bundleClass)
                    .apply {
                        internalProperties
                                .forEach {
                                    addStatement("result.${it.type.bundleStoreMethod(it.name, "%N.${it.name}")}", internalModelProperty!!)
                                }
                    }
                    .addStatement("return result")
                    .build()

    fun buildDeserializeStateMethod(data: ViewWithDelegateGenerationData, internalModelProperty: PropertySpec?, internalProperties: List<PropertyData>): FunSpec =
            FunSpec
                    .builder("setState")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("state", bundleClass)
                    .addStatement("%1N = %2T(", internalModelProperty!!, data.internalModelType)
                    .apply {
                        internalProperties
                                .dropLast(1)
                                .forEach {
                                    addStatement("    ${it.name} = state.get(\"${it.name}\") as %T,", it.type)
                                }
                        internalProperties
                                .last()
                                .also {
                                    addStatement("    ${it.name} = state.get(\"${it.name}\") as %T", it.type)
                                }
                    }
                    .addStatement(")")
                    .build()

}
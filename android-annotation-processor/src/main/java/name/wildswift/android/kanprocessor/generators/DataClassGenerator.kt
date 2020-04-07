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

import com.squareup.kotlinpoet.*
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import java.io.File

object DataClassGenerator {
    fun generateDataClass(classType: ClassName, inputProperties: List<PropertyData>, generationPath: File): TypeSpec? {
        if (inputProperties.isEmpty()) return null
        val classSpec = TypeSpec
                .classBuilder(classType)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .let { builder ->
                                    inputProperties
                                            .forEach {
                                                builder
                                                        .addParameter(ParameterSpec.builder(it.name, it.type)
                                                                .defaultValue(it.defaultValuePattern, it.defaultValueClass)
                                                                .build())
                                            }
                                    builder
                                }
                                .build()
                )
                .let { builder ->
                    inputProperties.forEach { builder.addProperty(PropertySpec.builder(it.name, it.type).initializer(it.name).build()) }
                    builder
                }
                .build()

        FileSpec
                .builder(classType.packageName, classType.simpleNames.first())
                .addType(classSpec)
                .build()
                .writeTo(generationPath)
        return classSpec
    }

}
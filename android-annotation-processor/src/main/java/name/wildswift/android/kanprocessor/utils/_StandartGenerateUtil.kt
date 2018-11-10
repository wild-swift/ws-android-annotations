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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Created by swift
 */
fun TypeSpec.Builder.addViewConstructors(additionalCalls: FunSpec.Builder.(Int) -> FunSpec.Builder = { this }) =
        this
                .addFunction(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .callSuperConstructor("context")
                                .additionalCalls(1)
                                .build()
                )
                .addFunction(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .addParameter(ParameterSpec.builder("attrs", ClassName("android.util", "AttributeSet").asNullable()).build())
                                .callSuperConstructor("context", "attrs")
                                .additionalCalls(2)
                                .build()
                )
                .addFunction(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .addParameter(ParameterSpec.builder("attrs", ClassName("android.util", "AttributeSet").asNullable()).build())
                                .addParameter(ParameterSpec.builder("defStyleAttr", Int::class.java).build())
                                .callSuperConstructor("context", "attrs", "defStyleAttr")
                                .additionalCalls(3)
                                .build()
                )

val contextClass = ClassName("android.content", "Context")
val viewClass = ClassName("android.view", "View")
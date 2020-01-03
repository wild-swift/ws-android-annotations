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
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import java.io.File

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
                                .addParameter(ParameterSpec.builder("attrs", ClassName("android.util", "AttributeSet").copy(nullable = true)).build())
                                .callSuperConstructor("context", "attrs")
                                .additionalCalls(2)
                                .build()
                )
                .addFunction(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .addParameter(ParameterSpec.builder("attrs", ClassName("android.util", "AttributeSet").copy(nullable = true)).build())
                                .addParameter(ParameterSpec.builder("defStyleAttr", Int::class.java).build())
                                .callSuperConstructor("context", "attrs", "defStyleAttr")
                                .additionalCalls(3)
                                .build()
                )

val contextClass = ClassName("android.content", "Context")
val viewClass = ClassName("android.view", "View")
val parcelableClass = ClassName("android.os", "Parcelable")
val bundleClass = ClassName("android.os", "Bundle")

fun TypeSpec.Builder.delegateCall(methodName: String, delegateProperty: PropertySpec, delegatedName: String) = addFunction(
        FunSpec.builder(methodName)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.$methodName()")
                .addStatement("%1N.$delegatedName()", delegateProperty)
                .build()
)

fun generateDataClass(pack: String, className: String, inputProperties: List<PropertyData>, generationPath: File): Pair<ClassName, TypeSpec?> {
    val classType = ClassName(pack, className)
    val classSpec = TypeSpec
            .classBuilder(classType)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                    FunSpec.constructorBuilder()
                            .let { builder ->
                                inputProperties.forEach { builder.addParameter(ParameterSpec.builder(it.name, it.type).defaultValue(it.defaultValue).build()) }
                                builder
                            }
                            .build()
            )
            .let { builder ->
                inputProperties.forEach { builder.addProperty(PropertySpec.builder(it.name, it.type).initializer(it.name).build()) }
                builder
            }
            .build()
            .takeIf { inputProperties.isNotEmpty() }

    if (classSpec != null) {
        FileSpec
                .builder(pack, className)
                .addType(classSpec)
                .build()
                .writeTo(generationPath)
    }
    return classType to classSpec
}

fun TypeSpec.Builder.generateViewSave(getIntState: FunSpec) = FunSpec
        .builder("onSaveInstanceState")
        .returns(parcelableClass)
        .addModifiers(KModifier.OVERRIDE)
        .addStatement("val result = %T()", bundleClass)
        .addStatement("result.putParcelable(\"superState\", super.onSaveInstanceState())")
        .addStatement("result.putBundle(\"currentState\", %N())", getIntState)
        .addStatement("return result")
        .build()
        .let {
            this.addFunction(it)
            this
        }

fun TypeSpec.Builder.generateViewRestore(setIntState: FunSpec) = FunSpec
        .builder("onRestoreInstanceState")
        .addParameter(ParameterSpec.builder("state", parcelableClass.copy(nullable = true)).build())
        .addModifiers(KModifier.OVERRIDE)
        .addCode(CodeBlock.of("""
            |    (state as? %1T)?.also {
            |        it.classLoader = javaClass.classLoader
            |        super.onRestoreInstanceState(it.getParcelable("superState"))
            |        it.getBundle("currentState")?.also {
            |            it.classLoader = javaClass.classLoader
            |            %2N(it)
            |        }
            |    }
        """.trimMargin(), bundleClass, setIntState))
        .build()
        .let {
            this.addFunction(it)
            this
        }
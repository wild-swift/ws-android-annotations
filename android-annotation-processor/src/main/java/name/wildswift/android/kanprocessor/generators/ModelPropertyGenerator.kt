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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import name.wildswift.android.kannotations.FieldRWType
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kanprocessor.datahelpers.FieldMethodsGenerationMetadata
import name.wildswift.android.kanprocessor.datahelpers.ListFieldGenerationData
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import name.wildswift.android.kanprocessor.utils.*
import kotlin.properties.Delegates

object ModelPropertyGenerator {
    fun internalModelProperty(data: ViewWithDelegateGenerationData, delegateProperty: PropertySpec, listFieldsGenerationData: List<ListFieldGenerationData>): PropertySpec {
        return PropertySpec.builder("intModel", data.internalModelType)
                .mutable()
                .addModifiers(KModifier.PRIVATE)
                .delegate(
                        CodeBlock.builder()
                                .add("""
                                                    |%1T.observable(%2T())·{·_,·oldValue,·newValue·->
                                                    |··if (oldValue == newValue) return@observable
                                                    |
                                                    |
                                                """.trimMargin(), Delegates::class.asTypeName(), data.internalModelType)
                                .apply {
                                    data.basicFields
                                            .filter { it.childName.isNotEmpty() }
                                            .filter { it.rwType != FieldRWType.readOnly }
                                            .forEach {
                                                addStatement("··if·(oldValue.${it.name}·!=·newValue.${it.name})·${it.childName}.${it.resolveSetter("newValue.${it.name}")}")
                                            }
                                }
                                .apply {
                                    listFieldsGenerationData
                                            .filter { it.childListView.isNotEmpty() }
                                            .forEach {
                                                // TODO Need use typeName, but this is hard for now
                                                addStatement("··if·(oldValue.${it.name}·!=·newValue.${it.name})·${it.childListView}.setAdapter(${it.name.capitalize()}Adapter(context,·newValue.${it.name}))")
                                            }
                                }
                                .add("""
                                                    |
                                                    |  %N.onNewInternalState(newValue)
                                                    |}
                                                """.trimMargin(), delegateProperty)
                                .build()
                )
                .build()
    }

    fun buildFieldsSpecs(data: ViewWithDelegateGenerationData, internalModelProperty: PropertySpec?, delegateProperty: PropertySpec, processingTypeMap: Map<String, ViewWithDelegateGenerationData>): List<FieldMethodsGenerationMetadata> {
        val basicFields = data.basicFields
                .filter { it.publicAccess }
                .map { field ->
                    val fieldType = if (field.property != ViewProperty.none) field.property.getType() else field.safeGetType { type }
                    val onChangedListener =
                            if (field.rwType != FieldRWType.writeOnly) {
                                PropertySpec
                                        .builder("on${field.name.capitalize()}Changed", LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(fieldType)), returnType = Unit::class.asTypeName()).copy(nullable = true))
                                        .mutable()
                                        .initializer("null")
                                        .build()
                            } else {
                                null
                            }

                    val fieldProperty = PropertySpec
                            .builder(field.name, fieldType)
                            .getter(FunSpec.getterBuilder().addStatement("return %N.${field.name}", internalModelProperty!!).build())
                            .also {
                                if (field.rwType != FieldRWType.readOnly) {
                                    it.mutable().setter(
                                            FunSpec.setterBuilder()
                                                    .addParameter(ParameterSpec.builder("value", fieldType).build())
                                                    .addStatement("if (%1N.${field.name} == value) return", internalModelProperty)
                                                    .addStatement("%1N = %2N.validateStateForNewInput(%1N.copy(${field.name} = value))", internalModelProperty, delegateProperty)
                                                    .apply {
                                                        onChangedListener?.apply {
                                                            addStatement("%1N?.invoke(value)", this)
                                                        }
                                                    }
                                                    .build()
                                    )
                                }
                            }
                            .build()
                    FieldMethodsGenerationMetadata(name = field.name, readWriteProperty = fieldProperty, listenerProperty = onChangedListener)
                }

        val listFields = data.listFields
                .filter { it.isPublic }
                .map { field ->
                    val fieldType = itemsDSClass.parameterizedBy(field.getModelType(processingTypeMap))
                    val fieldProperty = PropertySpec
                            .builder(field.name, fieldType)
                            .getter(FunSpec.getterBuilder().addStatement("return %N.${field.name}", internalModelProperty!!).build())
                            .mutable()
                            .setter(
                                    FunSpec.setterBuilder()
                                            .addParameter(ParameterSpec.builder("value", fieldType).build())
                                            .addStatement("if (%1N.${field.name} == value) return", internalModelProperty)
                                            .addStatement("%1N = %2N.validateStateForNewInput(%1N.copy(${field.name} = value))", internalModelProperty, delegateProperty)
                                            .build()
                            )
                            .build()

                    FieldMethodsGenerationMetadata(name = field.name, readWriteProperty = fieldProperty, listenerProperty = null)
                }

        return basicFields + listFields
    }

}
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
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kanprocessor.datahelpers.FieldMethodsGenerationMetadata
import name.wildswift.android.kanprocessor.datahelpers.ListFieldGenerationData
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import name.wildswift.android.kanprocessor.utils.*
import kotlin.properties.Delegates

object ModelPropertyGenerator {
    fun internalModelProperty(data: ViewWithDelegateGenerationData, delegateProperty: PropertySpec, listFieldsGenerationData: List<ListFieldGenerationData>, childrenUpdateProperty: PropertySpec): PropertySpec {
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
                                .addStatement("%N = true", childrenUpdateProperty)
                                .apply {
                                    data.basicFields
                                            .filter { it.childName.isNotEmpty() }
                                            .forEach {
                                                addStatement("··if·(oldValue.${it.name}·!=·newValue.${it.name})·${it.resolveSetter(it.childName, "newValue.${it.name}")}")
                                            }
                                }
                                .apply {
                                    listFieldsGenerationData
                                            .filter { it.childListView.isNotEmpty() }
                                            .forEach { listField ->
                                                val wrapMethod = data.wrapAdapterMapping.find { it.first.value == listField.name }?.second
                                                // TODO Need use typeName, but this is hard for now
                                                if (wrapMethod == null) {
                                                    addStatement("··if·(oldValue.${listField.name}·!=·newValue.${listField.name})·${listField.childListView}.setAdapter(${listField.name.capitalize()}Adapter(context,·newValue.${listField.name}))")
                                                } else {
                                                    addStatement("··if·(oldValue.${listField.name}·!=·newValue.${listField.name})·${listField.childListView}.setAdapter(delegate.${wrapMethod.simpleName}(${listField.name.capitalize()}Adapter(context,·newValue.${listField.name})))")
                                                }
                                            }
                                }
                                .addStatement("%N = false", childrenUpdateProperty)
                                .add("""
                                                    |
                                                    |  %N.onNewInternalState(newValue)
                                                    |}
                                                """.trimMargin(), delegateProperty)
                                .build()
                )
                .build()
    }

    fun buildListenersSpecs(basicFields: List<ViewField>, typeMapping: Map<String, ViewWithDelegateGenerationData>): List<Pair<String, PropertySpec>> {
        return basicFields
                .filter { it.rwType.notifyIntChanges }
                .map { field ->
                    val fieldType = field.resolveType(typeMapping)
                    val onChangedListener = PropertySpec
                                        .builder("on${field.name.capitalize()}Changed", LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(fieldType)), returnType = Unit::class.asTypeName()).copy(nullable = true))
                                        .mutable()
                                        .initializer("null")
                                        .build()
                    field.name to onChangedListener
                }
    }

    fun buildFieldsSpecs(
            data: ViewWithDelegateGenerationData,
            listenersMap: Map<String, PropertySpec>,
            internalModelProperty: PropertySpec,
            delegateProperty: PropertySpec,
            notifyChangeFunction: FunSpec?,
            processingTypeMap: Map<String, ViewWithDelegateGenerationData>
    ): List<FieldMethodsGenerationMetadata> {
        val basicFields = data.basicFields
                .filter { it.rwType.public }
                .map { field ->
                    val fieldType = field.resolveType(processingTypeMap)
                    val onChangedListener = listenersMap[field.name]

                    val fieldProperty = PropertySpec
                            .builder(field.name, fieldType)
                            .getter(FunSpec.getterBuilder().addStatement("return %N.${field.name}", internalModelProperty).build())
                            .also {
                                if (field.rwType.mutablePublic) {
                                    val setterParameter = ParameterSpec.builder("new${field.name.capitalize()}", fieldType).build()
                                    it.mutable().setter(
                                            FunSpec.setterBuilder()
                                                    .addParameter(setterParameter)
                                                    .addStatement("if (%1N.${field.name} == %2N) return", internalModelProperty, setterParameter)
                                                    .addStatement("val old${field.name.capitalize()} = %1N.copy(${field.name} = %2N)", internalModelProperty, setterParameter)
                                                    .addStatement("%1N = %2N.validateStateForNewInput(old${field.name.capitalize()})", internalModelProperty, delegateProperty)
                                                    .apply {
                                                        if (notifyChangeFunction != null) {
                                                            addStatement("%1N(old${field.name.capitalize()}, %2N)", notifyChangeFunction, internalModelProperty)
                                                        }
                                                    }
                                                    .apply {
                                                        if (field.rwType.notifyExtChanges) {
                                                            onChangedListener?.apply {
                                                                addStatement("%1N?.invoke(%2N)", this, setterParameter)
                                                            }
                                                        }
                                                    }
                                                    .build()
                                    )
                                }
                            }
                            .build()
                    FieldMethodsGenerationMetadata(name = field.name, readWriteProperty = fieldProperty, listenerProperty = onChangedListener)
                }

        val listFields = data.collectionFields
                .filter { it.rwType.public }
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
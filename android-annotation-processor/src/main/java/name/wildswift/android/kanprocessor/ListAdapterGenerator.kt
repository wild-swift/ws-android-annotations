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

package name.wildswift.android.kanprocessor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import name.wildswift.android.kannotations.ListViewField
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import name.wildswift.android.kanprocessor.utils.*
import java.lang.ref.WeakReference

object ListAdapterGenerator {

    fun buildOldAdapterClass(listField: ListViewField, processingTypeMap: Map<String, ViewWithDelegateGenerationData>): TypeSpec {
        return TypeSpec
                .classBuilder(listField.name.capitalize() + "Adapter")
                .addModifiers(KModifier.PRIVATE)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .addParameter(ParameterSpec.builder("values", itemsDSClass.parameterizedBy(listField.getModelType(processingTypeMap))).build())
                                .build()
                )
                .addProperty(PropertySpec.builder("context", contextClass, KModifier.PRIVATE).initializer("context").build())
                .addProperty(PropertySpec.builder("values", itemsDSClass.parameterizedBy(listField.getModelType(processingTypeMap)), KModifier.PRIVATE).initializer("values").build())
                .addProperty(PropertySpec.builder("createdViews", LIST.parameterizedBy(WeakReference::class.asTypeName().parameterizedBy(listField.getAdapterViewType(processingTypeMap))), KModifier.PRIVATE).initializer("listOf()").mutable().build())
                .superclass(baseAdapterClass)
                .addSuperinterface(itemsObserverClass)
                .addFunction(
                        FunSpec.builder("getView")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addParameter(ParameterSpec.builder("reuse", viewClass.copy(nullable = true)).build())
                                .addParameter(ParameterSpec.builder("parent", viewGroupClass).build())
                                .returns(viewClass)
                                .addStatement("val view = reuse as? %1T ?:·%1T(context).apply·{·createdViews·=·createdViews.filter·{·it.get()·!=·null·}·+·%2T(this)·}", listField.getAdapterViewType(processingTypeMap), WeakReference::class.asTypeName())
                                .addStatement("view.${listField.buildSetViewModelStatement(processingTypeMap, "values[index]")}")
                                .addStatement("view.tag = index")
                                .addStatement("return view")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("getItem")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .returns(ANY)
                                .addStatement("return values[index]")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("getItemId")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .returns(LONG)
                                .addStatement("return index.toLong()")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("getCount")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(INT)
                                .addStatement("return values.size")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("registerDataSetObserver")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("observer", dataSetObserverClass).build())
                                .addStatement("super.registerDataSetObserver(observer)")
                                .addStatement("values.addObserver(this)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("unregisterDataSetObserver")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("observer", dataSetObserverClass).build())
                                .addStatement("super.unregisterDataSetObserver(observer)")
                                .addStatement("values.removeObserver(this)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemInserted")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("notifyDataSetChanged()")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemChanged")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("createdViews.filter·{·it.get()?.tag·==·index·}.forEach·{")
                                .addStatement("⇥it.get()?.${listField.buildSetViewModelStatement(processingTypeMap, "values[index]")}")
                                .addStatement("⇤}")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemRemoved")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("notifyDataSetChanged()")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemsReloaded")
                                .addModifiers(KModifier.OVERRIDE)
                                .addStatement("notifyDataSetChanged()")
                                .build()
                )
                .build()
    }

    fun buildRecyclerAdapterClass(listField: ListViewField, processingTypeMap: Map<String, ViewWithDelegateGenerationData>): TypeSpec {
        return TypeSpec
                .classBuilder(listField.name.capitalize() + "Adapter")
                .addModifiers(KModifier.PRIVATE)
                .superclass(recyclerAdapterClass.parameterizedBy(recyclerHolderClass))
                .addSuperinterface(itemsObserverClass)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("context", contextClass).build())
                                .addParameter(ParameterSpec.builder("values", itemsDSClass.parameterizedBy(listField.getModelType(processingTypeMap))).build())
                                .build()
                )
                .addProperty(PropertySpec.builder("context", contextClass, KModifier.PRIVATE).initializer("context").build())
                .addProperty(PropertySpec.builder("values", itemsDSClass.parameterizedBy(listField.getModelType(processingTypeMap)), KModifier.PRIVATE).initializer("values").build())
                .addProperty(PropertySpec.builder("createdViews", LIST.parameterizedBy(WeakReference::class.asTypeName().parameterizedBy(listField.getAdapterViewType(processingTypeMap))), KModifier.PRIVATE).initializer("listOf()").mutable().build())
                .addFunction(
                        FunSpec.builder("onCreateViewHolder")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("parent", viewGroupClass).build())
                                .addParameter(ParameterSpec.builder("viewType", INT).build())
                                .returns(recyclerHolderClass)
                                .addStatement("return object : %T(%T(context)) {}", recyclerHolderClass, listField.getAdapterViewType(processingTypeMap))
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onBindViewHolder")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("holder", recyclerHolderClass).build())
                                .addParameter(ParameterSpec.builder("position", INT).build())
                                .beginControlFlow("(holder.itemView as %T).apply", listField.getAdapterViewType(processingTypeMap))
                                .addStatement("tag = position")
                                .addStatement(listField.buildSetViewModelStatement(processingTypeMap, "values[position]"))
                                .endControlFlow()
                                .build()
                )
                .addFunction(
                        FunSpec.builder("getItemCount")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(INT)
                                .addStatement("return values.size")
                                .build()
                )
                /*
                *     override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
      super.unregisterAdapterDataObserver(observer)
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
      super.registerAdapterDataObserver(observer)
    }
*/
                .addFunction(
                        FunSpec.builder("registerAdapterDataObserver")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("observer", recyclerDataObserverClass).build())
                                .addStatement("super.registerAdapterDataObserver(observer)")
                                .addStatement("values.addObserver(this)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("unregisterAdapterDataObserver")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("observer", recyclerDataObserverClass).build())
                                .addStatement("super.unregisterAdapterDataObserver(observer)")
                                .addStatement("values.removeObserver(this)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemInserted")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("notifyItemInserted(index)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemChanged")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("createdViews.filter·{·it.get()?.tag·==·index·}.forEach·{")
                                .addStatement("⇥it.get()?.${listField.buildSetViewModelStatement(processingTypeMap, "values[index]")}")
                                .addStatement("⇤}")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemRemoved")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("index", INT).build())
                                .addStatement("notifyItemRemoved(index)")
                                .build()
                )
                .addFunction(
                        FunSpec.builder("onItemsReloaded")
                                .addModifiers(KModifier.OVERRIDE)
                                .addStatement("notifyDataSetChanged()")
                                .build()
                ).build()
    }
}
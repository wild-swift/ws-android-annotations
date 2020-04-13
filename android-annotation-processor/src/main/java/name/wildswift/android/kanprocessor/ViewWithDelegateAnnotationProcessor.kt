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
import name.wildswift.android.kannotations.ListImplementation
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate
import name.wildswift.android.kanprocessor.ListAdapterGenerator.buildOldAdapterClass
import name.wildswift.android.kanprocessor.ListAdapterGenerator.buildRecyclerAdapterClass
import name.wildswift.android.kanprocessor.datahelpers.ListFieldGenerationData
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import name.wildswift.android.kanprocessor.datahelpers.ViewWithDelegateGenerationData
import name.wildswift.android.kanprocessor.generators.DataClassGenerator.generateDataClass
import name.wildswift.android.kanprocessor.generators.DelegatedMethodsGenerator.buildDelegatedMethods
import name.wildswift.android.kanprocessor.generators.ModelPropertyGenerator.buildFieldsSpecs
import name.wildswift.android.kanprocessor.generators.ModelPropertyGenerator.buildListenersSpecs
import name.wildswift.android.kanprocessor.generators.ModelPropertyGenerator.internalModelProperty
import name.wildswift.android.kanprocessor.generators.StateSerializationGenerator.buildDeserializeStateMethod
import name.wildswift.android.kanprocessor.generators.StateSerializationGenerator.buildSerializeStateMethod
import name.wildswift.android.kanprocessor.utils.*
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeVisitor
import javax.lang.model.util.SimpleTypeVisitor8
import javax.tools.Diagnostic

/**
 * Created by swift
 */
@SupportedAnnotationTypes("name.wildswift.android.kannotations.ViewWithDelegate")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ViewWithDelegateAnnotationProcessor : KotlinAbstractProcessor() {

    private val checkViewDelegateVisitor: TypeVisitor<Boolean, Any?> = object : SimpleTypeVisitor8<Boolean, Any?>() {
        override fun visitDeclared(t: DeclaredType, p: Any?): Boolean {
            return (t.asElement() as? TypeElement)?.asClassName() == ViewDelegate::class.asClassName()
        }
    }

    private var processingTypeMap: Map<String, ViewWithDelegateGenerationData> = mapOf()

    override fun getSupportedOptions() = setOf("application.id")

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val appId = processingEnv.options["application.id"]
                ?: throw IllegalArgumentException("Argument \"application.id\" is not set. Please add \"kapt\" -> \"arguments\" -> \"arg(\"application.id\", <value>)\"")
        val envConstants = ProcessingEnvConstants(appId = appId)

        val processingElements = roundEnv.getElementsAnnotatedWith(ViewWithDelegate::class.java)
        validate(processingElements)

        processingTypeMap = processingElements
                .mapNotNull { (it as? TypeElement)?.let { ViewWithDelegateGenerationData.from(it, processingEnv) } }
                .map { it.delegateType.canonicalName to it }
                .toMap()

        processingTypeMap.values.forEach { writeExtensionsFile(it, envConstants) }
        return true
    }

    private fun validate(processingElements: MutableSet<out Element>) {
        processingElements.forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied only to class.")
                throw IllegalArgumentException("ViewWithDelegate may be used only as class annotation. Can't apply to ${(it as? QualifiedNameable)?.qualifiedName
                        ?: it.simpleName}")
            }
            if ((it as? TypeElement)?.superclass?.accept<Boolean, Any?>(checkViewDelegateVisitor, null) != true) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Class is not implements ${ViewDelegate::class.java.name}.")
                throw IllegalArgumentException("ViewWithDelegate may be used only with class that implements ${ViewDelegate::class.java.name}. " +
                        "Can't apply to ${(it as? QualifiedNameable)?.qualifiedName
                                ?: it.simpleName}")
            }
        }
    }

    private fun writeExtensionsFile(data: ViewWithDelegateGenerationData, envConstants: ProcessingEnvConstants) {
        val delegateProperty = PropertySpec.builder("delegate", data.delegateType).addModifiers(KModifier.PRIVATE).initializer("%T(this)", data.delegateType).build()

        val internalProperties = data
                .basicFields
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { this.type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.resolveDefaultValue()
                    )
                }
                .plus(data.collectionFields.map {
                    PropertyData(
                            it.name,
                            itemsDSClass.parameterizedBy(it.getModelType(processingTypeMap)),
                            "%T()",
                            emptyIDSClass
                    )
                })


        val publicModelProperties = data
                .basicFields
                .filter { it.rwType.public }
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { this.type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.resolveDefaultValue()
                    )
                }
                .plus(data.collectionFields.filter { it.isPublic }.map {
                    PropertyData(
                            it.name,
                            itemsDSClass.parameterizedBy(it.getModelType(processingTypeMap)),
                            "%T()",
                            emptyIDSClass
                    )
                })

        val publicMutableProperties = data
                .basicFields
                .filter { it.rwType.mutablePublic }
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { this.type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.resolveDefaultValue()
                    )
                }
                .plus(data.collectionFields.filter { it.isPublic }.map {
                    PropertyData(
                            it.name,
                            itemsDSClass.parameterizedBy(it.getModelType(processingTypeMap)),
                            "%T()",
                            emptyIDSClass
                    )
                })


        val listFieldsGenerationData = data
                .collectionFields
                .map { listField ->
                    val adapter = when (listField.listImplementation) {
                        ListImplementation.ListView -> {
                            buildOldAdapterClass(listField, processingTypeMap)
                        }
                        ListImplementation.RecyclerView -> {
                            buildRecyclerAdapterClass(listField, processingTypeMap)
                        }
                    }
                    ListFieldGenerationData(listField.name, listField.listImplementation, listField.childListView, adapter)
                }

        val publicModelClass = if (data.rootAnnotation.generateViewDataObject) generateDataClass(data.externalModelType, publicModelProperties, generationPath) else null
        val internalModelClass = generateDataClass(data.internalModelType, internalProperties, generationPath)
        val internalModelProperty = internalModelClass?.let { internalModelProperty(data, delegateProperty, listFieldsGenerationData) }

        val listeners = buildListenersSpecs(data.basicFields)

        val onPublicModelChangedListener =
                if (publicModelClass != null) {
                    PropertySpec
                            .builder("onViewModelChanged", LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(data.externalModelType)), returnType = Unit::class.asTypeName()).copy(nullable = true))
                            .mutable()
                            .initializer("null")
                            .build()
                } else {
                    null
                }

        val notifyChangedFun = publicModelProperties
                .takeIf { it.isNotEmpty() }
                ?.let { createNotifyChanged(listeners, data.internalModelType, it, data.externalModelType, onPublicModelChangedListener) }

        val publicModelProperty =
                if (publicModelClass != null)
                    PropertySpec
                            .builder("viewModel", data.externalModelType)
                            .getter(FunSpec.getterBuilder().addStatement("return %1T(\n" +
                                    "⇥⇥${publicModelProperties.joinToString(", \n") { "${it.name} = %2N.${it.name}" }}\n" +
                                    "⇤⇤)", data.externalModelType, internalModelProperty!!).build())
                            .mutable()
                            .setter(
                                    FunSpec.setterBuilder()
                                            .addParameter(ParameterSpec.builder("value", data.externalModelType).build())
                                            .addStatement("if (${publicMutableProperties.joinToString("·&& ") { "%1N.${it.name}·==·value.${it.name}" }}) return", internalModelProperty)
                                            .addStatement("val oldValue = %1N", internalModelProperty)
                                            .addStatement("val newValue = %1N.copy(\n" +
                                                    "⇥⇥${publicMutableProperties.joinToString { "${it.name} = value.${it.name}" }}\n" +
                                                    "⇤⇤)", internalModelProperty)
                                            .addStatement("%1N = %2N.validateStateForNewInput(newValue)", internalModelProperty, delegateProperty)
                                            .apply {
                                                data.basicFields.filter { it.rwType.notifyExtChanges }.forEach {
                                                    val listener = listeners.toMap()[it.name]
                                                    if (listener != null)
                                                        addStatement("if·(oldValue.${it.name}·!=·value.${it.name})·%1N?.invoke(value.${it.name})", listener)
                                                }
                                                if (notifyChangedFun != null) {
                                                    addStatement("%1N(newValue, %2N)", notifyChangedFun, internalModelProperty)
                                                }
                                            }
                                            .addStatement("%1N?.invoke(value)", onPublicModelChangedListener!!)
                                            .build()
                            )
                            .build()
                else
                    null

        val saveStateMethod = if (data.basicFields.isNotEmpty() && data.rootAnnotation.saveInstanceState) buildSerializeStateMethod(internalProperties, internalModelProperty) else null
        val restoreStateMethod = if (data.basicFields.isNotEmpty() && data.rootAnnotation.saveInstanceState) buildDeserializeStateMethod(data, internalModelProperty, internalProperties) else null

        val viewClassSpec = TypeSpec
                .classBuilder(data.generateViewType)
                .addModifiers(data.visibilityModifier)
                .superclass(data.rootAnnotation.safeGetType { parent })
                .addViewConstructors {
                    if (data.attrs.isNotEmpty() && it > 1) {
                        addStatement("setAttrs(context, attrs)")
                    }
                    this
                }
                .addProperty(delegateProperty)
                .apply {
                    if (internalModelProperty != null) addProperty(internalModelProperty)
                    if (publicModelProperty != null) addProperty(publicModelProperty)
                    if (onPublicModelChangedListener != null) addProperty(onPublicModelChangedListener)
                }


        val fieldsProperties = internalModelProperty?.let { buildFieldsSpecs(data, listeners.toMap(), internalModelProperty, delegateProperty, notifyChangedFun, processingTypeMap) }
                ?: listOf()

        fieldsProperties.forEach { field ->
            viewClassSpec.addProperty(field.readWriteProperty)
            field.listenerProperty?.apply {
                viewClassSpec.addProperty(field.listenerProperty)
            }
        }

        data.events.forEach {
            PropertySpec
                    .builder(it.name, LambdaTypeName.get(returnType = Unit::class.asTypeName()).copy(nullable = true))
                    .mutable()
                    .initializer("null")
                    .build()
                    .apply {
                        viewClassSpec.addProperty(this)
                    }
        }

        viewClassSpec.addInitializerBlock(
                CodeBlock.builder()
                        .addStatement("inflate(context, R.layout.${data.layoutName}, this)")
                        .addStatement("%1N.setupView()", delegateProperty)
                        .also { codeBlockBuilder ->
                            data.events.forEach { event ->
                                codeBlockBuilder.add(event.childName.let { if (it.isEmpty()) "" else "$it." } + event.resolveListener("${event.name}?.invoke()"))
                            }
                        }
                        .also { codeBlockBuilder ->
                            data.basicFields
                                    .filter { it.childName.isNotEmpty() }
                                    .filter { it.rwType.notifyIntChanges }
                                    .groupBy { it.childName }
                                    .forEach { child, value ->
                                        var propertiesList = value
                                        while (propertiesList.isNotEmpty()) {
                                            val viewField = propertiesList[0]
                                            val listenerGroup = viewField.property.getListenerGroup()
                                            if (listenerGroup.isNotEmpty()) {
                                                listenerGroup
                                                        .mapNotNull { property -> propertiesList.find { it.property == property } }
                                                        .let { viewField ->
                                                            """
                                                                |    if (${viewField.joinToString(separator = " || ") { "%1N.${it.name} != ${it.property.getListenerPropertyName()}" }}) {
                                                                |        val oldModel = %1N
                                                                |        %1N = %2N.validateStateForOutput(
                                                                |                %1N.copy(
                                                                |${viewField.joinToString(separator = ",\n") { "                    ${it.name} = ${it.property.getListenerPropertyName()}" }}
                                                                |                )
                                                                |        )
                                                                |        %2N.onNewInternalState(%1N)
                                                                |        %3N(oldModel, %1N)
                                                                |    }
                                                                |
                                                        """.trimMargin()
                                                        }
                                                        .also { listenerGroup.first().buildListener(child, it, internalModelProperty!!, delegateProperty, notifyChangedFun, codeBlockBuilder) }

                                                propertiesList = propertiesList.filter { !listenerGroup.contains(it.property) }
                                            } else {
                                                propertiesList = propertiesList.drop(1)
                                            }
                                        }
                                    }

                        }
                        .build()
        )


        if (data.attrs.isNotEmpty()) {
            val setAttrsFun = FunSpec
                    .builder("setAttrs")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("context", contextClass)
                    .addParameter("attrs", ClassName("android.util", "AttributeSet").copy(nullable = true))
                    .addStatement("if (attrs == null) return")
                    .addStatement("val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.${data.generateViewType.simpleName})")

            data.attrs.forEach {
                val propertyName = if (it.fieldName.isBlank()) it.reference else it.fieldName
                if (data.basicFields.find { it.isPublic && it.name == propertyName } == null) {
                    viewClassSpec.addProperty(PropertySpec
                            .builder(propertyName, it.type.fieldClass())
                            .mutable()
                            .initializer(it.type.initValue())
                            .build()
                    )
                }

                setAttrsFun.addStatement("$propertyName = styleAttrs.${it.type.loadCode("R.styleable.${data.generateViewType.simpleName}_${it.reference}")}")
            }

            setAttrsFun.addStatement("styleAttrs.recycle()")
            viewClassSpec.addFunction(setAttrsFun.build())

        }

        viewClassSpec
                .delegateCall("onAttachedToWindow", delegateProperty, "onShow")
                .delegateCall("onDetachedFromWindow", delegateProperty, "onHide")

        saveStateMethod?.apply {
            viewClassSpec.addFunction(this)
            viewClassSpec.generateViewSave(this)
        }

        restoreStateMethod?.apply {
            viewClassSpec.addFunction(this)
            viewClassSpec.generateViewRestore(this)
        }

        if (notifyChangedFun != null) {
            viewClassSpec.addFunction(notifyChangedFun)
        }

        buildDelegatedMethods(data.delegatedMethods, delegateProperty).forEach { viewClassSpec.addFunction(it) }


        listFieldsGenerationData
                .forEach { (_, _, _, adapter) ->
                    viewClassSpec.addType(adapter)
                }

        FileSpec
                .builder(data.generateViewType.packageName, data.generateViewType.simpleNames.first())
                .addImport(envConstants.appId, "R")
                .addImport(viewClass, "inflate")
                .addImport("kotlinx.android.synthetic.main.${data.layoutName}.view", data.basicFields.mapNotNull { it.childName.takeIf { it.isNotBlank() } } + data.events.mapNotNull { it.childName.takeIf { it.isNotBlank() } } + data.collectionFields.mapNotNull { it.childListView.takeIf { it.isNotBlank() } })
                .addImport("name.wildswift.android.kannotations.util", "put")
                .addType(viewClassSpec.build())
                .addComment("""
                    |
                """.trimMargin())
                .build()
                .writeTo(generationPath)

    }


    private fun createNotifyChanged(methodsMapping: List<Pair<String, PropertySpec>>, internalModelType: ClassName, publicFields: List<PropertyData>, publicModelType: ClassName, publicModelChangedListener: PropertySpec?): FunSpec {
        val notifyChangedOldModel = ParameterSpec.builder("oldModel", internalModelType).build()
        val notifyChangedCurrentModel = ParameterSpec.builder("currentModel", internalModelType).build()
        val notifyChangedFunBuilder = FunSpec
                .builder("notifyChanged")
                .addModifiers(KModifier.PRIVATE)
                .addParameter(notifyChangedOldModel)
                .addParameter(notifyChangedCurrentModel)
                .apply {
                    if (publicModelChangedListener != null) {
                        addStatement("if (${publicFields.joinToString(" || ") { "%1N.${it.name} != %2N.${it.name}" }}) %3N?.invoke(%4T(${publicFields.joinToString { "${it.name} = %2N.${it.name}" }}))", notifyChangedOldModel, notifyChangedCurrentModel, publicModelChangedListener, publicModelType)
                    }
                }

        methodsMapping.forEach { (name, listenerProperty) ->
            notifyChangedFunBuilder.addStatement("if (%1N.${name} != %2N.${name}) %3N?.invoke(%2N.${name})", notifyChangedOldModel, notifyChangedCurrentModel, listenerProperty)
        }
        return notifyChangedFunBuilder.build()
    }
}
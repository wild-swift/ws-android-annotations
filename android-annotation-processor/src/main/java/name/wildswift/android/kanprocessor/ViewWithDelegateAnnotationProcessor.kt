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
import name.wildswift.android.kannotations.*
import name.wildswift.android.kannotations.interfaces.ViewDelegate
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import name.wildswift.android.kanprocessor.utils.*
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeVisitor
import javax.lang.model.util.SimpleTypeVisitor8
import javax.tools.Diagnostic
import kotlin.properties.Delegates

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

    override fun getSupportedOptions() = setOf("application.id")

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val appId = processingEnv.options["application.id"]
                ?: throw IllegalArgumentException("Argument \"application.id\" is not set. Please add \"kapt\" -> \"arguments\" -> \"arg(\"application.id\", <value>)\"")
        val envConstants = ProcessingEnvConstants(
                appId = appId
        )
        roundEnv.getElementsAnnotatedWith(ViewWithDelegate::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException("ViewWithDelegate may be used only as class annotation. Can't apply to ${(it as? QualifiedNameable)?.qualifiedName
                        ?: it.simpleName}")
            }
            if ((it as? TypeElement)?.superclass?.accept<Boolean, Any?>(checkViewDelegateVisitor, null) != true) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Class is not implements ${ViewDelegate::class.java.name}.")
                throw IllegalArgumentException("ViewWithDelegate may be used only with class that implements ${ViewDelegate::class.java.name}. " +
                        "Can't apply to ${(it as? QualifiedNameable)?.qualifiedName
                                ?: it.simpleName}")
            }
            (it as? TypeElement)?.apply {
                val className = simpleName.toString()
                val packageName = processingEnv.elementUtils.getPackageOf(this).toString()
                val metadata = this.getAnnotation(ViewWithDelegate::class.java)
                val attrs = this.getAnnotationsByType(Attributes::class.java).flatMap { it.value.asIterable() } + this.getAnnotationsByType(ViewAttribute::class.java)
                val events = this.getAnnotationsByType(Events::class.java).flatMap { it.value.asIterable() } + this.getAnnotationsByType(ViewEvent::class.java)
                val fields = this.getAnnotationsByType(Fields::class.java).flatMap { it.value.asIterable() } + this.getAnnotationsByType(ViewField::class.java)
                val listFields = this.getAnnotationsByType(ListingsFields::class.java).flatMap { it.value.asIterable() } + this.getAnnotationsByType(ListViewField::class.java)
                val visibilityModifier = resolveKotlinVisibility(this)
                val delegatedMethods = enclosedElements.filter { it.getAnnotation(Delegated::class.java) != null }.filterIsInstance<ExecutableElement>()
                writeExtensionsFile(metadata, attrs, events, fields, listFields, delegatedMethods, className, packageName, visibilityModifier, envConstants)
            }
        }
        return true
    }

    private fun writeExtensionsFile(rootAnnotation: ViewWithDelegate, attrs: List<ViewAttribute>, events: List<ViewEvent>, basicFields: List<ViewField>, listFields: List<ListViewField>, delegatedMethods: List<ExecutableElement>,
                                    className: String, pack: String, visibilityModifier: KModifier, envConstants: ProcessingEnvConstants) {
        val viewClassName = rootAnnotation.name.takeIf { it.isNotBlank() }
                ?: className.let { if (it.endsWith("Delegate")) it.substring(0, it.length - "Delegate".length) else null }
                ?: throw IllegalArgumentException("Class name must be specified or delegate must ends with 'Delegate' suffix. Class $pack.$className")

        val layoutName = rootAnnotation.layoutResourceName.takeIf { it.isNotEmpty() }
                ?: viewClassName.toViewResourceName()

        if (basicFields.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Fields not configured properly for class $pack.$className")
        if (events.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Events not configured properly for class $pack.$className")

        val delegateType = ClassName(pack, className)
        val delegateProperty = PropertySpec.builder("delegate", delegateType).addModifiers(KModifier.PRIVATE).initializer("%T(this)", delegateType).build()

        val internalProperties = basicFields
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { this.type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.resolveDefaultValue()
                    )
                }

        val modelProperties = basicFields
                .filter { it.publicAccess }
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { this.type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.resolveDefaultValue()
                    )
                }

        val (publicModelType, publicModelClass) = generateDataClass(pack, "${viewClassName}Model", modelProperties, generationPath)
        val (internalModelType, internalModelClass) = generateDataClass(pack, "${viewClassName}IntState", internalProperties, generationPath)
        val internalModelProperty = internalModelClass?.let { internalModelProperty(internalModelType, basicFields, delegateProperty) }

        val onPublicModelChangedListener =
                if (publicModelClass != null) {
                    PropertySpec
                            .builder("onViewModelChanged", LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(publicModelType)), returnType = Unit::class.asTypeName()).copy(nullable = true))
                            .mutable()
                            .initializer("null")
                            .build()
                } else {
                    null
                }

        val publicModelProperty =
                if (publicModelClass != null)
                    PropertySpec
                            .builder("viewModel", publicModelType)
                            .getter(FunSpec.getterBuilder().addStatement("return %1T(${modelProperties.joinToString { "${it.name} = %2N.${it.name}" }})", publicModelType, internalModelProperty!!).build())
                            .mutable()
                            .setter(
                                    FunSpec.setterBuilder()
                                            .addParameter(ParameterSpec.builder("value", publicModelType).build())
                                            .addStatement("if (${modelProperties.joinToString(" && ") { "%1N.${it.name} == value.${it.name}" }}) return", internalModelProperty)
                                            .addStatement("%1N = %2N.validateStateForNewInput(%1N.copy(${modelProperties.joinToString { "${it.name} = value.${it.name}" }}))", internalModelProperty, delegateProperty)
                                            .addStatement("%1N?.invoke(value)", onPublicModelChangedListener!!)
                                            .build()
                            )
                            .build()
                else
                    null

        val saveStateMethod =
                if (basicFields.isNotEmpty()) {
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
                } else {
                    null
                }

        val restoreStateMethod =
                if (basicFields.isNotEmpty()) {
                    FunSpec
                            .builder("setState")
                            .addModifiers(KModifier.PRIVATE)
                            .addParameter("state", bundleClass)
                            .addStatement("%1N = %2T(", internalModelProperty!!, internalModelType)
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
                } else {
                    null
                }

        val viewClass = TypeSpec
                .classBuilder(ClassName(pack, viewClassName))
                .addModifiers(visibilityModifier)
                .superclass(rootAnnotation.safeGetType { parent })
                .addViewConstructors {
                    if (attrs.isNotEmpty() && it > 1) {
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


        val notifyChangedFun = basicFields
                .filter { it.publicAccess }
                .takeIf { it.isNotEmpty() }
                ?.let { createNotifyChanged(it, internalModelType, internalModelProperty!!, publicModelType, onPublicModelChangedListener, delegateProperty, viewClass) }


        events.forEach {
                    PropertySpec
                            .builder(it.name, LambdaTypeName.get(returnType = Unit::class.asTypeName()).copy(nullable = true))
                            .mutable()
                            .initializer("null")
                            .build()
                            .apply {
                                viewClass.addProperty(this)
                            }
                }

        viewClass.addInitializerBlock(
                CodeBlock.builder()
                        .addStatement("inflate(context, R.layout.$layoutName, this)")
                        .addStatement("%1N.setupView()", delegateProperty)
                        .also { codeBlockBuilder ->
                            events.forEach { event ->
                                        codeBlockBuilder.add(event.childName.let { if (it.isEmpty()) "" else "$it." } + event.resolveListener("${event.name}?.invoke()"))
                                    }
                        }
                        .also { codeBlockBuilder ->
                            basicFields
                                    .filter { it.childName.isNotEmpty() }
                                    .filter { it.rwType != FieldRWType.writeOnly }
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


        if (attrs.isNotEmpty()) {
            val setAttrsFun = FunSpec
                    .builder("setAttrs")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("context", contextClass)
                    .addParameter("attrs", ClassName("android.util", "AttributeSet").copy(nullable = true))
                    .addStatement("if (attrs == null) return")
                    .addStatement("val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.$viewClassName)")

            attrs.forEach {
                val propertyName = if (it.fieldName.isBlank()) it.reference else it.fieldName
                if (basicFields.find { it.publicAccess && it.name == propertyName } == null) {
                    viewClass.addProperty(PropertySpec
                            .builder(propertyName, it.type.fieldClass())
                            .mutable()
                            .initializer(it.type.initValue())
                            .build()
                    )
                }

                setAttrsFun.addStatement("$propertyName = styleAttrs.${it.type.loadCode("R.styleable.${viewClassName}_${it.reference}")}")
            }

            setAttrsFun.addStatement("styleAttrs.recycle()")
            viewClass.addFunction(setAttrsFun.build())

        }

        viewClass
                .delegateCall("onAttachedToWindow", delegateProperty, "onShow")
                .delegateCall("onDetachedFromWindow", delegateProperty, "onHide")

        if (saveStateMethod != null) {
            viewClass.addFunction(saveStateMethod)
            if (rootAnnotation.saveInstanceState) {
                viewClass.generateViewSave(saveStateMethod)
            }
        }

        if (restoreStateMethod != null) {
            viewClass.addFunction(restoreStateMethod)
            if (rootAnnotation.saveInstanceState) {
                viewClass.generateViewRestore(restoreStateMethod)
            }
        }

        if (notifyChangedFun != null) {
            viewClass.addFunction(notifyChangedFun)
        }

        val delegates = delegatedMethods.map {
            val name = it.simpleName.toString().split("$").firstOrNull().orEmpty()
            val funBuilder = FunSpec.builder(name)
                    .returns(it.returnType.asTypeName())
                    .addCode("""
                        |    return %N.$name(${it.parameters.joinToString { it.simpleName.toString() }})
                        |
                    """.trimMargin(), delegateProperty)
            it.parameters.forEach { funBuilder.addParameter(it.simpleName.toString(), it.asType().asTypeName().let { if (it.toString() == "java.lang.String") ClassName("kotlin", "String") else it }) }
            funBuilder.build()
        }

        delegates.forEach {
            viewClass
                    .addFunction(it)
        }

        FileSpec
                .builder(pack, viewClassName)
                .addComment(processingEnv.options.map { (f, s) -> "$f = $s" }.joinToString("\n"))
                .addImport(envConstants.appId, "R")
                .addImport(name.wildswift.android.kanprocessor.utils.viewClass, "inflate")
                .addImport("kotlinx.android.synthetic.main.$layoutName.view", basicFields.mapNotNull { it.childName.takeIf { it.isNotBlank() } } + events.mapNotNull { it.childName.takeIf { it.isNotBlank() } })
                .addImport("name.wildswift.android.kannotations.util", "put")
                .addType(viewClass.build())
                .build()
                .writeTo(generationPath)

    }


    private fun createNotifyChanged(fields: List<ViewField>, internalModelType: ClassName, internalModelProperty: PropertySpec, publicModelType: ClassName, publicModelChangedListener: PropertySpec?, delegateProperty: PropertySpec, viewClass: TypeSpec.Builder): FunSpec {
        val notifyChangedOldModel = ParameterSpec.builder("oldModel", internalModelType).build()
        val notifyChangedCurrentModel = ParameterSpec.builder("currentModel", internalModelType).build()
        val notifyChangedFunBuilder = FunSpec
                .builder("notifyChanged")
                .addModifiers(KModifier.PRIVATE)
                .addParameter(notifyChangedOldModel)
                .addParameter(notifyChangedCurrentModel)
                .apply {
                    if (publicModelChangedListener != null) {
                        addStatement("if (${fields.filter { it.publicAccess }.joinToString(" || ") { "%1N.${it.name} != %2N.${it.name}" }}) %3N?.invoke(%4T(${fields.filter { it.publicAccess }.joinToString { "${it.name} = %2N.${it.name}" }}))", notifyChangedOldModel, notifyChangedCurrentModel, publicModelChangedListener, publicModelType)
                    }
                }


        fields.forEach { field ->
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
                    .getter(FunSpec.getterBuilder().addStatement("return %N.${field.name}", internalModelProperty).build())
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

            viewClass.addProperty(fieldProperty)
            onChangedListener?.apply {
                viewClass.addProperty(this)
                notifyChangedFunBuilder.addStatement("if (%1N.${field.name} != %2N.${field.name}) %3N?.invoke(%2N.${field.name})", notifyChangedOldModel, notifyChangedCurrentModel, onChangedListener)
            }

        }
        return notifyChangedFunBuilder.build()
    }

    private fun internalModelProperty(modelType: ClassName, fieldsToSet: List<ViewField>, delegateProperty: PropertySpec): PropertySpec {
        return PropertySpec.builder("intModel", modelType)
                .mutable()
                .addModifiers(KModifier.PRIVATE)
                .delegate(
                        CodeBlock.builder()
                                .add("""
                                                    |%1T.observable(%2T()) { _, oldValue, newValue ->
                                                    |        if (oldValue == newValue) return@observable
                                                    |
                                                """.trimMargin(), Delegates::class.asTypeName(), modelType)
                                .add(fieldsToSet
                                        .filter { it.childName.isNotEmpty() }
                                        .filter { it.rwType != FieldRWType.readOnly }
                                        .joinToString(separator = "\n") {
                                            "        if (oldValue.${it.name} != newValue.${it.name}) ${it.childName}.${it.resolveSetter("newValue.${it.name}")}"
                                        }
                                )
                                .add("""
                                                    |
                                                    |        %N.onNewInternalState(newValue)
                                                    |    }
                                                """.trimMargin(), delegateProperty)
                                .build()
                )
                .build()
    }
}
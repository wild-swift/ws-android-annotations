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
import name.wildswift.android.kannotations.FieldRWType
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate
import name.wildswift.android.kanprocessor.datahelpers.PropertyData
import name.wildswift.android.kanprocessor.utils.*
import java.io.File
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
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
    override val tmpFileName: String = "__V"

    private val checkViewDelegateVisitor: TypeVisitor<Boolean, Any?> = object : SimpleTypeVisitor8<Boolean, Any?>() {
        override fun visitDeclared(t: DeclaredType, p: Any?): Boolean {
            return (t.asElement() as? TypeElement)?.asClassName() == ViewDelegate::class.asClassName()
        }
    }


    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val appId = processingEnv.options["application.id"]
                ?: throw IllegalArgumentException("Argument \"application.id\" is not set.")
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
                writeExtensionsFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(ViewWithDelegate::class.java), resolveKotlinVisibility(it), envConstants)
            }
        }
        return true
    }

    private fun writeExtensionsFile(className: String, pack: String, annotation: ViewWithDelegate, visibilityModifier: KModifier, envConstants: ProcessingEnvConstants) {
        val viewClassName = annotation.name.takeIf { it.isNotBlank() }
                ?: className.let { if (it.endsWith("Delegate")) it.substring(0, it.length - "Delegate".length) else null }
                ?: throw IllegalArgumentException("Class name must be specified or delegate must ends with 'Delegate' suffix. Class $pack.$className")

        val layoutName = annotation.layoutResourceName.takeIf { it.isNotEmpty() }
                ?: viewClassName.toViewResourceName()

        if (annotation.fields.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Fields not configured properly for class $pack.$className")
        if (annotation.events.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Fields not configured properly for class $pack.$className")

        val delegateType = ClassName(pack, className)
        val delegateProperty = PropertySpec.builder("delegate", delegateType).addModifiers(KModifier.PRIVATE).initializer("%T(this)", delegateType).build()

        val internalProperties = annotation
                .fields
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { type },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.defaultValue
                    )
                }
        val (internalModelType, internalModelClass) = generateDataClass(pack, "${viewClassName}IntState", internalProperties, generationPath)
        val internalModelProperty = internalModelClass?.let { internalModelProperty(internalModelType, annotation.fields, delegateProperty) }


//    fun setState(state: Bundle)  {
//
//    }

        val saveStateMethod =
                if (annotation.fields.isNotEmpty()) {
                    FunSpec
                            .builder("getState")
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
                if (annotation.fields.isNotEmpty()) {
                    FunSpec
                            .builder("setState")
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
                .superclass(annotation.safeGetType { parent })
                .addViewConstructors {
                    if (annotation.attrs.isNotEmpty() && it > 1) {
                        addStatement("setAttrs(context, attrs)")
                    }
                    this
                }
                .addProperty(delegateProperty)
                .apply {
                    if (internalModelProperty != null) addProperty(internalModelProperty)
                }


        val notifyChangedFun = annotation
                .fields
                .filter { it.publicAccess }
                .takeIf { it.isNotEmpty() }
                ?.let { createNotifyChanged(it, internalModelType, internalModelProperty!!, delegateProperty, viewClass) }


        annotation
                .events
                .forEach {
                    PropertySpec
                            .builder(it.name, LambdaTypeName.get(returnType = Unit::class.asTypeName()).asNullable())
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
                            annotation
                                    .events
                                    .forEach {
                                        codeBlockBuilder.add(it.childName.let { if (it.isEmpty()) "" else "$it." } + it.resolveListener("${it.name}?.invoke()"))
                                    }
                        }
                        .also { codeBlockBuilder ->
                            annotation
                                    .fields
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
                                                        .let {
                                                            """
                                                                |    if (${it.joinToString(separator = " || ") { "%1N.${it.name} != ${it.property.getListenerPropertyName()}" }}) {
                                                                |        val oldModel = %1N
                                                                |        %1N = %2N.validateStateForOutput(
                                                                |                %1N.copy(
                                                                |${it.joinToString(separator = ",\n") { "                    ${it.name} = ${it.property.getListenerPropertyName()}" }}
                                                                |                )
                                                                |        )
                                                                |        %2N.onNewInternalState(%1N)
                                                                |        %3N(oldModel, %1N)
                                                                |    }
                                                        """.trimMargin()
                                                        }
                                                        .let { listenerGroup.first().buildListener(it) }
                                                        .let { "$child.$it" }
                                                        .also {
                                                            codeBlockBuilder.add(it, internalModelProperty!!, delegateProperty, notifyChangedFun)

                                                        }

                                                propertiesList = propertiesList.filter { !listenerGroup.contains(it.property) }
                                            }
                                        }
                                    }

                        }
                        .build()
        )


        if (annotation.attrs.isNotEmpty()) {
            val setAttrsFun = FunSpec
                    .builder("setAttrs")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("context", contextClass)
                    .addParameter("attrs", ClassName("android.util", "AttributeSet").asNullable())
                    .addStatement("if (attrs == null) return")
                    .addStatement("val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.$viewClassName)")

            annotation.attrs.forEach {
                val propertyName = if (it.fieldName.isBlank()) it.reference else it.fieldName
                if (annotation.fields.find { it.publicAccess && it.name == propertyName } == null) {
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
            if (annotation.saveInstanceState) {
                viewClass.generateViewSave(saveStateMethod)
            }
        }

        if (restoreStateMethod != null) {
            viewClass.addFunction(restoreStateMethod)
            if (annotation.saveInstanceState) {
                viewClass.generateViewRestore(restoreStateMethod)
            }
        }

        if (notifyChangedFun != null) {
            viewClass.addFunction(notifyChangedFun)
        }

        FileSpec
                .builder(pack, viewClassName)
                .addImport(envConstants.appId, "R")
                .addImport(name.wildswift.android.kanprocessor.utils.viewClass, "inflate")
                .addImport("kotlinx.android.synthetic.main.$layoutName", "view.*")
                .addImport("name.wildswift.android.kannotations.util", "put")
                .addType(viewClass.build())
                .build()
                .writeTo(File(generationPath))

    }

    private fun createNotifyChanged(fields: List<ViewField>, internalModelType: ClassName, internalModelProperty: PropertySpec, delegateProperty: PropertySpec, viewClass: TypeSpec.Builder): FunSpec {
        val notifyChangedOldModel = ParameterSpec.builder("oldModel", internalModelType).build()
        val notifyChangedCurrentModel = ParameterSpec.builder("currentModel", internalModelType).build()
        val notifyChangedFunBuilder = FunSpec
                .builder("notifyChanged")
                .addModifiers(KModifier.PRIVATE)
                .addParameter(notifyChangedOldModel)
                .addParameter(notifyChangedCurrentModel)

        fields.forEach { field ->
            val fieldType = if (field.property != ViewProperty.none) field.property.getType() else field.safeGetType { type }
            val onChangedListener =
                    if (field.rwType != FieldRWType.writeOnly) {
                        PropertySpec
                                .builder("on${field.name.capitalize()}Changed", LambdaTypeName.get(parameters = listOf(ParameterSpec.unnamed(fieldType)), returnType = Unit::class.asTypeName()).asNullable())
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

    private fun internalModelProperty(modelType: ClassName, fieldsToSet: Array<ViewField>, delegateProperty: PropertySpec): PropertySpec {
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
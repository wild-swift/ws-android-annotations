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

package name.wildswift.android.kanprocessor

import com.squareup.kotlinpoet.*
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
import javax.lang.model.element.VariableElement
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
                appId = appId,
                packageLayoutsTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.layout")
                        ?: throw IllegalArgumentException("Can't find R.layout class. Is \"$appId\" correct package name?"),
                packageIdsTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.id")
                        ?: throw IllegalArgumentException("Can't find R.id class. Is \"$appId\" correct package name?"),
                packageAttrTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.attr"),
                packageStyleableTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.styleable")
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
        val layoutName = annotation.layoutResource
                .let { if (it != 0) resolveIntFieldName(envConstants.packageLayoutsTypeElement, it) else className.toViewResourceName() }
                ?: throw IllegalArgumentException("Can't resolve name for ${annotation.layoutResource} in class ${envConstants.packageLayoutsTypeElement.qualifiedName}")

        val viewClassName = annotation.name.takeIf { it.isNotBlank() }
                ?: className.let { if (it.endsWith("Delegate")) it.substring(0, it.length - "Delegate".length) else null }
                ?: throw IllegalArgumentException("Class name must be specified or delegate must ends with 'Delegate' suffix. Class $pack.$className")

        val inputProperties = annotation
                .fields
                .filter { it.publicAccess }
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { propertyType },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.defaultValue
                    )
                }

        val internalProperties = annotation
                .fields
                .map {
                    PropertyData(
                            it.name,
                            if (it.property != ViewProperty.none) it.property.getType() else it.safeGetType { propertyType },
                            if (it.property != ViewProperty.none) it.property.getDefaultValue() else it.defaultValue
                    )
                }

        val (inputDataClassType, inputDataClass) = generateDataClass(pack, "${viewClassName}Input", inputProperties)
        val (internalDataClassType, internalDataClass) = generateDataClass(pack, "${viewClassName}IntState", internalProperties)


        val viewClassFile = FileSpec
                .builder(pack, viewClassName)


        viewClassFile.addImport(envConstants.appId, "R")
        viewClassFile.addImport("android.view.View", "inflate")
        viewClassFile.addImport("kotlinx.android.synthetic.main.$layoutName", "view.*")

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


        viewClass.addInitializerBlock(
                CodeBlock.builder()
                        .addStatement("inflate(context, R.layout.$layoutName, this)")
                        .addStatement("delegate.setupView(this)")
                        .build()
        )

        val delegateClass = ClassName(pack, className)
        val delegateProperty = PropertySpec.builder("delegate", delegateClass).addModifiers(KModifier.PRIVATE).initializer("%T()", delegateClass).build()
        viewClass.addProperty(delegateProperty)

        val internalModelProperty =
                if (internalDataClass != null)
                    PropertySpec.builder("intModel", internalDataClassType)
                            .mutable()
                            .addModifiers(KModifier.PRIVATE)
                            .delegate(
                                    CodeBlock.builder()
                                            .add("""
                                                |%1T.observable(%2T()) { _, oldValue, newValue ->
                                                |        if (oldValue == newValue) return@observable
                                                |
                                            """.trimMargin(), Delegates::class.asTypeName(), internalDataClassType)
                                            .add(annotation
                                                    .fields
                                                    .filter { it.childId != 0 }
                                                    .joinToString(separator = "\n") {
                                                        "        if (oldValue.${it.name} != newValue.${it.name}) ${resolveIntFieldName(envConstants.packageIdsTypeElement, it.childId)}.${it.resolveSetter("newValue.${it.name}")}"
                                                    }
                                            )
                                            .add("""
                                                |
                                                |        %N.onNewInternalState(this, newValue)
                                                |    }
                                            """.trimMargin(), delegateProperty)
                                            .build()
                            )
                            .build()
                else
                    null


        internalModelProperty?.also { viewClass.addProperty(internalModelProperty) }

        val inputModelProperty =
                if (inputDataClass != null) {
                    PropertySpec.builder("inputModel", inputDataClassType)
                            .mutable()
                            .delegate(
                                    CodeBlock.builder()
                                            .add("""
                                                |%1T.observable(%2T()) { _, _, newValue ->
                                                |        %3N =
                                                |                %4N.validateStateForNewInput(
                                                |                        %3N.copy(
                                                |
                                            """.trimMargin(), Delegates::class.asTypeName(), inputDataClassType, internalModelProperty, delegateProperty)
                                            .add(inputProperties
                                                    .joinToString(separator = ",\n") {
                                                        "                                ${it.name} = newValue.${it.name}"
                                                    }
                                            )
                                            .add("""
                                                |
                                                |                        )
                                                |                )
                                                |    }
                                            """.trimMargin())
                                            .build()
                            )
                            .build()
                } else {
                    null
                }
        inputModelProperty?.also { viewClass.addProperty(inputModelProperty) }


        if (annotation.attrs.isNotEmpty()) {
            envConstants.packageAttrTypeElement
                    ?: throw IllegalArgumentException("Can't find R.attr class. Is \"${envConstants.appId}\" correct package name?")
            val setAttrsFun = FunSpec
                    .builder("setAttrs")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("context", contextClass)
                    .addParameter("attrs", ClassName("android.util", "AttributeSet").asNullable())
                    .addStatement("if (attrs == null) return")
                    .addStatement("val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.$viewClassName)")

            annotation.attrs.forEach {
                val fieldName = resolveIntFieldName(envConstants.packageAttrTypeElement, it.reference)
                        ?: throw IllegalArgumentException("Can't resolve name for ${it.reference} in class ${envConstants.packageAttrTypeElement.qualifiedName}")
                val propertyName = if (it.fieldName.isBlank()) fieldName else it.fieldName
                val propertyForAttr = PropertySpec
                        .builder(propertyName, it.type.fieldClass())
                        .mutable()
                        .initializer(it.type.initValue())
                if (inputProperties.find { it.name == propertyName } != null && inputModelProperty != null) {
                    propertyForAttr.setter(FunSpec
                            .setterBuilder()
                            .addParameter("value", it.type.fieldClass())
                            .addStatement("%1N = %1N.copy($propertyName = value)", inputModelProperty)
                            .build()
                    )
                }
                viewClass.addProperty(propertyForAttr.build())

                setAttrsFun.addStatement("$propertyName = styleAttrs.${it.type.loadCode("R.styleable.${viewClassName}_$propertyName")}")
            }

            setAttrsFun.addStatement("styleAttrs.recycle()")
            viewClass.addFunction(setAttrsFun.build())

        }

        viewClassFile.addType(viewClass.build())

        viewClassFile.build().writeTo(File(generationPath))

    }

    private fun generateDataClass(pack: String, className: String, inputProperties: List<PropertyData>): Pair<ClassName, TypeSpec?> {
        val inputDataClassType = ClassName(pack, className)
        val inputDataClass = TypeSpec
                .classBuilder(inputDataClassType)
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

        if (inputDataClass != null) {
            FileSpec
                    .builder(pack, className)
                    .addType(inputDataClass)
                    .build()
                    .writeTo(File(generationPath))
        }
        return inputDataClassType to inputDataClass
    }

    private fun resolveIntFieldName(typeElement: TypeElement, value: Int): String? {
        return typeElement
                .enclosedElements
                .firstOrNull { it.kind == ElementKind.FIELD && (it as? VariableElement)?.constantValue == value }
                ?.simpleName
                ?.toString()
    }

}
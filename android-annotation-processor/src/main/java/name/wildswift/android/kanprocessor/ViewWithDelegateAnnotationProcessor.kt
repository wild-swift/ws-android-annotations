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
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate
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
        viewClass.addProperty(PropertySpec.builder("delegate", delegateClass).addModifiers(KModifier.PRIVATE).initializer("%T()", delegateClass).build())

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
                viewClass.addProperty(PropertySpec.builder(propertyName, it.type.fieldClass()).mutable().initializer(it.type.initValue()).build())
                setAttrsFun.addStatement("$propertyName = styleAttrs.${it.type.loadCode("R.styleable.${viewClassName}_$propertyName")}")
            }

            setAttrsFun.addStatement("styleAttrs.recycle()")
            viewClass.addFunction(setAttrsFun.build())

        }

        viewClassFile.addType(viewClass.build())

        val file = viewClassFile.build()

        file.writeTo(File(generationPath))
    }

    private fun resolveIntFieldName(typeElement: TypeElement, value: Int): String? {
        return typeElement
                .enclosedElements
                .firstOrNull { it.kind == ElementKind.FIELD && (it as? VariableElement)?.constantValue == value }
                ?.simpleName
                ?.toString()
    }

}
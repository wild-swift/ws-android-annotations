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
package name.wildswift.lib.androidkotlinprocessor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import name.wildswift.lib.androidkotlinannotations.ViewWithModel
import name.wildswift.lib.androidkotlinprocessor.utils.toViewResourceName
import java.io.File
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

/**
 * Created by swift
 */
@SupportedAnnotationTypes("name.wildswift.lib.androidkotlinannotations.ViewWithModel")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ViewWithModelAnnotationProcessor : KotlinAbstractProcessor() {
    override val tmpFileName: String = "__V"

    private val useGoogleArch by lazy {
        processingEnv.options["use.google.arch"]?.equals("true") ?: false
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val appId = processingEnv.options["application.id"]
                ?: throw IllegalArgumentException("Argument \"application.id\" is not set.")
        val envConstants = ProcessingEnvConstants(
                appId = appId,
                packageLayoutsTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.layout")
                        ?: throw IllegalArgumentException("Can't find R.layout class. Is \"$appId\" correct package name?"),
                packageIdsTypeElement = processingEnv.elementUtils.getTypeElement("$appId.R.id")
                        ?: throw IllegalArgumentException("Can't find R.layout class. Is \"$appId\" correct package name?")
        )
        roundEnv.getElementsAnnotatedWith(ViewWithModel::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException("ViewWithModel may be only as class annotation. Can't apply to ${(it as? QualifiedNameable)?.qualifiedName ?: it.simpleName}")
            }
            (it as? TypeElement)?.apply {
                writeExtensionsFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(ViewWithModel::class.java), resolveKotlinVisibility(it), envConstants)
            }
        }
        return true
    }

    private fun writeExtensionsFile(className: String, pack: String, annotation: ViewWithModel, visibilityModifier: KModifier, envConstants: ProcessingEnvConstants) {
        val fileName = "_${className}Ext"

        val fileBuilder = FileSpec
                .builder(pack, fileName)


        val layoutName = annotation.layoutResource
                .let { if (it != 0) resolveIntFieldName(envConstants, it) else className.toViewResourceName() }
                ?: throw IllegalArgumentException("Can't resolve name for ${annotation.layoutResource} in class ${envConstants.packageLayoutsTypeElement.qualifiedName}")


        fileBuilder.addImport(envConstants.appId, "R")
        fileBuilder.addImport("android.view.View", "inflate")
        fileBuilder.addImport("kotlinx.android.synthetic.main.$layoutName", "view.*")

//        import kotlinx.android.synthetic.main.view_test.view.*

        fileBuilder.addFunction(
                FunSpec
                        .builder("initialize")
                        .addModifiers(visibilityModifier)
                        .receiver(ClassName(pack, className))
                        .addStatement("inflate(context, R.layout.$layoutName, this)")
                        .build()
        )

        val file = fileBuilder.build()

        file.writeTo(File(generationPath))
    }

    private fun resolveIntFieldName(processingEnvConstants: ProcessingEnvConstants, value: Int): String? {
        return processingEnvConstants
                .packageLayoutsTypeElement
                .enclosedElements
                .firstOrNull { it.kind == ElementKind.FIELD && (it as? VariableElement)?.constantValue == value }
                ?.simpleName
                ?.toString()
    }


}
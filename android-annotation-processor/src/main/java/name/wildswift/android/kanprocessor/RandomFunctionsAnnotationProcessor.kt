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
import name.wildswift.android.kannotations.RandomFunction
import name.wildswift.android.kannotations.RandomFunctionType
import name.wildswift.android.kannotations.RandomFunctions
import name.wildswift.android.kanprocessor.utils.resolveKotlinVisibility
import name.wildswift.android.kanprocessor.utils.safeGetType
import java.util.*
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


/**
 * Created by swift
 */
@SupportedAnnotationTypes("name.wildswift.android.kannotations.RandomFunctions", "name.wildswift.android.kannotations.RandomFunction")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class RandomFunctionsAnnotationProcessor : KotlinAbstractProcessor() {
    private val randomizer by lazy { Random() }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(RandomFunctions::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException()
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(RandomFunctions::class.java).value, resolveKotlinVisibility(it))
            }
        }
        roundEnv.getElementsAnnotatedWith(RandomFunction::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException()
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), arrayOf(it.getAnnotation(RandomFunction::class.java)), resolveKotlinVisibility(it))
            }
        }
        return true
    }

    private fun writeSourceFile(className: String, pack: String, annotations: Array<RandomFunction>, visibilityModifier: KModifier) {
        val fileName = "_${className}Randomizer"

        val fileBuilder = FileSpec
                .builder(pack, fileName)

        fileBuilder.addAnnotation(AnnotationSpec.builder(Suppress::class.asTypeName())
                .addMember("\"NOTHING_TO_INLINE\"")
                .build())

        annotations.forEach { annotation ->
            if (annotation.dictionary.isEmpty()) throw IllegalArgumentException("Dictionary can't be empty")
            annotation.dictionary.forEach {
                val importPack = it.split("\\.").let { it.subList(0, it.size - 1) }.fold(StringBuilder()) { cur, new -> if (cur.isNotEmpty()) cur.append(".").append(new) else cur.append(new) }.toString()
                val importName = it.split("\\.").last()
                if (importPack.isNotEmpty()) {
                    fileBuilder.addImport(importPack, importName)
                }
            }
            if (annotation.type == RandomFunctionType.boolCheck) {
                (1..annotation.count).forEach {
                    val selectedFunction = randomizer
                            .nextInt(annotation.dictionary.size)
                            .let {
                                annotation.dictionary[it].split("\\.").last()
                            }
                    val funSpec = FunSpec.builder("${annotation.perfix}$it")

                    annotation.parameters.forEach { paramSpec ->
                        funSpec.addParameter(paramSpec.name, paramSpec
                                .safeGetType { this.type }
                                .copy(nullable = paramSpec.nullable)
                        )
                    }



                    funSpec.receiver(ClassName(pack, className))
                            .returns(Boolean::class.java)
                            .addModifiers(visibilityModifier)
                            .addModifiers(KModifier.INLINE)
                            .addStatement("return $selectedFunction(${annotation.parameters.joinToString { it.name }})")

                    fileBuilder.addFunction(funSpec.build())

                }
            }
        }


        val file = fileBuilder.build()

        file.writeTo(generationPath)
    }
}
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
import name.wildswift.android.kannotations.ActivityField
import name.wildswift.android.kannotations.ActivityFields
import name.wildswift.android.kanprocessor.utils.bundleClass
import name.wildswift.android.kanprocessor.utils.toScreamingCase
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.tools.Diagnostic


/**
 * Created by swift
 */
@SupportedAnnotationTypes("name.wildswift.android.kannotations.ActivityFields", "name.wildswift.android.kannotations.ActivityField")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ActivityFieldsAnnotationProcessor : KotlinAbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(ActivityFields::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException()
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(ActivityFields::class.java).value, resolveKotlinVisibility(it))
            }
        }
        roundEnv.getElementsAnnotatedWith(ActivityField::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                throw IllegalArgumentException()
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), arrayOf(it.getAnnotation(ActivityField::class.java)), resolveKotlinVisibility(it))
            }
        }
        return true
    }

    private fun writeSourceFile(className: String, pack: String, annotations: Array<ActivityField>, visibilityModifier: KModifier) {
        val fileName = "_${className}Extension"
        val intentBuilderClassName = "${className}IntentBuilder"

        val fileBuilder = FileSpec
                .builder(pack, fileName)
                .addImport("android.os", "Build")
                .addImport("name.wildswift.android.kannotations.util", "ExtrasFieldLoader")

        val intentBuilderClassBuilder = TypeSpec
                .classBuilder(intentBuilderClassName)
                .addProperty(PropertySpec.builder("context", ClassName("android.content", "Context"), KModifier.PRIVATE).build())
                .addProperty(PropertySpec.builder("intent", ClassName("android.content", "Intent"), KModifier.PRIVATE).build())
                .addFunction(FunSpec
                        .constructorBuilder()
                        .addParameter(ParameterSpec
                                .builder("context", ClassName("android.content", "Context"))
                                .build())
                        .addStatement("this.context = context")
                        .addStatement("this.intent = Intent(context, $className::class.java)")
                        .build())
                .addFunction(FunSpec
                        .builder("startForResultWithOptions")
                        .addParameter(ParameterSpec.builder("requestCode", Int::class).build())
                        .addParameter(ParameterSpec.builder("options", bundleClass.copy(nullable = true)).build())
                        .addCode("        if (context is Activity) {\n" +
                                "            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {\n" +
                                "                context.startActivityForResult(intent, requestCode, options)\n" +
                                "            } else {\n" +
                                "                context.startActivityForResult(intent, requestCode)\n" +
                                "            }\n" +
                                "        } else {\n" +
                                "            context.startActivity(intent)\n" +
                                "        }\n")
                        .build())
                .addFunction(FunSpec
                        .builder("startForResult")
                        .addParameter(ParameterSpec.builder("requestCode", Int::class).build())
                        .addStatement("startForResultWithOptions(requestCode, null)")
                        .build())
                .addFunction(FunSpec
                        .builder("startWithOptions")
                        .addParameter(ParameterSpec.builder("options", bundleClass.copy(nullable = true)).build())
                        .addStatement("startForResultWithOptions(-1, options)")
                        .build())
                .addFunction(FunSpec
                        .builder("start")
                        .addStatement("startForResult(-1)")
                        .build())
                .addFunction(FunSpec
                        .builder("loadFromHistory")
                        .returns(ClassName(pack, intentBuilderClassName))
                        .addStatement("intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)")
                        .addStatement("return this")
                        .build())

        annotations.forEach { fieldSpec ->
            try {
                val name = fieldSpec.name
                val getterName = "_get${name[0].toUpperCase() + name.substring(1)}"
                val nameScreamingCase = name.toScreamingCase()
                val extraName = "${nameScreamingCase}_EXTRA"

                val propertyType = try {
                    val simpleName = fieldSpec.type.simpleName!!
                    val packageName = fieldSpec.type.qualifiedName!!.let { it.substring(0, it.length - simpleName.length - 2) }
                    ClassName(packageName, simpleName)
                } catch (mte: MirroredTypeException) {
                    mte.typeMirror.asTypeName()
                }
                        .let { if (it.toString() == "java.lang.String") ClassName("kotlin", "String") else it }
                        .let { if (fieldSpec.nullable) it.copy(nullable = true) else it }
                        .let { it as ClassName }


                fileBuilder.addProperty(PropertySpec.builder(extraName, String::class).mutable(false).initializer("\"$name\"").addModifiers(KModifier.PRIVATE).build())
                fileBuilder.addFunction(FunSpec
                        .builder(getterName)
                        .addModifiers(KModifier.PRIVATE)
                        .receiver(ClassName(pack, className))
                        .returns(propertyType)
                        .addStatement("return intent.extras?.get($extraName) as${if (fieldSpec.nullable) "?" else ""} ${(propertyType.copy(nullable = false) as ClassName).simpleName}")
                        .build())

                fileBuilder.addProperty(PropertySpec
                        .builder(fieldSpec.name, propertyType)
                        .receiver(ClassName(pack, className))
                        .addModifiers(visibilityModifier)
                        .delegate(CodeBlock.of("ExtrasFieldLoader { this.$getterName() }"))
                        .build())

                intentBuilderClassBuilder.addFunction(FunSpec
                        .builder(fieldSpec.name)
                        .returns(ClassName(pack, intentBuilderClassName))
                        .addParameter(ParameterSpec.builder(fieldSpec.name, propertyType).build())
                        .addStatement("intent.putExtra($extraName, ${fieldSpec.name})")
                        .addStatement("return this")
                        .build())

            } catch (e: Exception) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                e.printStackTrace(PrintStream(byteArrayOutputStream))
                fileBuilder.addComment(byteArrayOutputStream.toString())
            }
        }

        intentBuilderClassBuilder.addFunction(FunSpec
                .builder("build")
                .returns(ClassName("android.content", "Intent"))
                .addStatement("return intent")
                .build())


        val file = fileBuilder
                .addType(intentBuilderClassBuilder
                        .build())
                .addFunction(FunSpec
                        .builder(className[0].toLowerCase() + className.substring(1))
                        .receiver(ClassName("android.app", "Activity"))
                        .returns(ClassName(pack, intentBuilderClassName))
                        .addStatement("return $intentBuilderClassName(this)")
                        .build())
                .build()

        file.writeTo(generationPath)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
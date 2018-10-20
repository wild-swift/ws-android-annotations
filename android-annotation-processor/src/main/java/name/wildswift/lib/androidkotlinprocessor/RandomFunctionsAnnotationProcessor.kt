package name.wildswift.lib.androidkotlinprocessor


import com.squareup.kotlinpoet.*
import name.wildswift.lib.androidkotlinannotations.RandomFunction
import name.wildswift.lib.androidkotlinannotations.RandomFunctionType
import name.wildswift.lib.androidkotlinannotations.RandomFunctions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import javax.annotation.processing.AbstractProcessor
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
@SupportedAnnotationTypes("name.wildswift.lib.androidkotlinannotations.RandomFunctions", "name.wildswift.lib.androidkotlinannotations.RandomFunction")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
class RandomFunctionsAnnotationProcessor : AbstractProcessor() {
    val generationPath: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        //        val result = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
//        if (result != null) return@lazy result!!

        try {
            val sourceFile = processingEnv.filer.createSourceFile("__R")
            val file = File(sourceFile.toUri()).parent
            sourceFile.delete()
            return@lazy file
        } catch (e: Exception) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            e.printStackTrace(PrintStream(byteArrayOutputStream))
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, byteArrayOutputStream.toString())
            throw e
        }
    }

    private val randomizer by lazy { Random() }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(RandomFunctions::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                return true
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(RandomFunctions::class.java).value)
            }
        }
        roundEnv.getElementsAnnotatedWith(RandomFunction::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                return true
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), arrayOf(it.getAnnotation(RandomFunction::class.java)))
            }
        }
        return true
    }

    private fun writeSourceFile(className: String, pack: String, annotations: Array<RandomFunction>) {
        val fileName = "_${className}Randomizer"

        val fileBuilder = FileSpec
                .builder(pack, fileName)

        annotations.forEach { annotation ->
            if (annotation.dictionary.isEmpty()) throw IllegalArgumentException("Dictionary can't be empty")
            annotation.dictionary.forEach {
                val importPack = it.split("\\.").let { it.subList(0, it.size - 1) }.fold(StringBuilder()) { cur, new -> if (cur.isNotEmpty()) cur.append(".").append(new) else cur.append(new) }.toString()
                val importName = it.split("\\.").last()
                if (importPack.isNotEmpty()) {
                    fileBuilder.addStaticImport(importPack, importName)
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
                        val type =
                                try {
                                    paramSpec.type.asTypeName()
                                } catch (mte: MirroredTypeException) {
                                    mte.typeMirror.asTypeName()
                                }
                                        .let { if (paramSpec.nullable) it.asNullable() else it.asNonNullable() }

                        funSpec.addParameter(paramSpec.name, type)
                    }



                    funSpec.receiver(ClassName(pack, className))
                            .returns(Boolean::class.java)
                            .addModifiers(KModifier.INLINE)
                            .addStatement("return $selectedFunction(${annotation.parameters.joinToString { it.name }})")

                    fileBuilder.addFunction(funSpec.build())

                }
            }
        }


        val file = fileBuilder.build()

        file.writeTo(File(generationPath))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
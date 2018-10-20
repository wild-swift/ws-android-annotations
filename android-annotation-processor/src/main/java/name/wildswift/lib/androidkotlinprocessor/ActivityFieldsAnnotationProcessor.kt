package name.wildswift.lib.androidkotlinprocessor


import com.squareup.kotlinpoet.*
import name.wildswift.lib.androidkotlinannotations.ActivityFields
import name.wildswift.lib.androidkotlinprocessor.utils.toScreamingCase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
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
@SupportedAnnotationTypes("name.wildswift.lib.androidkotlinannotations.ActivityFields")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
class ActivityFieldsAnnotationProcessor : AbstractProcessor() {
    val generationPath: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
//        val result = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
//        if (result != null) return@lazy result!!

        try {
            val sourceFile = processingEnv.filer.createSourceFile("__T")
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

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(ActivityFields::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                return true
            }
            (it as? TypeElement)?.apply {
                writeSourceFile(simpleName.toString(), processingEnv.elementUtils.getPackageOf(it).toString(), it.getAnnotation(ActivityFields::class.java))
            }
        }
        return true
    }

    private fun writeSourceFile(className: String, pack: String, annotation: ActivityFields) {
        val fileName = "_${className}Extension"
        val intentBuilderClassName = "${className}IntentBuilder"

        val fileBuilder = FileSpec
                .builder(pack, fileName)
                .addAliasedImport(ClassName("android.support.v4.app", "ActivityCompat"), "ActivityCompat")

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
                        .addParameter(ParameterSpec.builder("options", ClassName("android.os", "Bundle").asNullable()).build())
                        .addCode("        if (context is Activity) {\n" +
                                "            ActivityCompat.startActivityForResult(context, intent, requestCode, options)\n" +
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
                        .addParameter(ParameterSpec.builder("options", ClassName("android.os", "Bundle").asNullable()).build())
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

        annotation.value.forEach { fieldSpec ->
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
                        .let { if (fieldSpec.nullable) it.asNullable() else it }
                        .let { it as ClassName }


                fileBuilder.addProperty(PropertySpec.builder(extraName, String::class).mutable(false).initializer("\"$name\"").addModifiers(KModifier.PRIVATE).build())
                fileBuilder.addFunction(FunSpec
                        .builder(getterName)
                        .addModifiers(KModifier.PRIVATE)
                        .receiver(ClassName(pack, className))
                        .returns(propertyType)
                        .addStatement("return intent.extras?.get($extraName) as${if (fieldSpec.nullable) "?" else ""} ${propertyType.asNonNullable().simpleName()}")
                        .build())

                fileBuilder.addProperty(PropertySpec
                        .builder("$className.${fieldSpec.name}", propertyType)
                        .delegate(CodeBlock.of("name.wildswift.lib.util.ExtrasFieldLoader { this.$getterName() }" ))
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

        file.writeTo(File(generationPath))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
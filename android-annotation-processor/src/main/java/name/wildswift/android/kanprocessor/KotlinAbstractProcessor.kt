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

import com.squareup.kotlinpoet.KModifier
import kotlinx.metadata.ClassName
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.util.SimpleAnnotationValueVisitor7

/**
 * Created by swift
 */
abstract class KotlinAbstractProcessor : AbstractProcessor() {
    val generationPath: File by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        File(processingEnv.options["kapt.kotlin.generated"] ?: ".")
    }
    private val intArrayVisitor = object : SimpleAnnotationValueVisitor7<IntArray, Any?>() {
        override fun visitArray(vals: MutableList<out AnnotationValue>, p: Any?) = vals.mapNotNull { it.value as? Int }.toIntArray()
    }
    private val stringArrayVisitor = object : SimpleAnnotationValueVisitor7<Array<String>, Any?>() {
        override fun visitArray(vals: MutableList<out AnnotationValue>, p: Any?) = vals.mapNotNull { it.value as? String }.toTypedArray()
    }

    protected fun resolveKotlinVisibility(it: TypeElement): KModifier {
        val read = readKotlinMetadata(it)
        var visibilityModifier = KModifier.PUBLIC
        read?.accept(object : KmClassVisitor() {
            override fun visit(flags: Flags, name: ClassName) {
                if (Flag.IS_INTERNAL(flags)) {
                    visibilityModifier = KModifier.INTERNAL
                }
                if (Flag.IS_PROTECTED(flags)) {
                    visibilityModifier = KModifier.PROTECTED
                }
                if (Flag.IS_PRIVATE(flags)) {
                    visibilityModifier = KModifier.PRIVATE
                }
            }
        })
        return visibilityModifier
    }

    protected fun readKotlinMetadata(it: TypeElement): KotlinClassMetadata.Class? {
        var kind: Int? = null
        var metadataVersion: IntArray? = null
        var bytecodeVersion: IntArray? = null
        var data1: Array<String>? = null
        var data2: Array<String>? = null
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int? = null


        it.annotationMirrors
                .filter { it.annotationType.asElement().simpleName.contentEquals("Metadata") }
                .flatMap { it.elementValues.entries }
                .forEach { (key, value) ->
                    when (key.simpleName.toString()) {
                        "k" -> kind = value.value as? Int
                        "mv" -> metadataVersion = value.accept(intArrayVisitor, null)
                        "bv" -> bytecodeVersion = value.accept(intArrayVisitor, null)
                        "d1" -> data1 = value.accept(stringArrayVisitor, null)
                        "d2" -> data2 = value.accept(stringArrayVisitor, null)
                        "es" -> extraString = value.value as? String
                        "pn" -> packageName = value.value as? String
                        "ei" -> extraInt = value.value as? Int
                    }
                }

        val header = KotlinClassHeader(
                kind = kind,
                metadataVersion = metadataVersion,
                bytecodeVersion = bytecodeVersion,
                data1 = data1,
                data2 = data2,
                extraString = extraString,
                packageName = packageName,
                extraInt = extraInt
        )
        val read = KotlinClassMetadata.read(header) as? KotlinClassMetadata.Class
        return read
    }
}
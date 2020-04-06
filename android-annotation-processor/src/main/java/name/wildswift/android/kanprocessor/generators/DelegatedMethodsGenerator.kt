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

package name.wildswift.android.kanprocessor.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.ExecutableElement

object DelegatedMethodsGenerator {
    fun buildDelegatedMethods(delegatedMethods: List<ExecutableElement>, delegateProperty: PropertySpec): List<FunSpec> {
        return delegatedMethods
                .map { method ->
                    val name = method.simpleName.toString().split("$").firstOrNull().orEmpty()
                    val funBuilder = FunSpec.builder(name)
                            .returns(method.returnType.asTypeName())
                            .addCode("""
                            |    return %N.$name(${method.parameters.joinToString { it.simpleName.toString() }})
                            |
                        """.trimMargin(), delegateProperty)
                    method.parameters.forEach { funBuilder.addParameter(it.simpleName.toString(), it.asType().asTypeName().let { if (it.toString() == "java.lang.String") ClassName("kotlin", "String") else it }) }
                    funBuilder.build()
                }
    }

}
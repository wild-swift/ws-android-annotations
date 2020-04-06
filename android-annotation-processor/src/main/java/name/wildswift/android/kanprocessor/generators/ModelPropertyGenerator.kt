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

import com.squareup.kotlinpoet.*
import name.wildswift.android.kannotations.FieldRWType
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kanprocessor.utils.resolveSetter
import kotlin.properties.Delegates

object ModelPropertyGenerator {
    fun internalModelProperty(modelType: ClassName, fieldsToSet: List<ViewField>, delegateProperty: PropertySpec): PropertySpec {
        return PropertySpec.builder("intModel", modelType)
                .mutable()
                .addModifiers(KModifier.PRIVATE)
                .delegate(
                        CodeBlock.builder()
                                .add("""
                                                    |%1T.observable(%2T())·{·_,·oldValue,·newValue·->
                                                    |··if (oldValue == newValue) return@observable
                                                    |
                                                """.trimMargin(), Delegates::class.asTypeName(), modelType)
                                .add(fieldsToSet
                                        .filter { it.childName.isNotEmpty() }
                                        .filter { it.rwType != FieldRWType.readOnly }
                                        .joinToString(separator = "\n") {
                                            "··if·(oldValue.${it.name}·!=·newValue.${it.name})·${it.childName}.${it.resolveSetter("newValue.${it.name}")}"
                                        }
                                )
                                .add("""
                                                    |
                                                    |  %N.onNewInternalState(newValue)
                                                    |}
                                                """.trimMargin(), delegateProperty)
                                .build()
                )
                .build()
    }

}
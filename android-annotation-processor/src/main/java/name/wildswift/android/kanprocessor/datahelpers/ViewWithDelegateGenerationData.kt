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

package name.wildswift.android.kanprocessor.datahelpers

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import name.wildswift.android.kannotations.*
import name.wildswift.android.kanprocessor.utils.resolveKotlinVisibility
import name.wildswift.android.kanprocessor.utils.toViewResourceName
import name.wildswift.android.kanprocessor.utils.validateCorrectSetup
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

data class ViewWithDelegateGenerationData(
        val rootAnnotation: ViewWithDelegate,
        val attrs: List<ViewAttribute>,
        val events: List<ViewEvent>,
        val basicFields: List<ViewField>,
        val collectionFields: List<CollectionViewField>,
        val delegatedMethods: List<ExecutableElement>,
        val visibilityModifier: KModifier,
        val delegateType: ClassName,
        val generateViewType: ClassName,
        val internalModelType: ClassName,
        val externalModelType: ClassName,
        val layoutName: String,
        val wrapAdapterMapping: List<Pair<WrapAdapter, ExecutableElement>>
) {
    companion object {
        fun from(typeElement: TypeElement, env: ProcessingEnvironment): ViewWithDelegateGenerationData {
            val className = typeElement.simpleName.toString()
            val packageName = env.elementUtils.getPackageOf(typeElement).toString()
            val metadata = typeElement.getAnnotation(ViewWithDelegate::class.java)
            val attrs = typeElement.getAnnotationsByType(Attributes::class.java).flatMap { it.value.asIterable() } + typeElement.getAnnotationsByType(ViewAttribute::class.java)
            val events = typeElement.getAnnotationsByType(Events::class.java).flatMap { it.value.asIterable() } + typeElement.getAnnotationsByType(ViewEvent::class.java)
            val fields = typeElement.getAnnotationsByType(Fields::class.java).flatMap { it.value.asIterable() } + typeElement.getAnnotationsByType(ViewField::class.java)
            val listFields = typeElement.getAnnotationsByType(CollectionsFields::class.java).flatMap { it.value.asIterable() } + typeElement.getAnnotationsByType(CollectionViewField::class.java)
            val visibilityModifier = resolveKotlinVisibility(typeElement)
            val delegatedMethods = typeElement.enclosedElements.filter { it.getAnnotation(Delegated::class.java) != null }.filterIsInstance<ExecutableElement>()
            val wrapAdapterMapping = typeElement.enclosedElements.filterIsInstance<ExecutableElement>().mapNotNull {
                val annotation = it.getAnnotation(WrapAdapter::class.java)
                if (annotation != null)
                    annotation to it
                else
                    null
            }

            val viewClassName = metadata.name.takeIf { it.isNotBlank() }
                    ?: className.let { if (it.endsWith("Delegate")) it.substring(0, it.length - "Delegate".length) else null }
                    ?: throw IllegalArgumentException("Class name must be specified or delegate must ends with 'Delegate' suffix. Class $packageName.$className")

            val layoutName = if (metadata.haveChild) metadata.layoutResourceName.takeIf { it.isNotEmpty() }
                    ?: viewClassName.toViewResourceName() else ""

            if (fields.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Fields not configured properly for class $packageName.$className")
            if (events.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("Events not configured properly for class $packageName.$className")
            if (listFields.any { !it.validateCorrectSetup() }) throw IllegalArgumentException("List fields not configured properly for class $packageName.$className")

            val delegateType = ClassName(packageName, className)
            val generateViewType = ClassName(packageName, viewClassName)
            val internalModelType = ClassName(packageName, "${viewClassName}IntState")
            val externalModelType = ClassName(packageName, "${viewClassName}Model")

            return ViewWithDelegateGenerationData(
                    rootAnnotation = metadata,
                    attrs = attrs,
                    events = events,
                    basicFields = fields,
                    collectionFields = listFields,
                    delegatedMethods = delegatedMethods,
                    visibilityModifier = visibilityModifier,
                    delegateType = delegateType,
                    generateViewType = generateViewType,
                    internalModelType = internalModelType,
                    externalModelType = externalModelType,
                    layoutName = layoutName,
                    wrapAdapterMapping = wrapAdapterMapping
            )
        }
    }
}
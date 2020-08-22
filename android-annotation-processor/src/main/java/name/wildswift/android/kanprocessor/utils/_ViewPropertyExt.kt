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

package name.wildswift.android.kanprocessor.utils

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import name.wildswift.android.kannotations.ViewProperty

fun ViewProperty.getDefaultValue(): Pair<String, TypeName?> = when (this) {
    ViewProperty.none -> "" to null
    ViewProperty.text -> "\"\"" to null
    ViewProperty.visibility -> "%T.VISIBLE" to viewClass
    ViewProperty.textColor -> "0xFFFFFFFF.toInt()" to null
    ViewProperty.checked -> "false" to null
    ViewProperty.timePickerHour -> "-1" to null
    ViewProperty.timePickerMinute -> "-1" to null
    ViewProperty.imageResource -> "0" to null
    ViewProperty.imageDrawable -> "null" to null
    ViewProperty.backgroundResource -> "0" to null
    ViewProperty.backgroundColor -> "0" to null
    ViewProperty.backgroundDrawable -> "null" to null
    ViewProperty.radioSelect -> "null" to null
    ViewProperty.alpha -> "1.0f" to null
    ViewProperty.enable -> "true" to null
    ViewProperty.selected -> "false" to null
    ViewProperty.elevation -> "0.0f" to null
}

fun ViewProperty.getListenerGroup() = when (this) {
    ViewProperty.none -> arrayOf()
    ViewProperty.text -> arrayOf(ViewProperty.text)
    ViewProperty.checked -> arrayOf(ViewProperty.checked)
    ViewProperty.timePickerHour -> arrayOf(ViewProperty.timePickerHour, ViewProperty.timePickerMinute)
    ViewProperty.timePickerMinute -> arrayOf(ViewProperty.timePickerHour, ViewProperty.timePickerMinute)
    ViewProperty.radioSelect -> arrayOf(ViewProperty.radioSelect)
    ViewProperty.visibility -> arrayOf()
    ViewProperty.textColor -> arrayOf()
    ViewProperty.imageResource -> arrayOf()
    ViewProperty.imageDrawable -> arrayOf()
    ViewProperty.backgroundResource -> arrayOf()
    ViewProperty.backgroundColor -> arrayOf()
    ViewProperty.backgroundDrawable -> arrayOf()
    ViewProperty.alpha -> arrayOf()
    ViewProperty.enable -> arrayOf()
    ViewProperty.selected -> arrayOf()
    ViewProperty.elevation -> arrayOf()
}

// TODO make more pretty
fun ViewProperty.buildListener(childName: String, body: String, bodyCodeProperty1: PropertySpec, bodyCodeProperty2: PropertySpec, bodyCodeProperty3: FunSpec?, bodyCodeProperty4: PropertySpec, codeBlockBuilder: CodeBlock.Builder) {
    when (this) {
        ViewProperty.text -> {
            codeBlockBuilder.add("""
                |$childName.addTextChangedListener(object : %5T {
                |    override fun afterTextChanged(text: %6T) {
                |    $body
                |    }
                |
                |    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                |    
                |    }
                |   
                |    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                |   
                |    }
                |})
                |
            """.trimMargin(), bodyCodeProperty1, bodyCodeProperty2, bodyCodeProperty3, bodyCodeProperty4, textWatcherClass, editableClass)
        }
        ViewProperty.checked -> {
            codeBlockBuilder.add("""
                    |$childName.setOnCheckedChangeListener { _, isChecked ->
                    |    $body
                    |}
                    |
                """.trimMargin(), bodyCodeProperty1, bodyCodeProperty2, bodyCodeProperty3, bodyCodeProperty4)
        }
        ViewProperty.timePickerHour, ViewProperty.timePickerMinute -> {
            codeBlockBuilder.add("""
                    |$childName.setOnTimeChangedListener { _, hour, minute ->
                    |$body
                    |}
                    |
                """.trimMargin(), bodyCodeProperty1, bodyCodeProperty2, bodyCodeProperty3, bodyCodeProperty4)
        }
        ViewProperty.radioSelect -> {
            codeBlockBuilder.add("""
                |$childName.setOnCheckedChangeListener { _, checkedIdRaw ->
                |     val checkedId = checkedIdRaw.takeIf { it != -1 }
                |$body     
                |}
                | 
            """.trimMargin(), bodyCodeProperty1, bodyCodeProperty2, bodyCodeProperty3, bodyCodeProperty4)
        }
        else -> {

        }
    }
}

fun ViewProperty.getListenerPropertyName() = when (this) {
    ViewProperty.text -> "text.toString()"
    ViewProperty.checked -> "isChecked"
    ViewProperty.timePickerHour -> "hour"
    ViewProperty.timePickerMinute -> "minute"
    ViewProperty.radioSelect -> "checkedId"
    else -> ""
}
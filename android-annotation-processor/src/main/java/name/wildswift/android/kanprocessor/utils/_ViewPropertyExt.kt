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

package name.wildswift.android.kanprocessor.utils

import com.squareup.kotlinpoet.asTypeName
import name.wildswift.android.kannotations.ViewProperty

/**
 * Created by swift
 */
fun ViewProperty.getType() = when (this) {
    ViewProperty.none -> Any::class.asTypeName()
    ViewProperty.text -> String::class.asTypeName()
    ViewProperty.visibility -> Int::class.asTypeName()
    ViewProperty.textColor -> Int::class.asTypeName()
    ViewProperty.checked -> Boolean::class.asTypeName()
}

fun ViewProperty.getDefaultValue() = when (this) {
    ViewProperty.none -> ""
    ViewProperty.text -> "\"\""
// it is better to use View.VISIBLE, but this is GENERATED CODE!!!
    ViewProperty.visibility -> "0"
    ViewProperty.textColor -> "0xFFFFFFFF.toInt()"
    ViewProperty.checked -> "false"
}
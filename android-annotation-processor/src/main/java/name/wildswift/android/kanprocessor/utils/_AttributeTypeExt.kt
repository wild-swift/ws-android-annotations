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

import name.wildswift.android.kannotations.AttributeType
import name.wildswift.android.kannotations.AttributeType.*

/**
 * Created by swift
 */
fun AttributeType.fieldClass() = when(this) {
    string -> String::class
    color -> Int::class
    enum_ -> Int::class
}

fun AttributeType.initValue() = when(this) {
    string -> "\"\""
    color -> "0xFFFFFFFF.toInt()"
    enum_ -> "0"
}

fun AttributeType.loadCode(indexRef: String) = when(this) {
    string -> "getString($indexRef)"
    color -> "getColor($indexRef, ${initValue()})"
    enum_ -> "getInteger($indexRef, 0)"
}

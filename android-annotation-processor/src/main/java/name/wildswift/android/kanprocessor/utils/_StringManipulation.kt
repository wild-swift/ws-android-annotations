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

/**
 * Created by swift
 */
fun String.toScreamingCase() = this
        .toCharArray()
        .fold("") { prev, v ->
            when {
                prev.length == 0 -> "" + v
                v.isUpperCase() -> prev + "_" + v
                !prev.last().isDigit() && v.isDigit() -> prev + "_" + v
                prev.last().isDigit() && !v.isDigit() -> prev + "_" + v
                else -> prev + v
            }
        }
        .toUpperCase()
        .let {
            var result = it
            while (result.startsWith("_")) result = result.substring(1)
            result
        }

fun String.toViewResourceName() = this
        .let {
            val index = it.indexOf("View")
            if (index >= 0) {
                "View${it.substring(0, index)}${it.substring(index + 4)}"
            } else {
                "View$it"
            }
        }
        .toScreamingCase()
        .toLowerCase()
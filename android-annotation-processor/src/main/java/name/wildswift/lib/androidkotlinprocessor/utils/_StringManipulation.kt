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
package name.wildswift.lib.androidkotlinprocessor.utils

/**
 * Created by swift
 */
fun String.toScreamingCase() = this
        .map {
            if (it.isUpperCase()) "_$it" else "" + it.toUpperCase()
        }
        .fold("") { prev, v ->
            prev + v
        }
        .let {
            var result = it
            while (result.startsWith("_")) result = result.substring(1)
            result
        }
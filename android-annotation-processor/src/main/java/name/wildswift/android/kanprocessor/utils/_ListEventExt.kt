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

import name.wildswift.android.kannotations.ListEvent
import name.wildswift.android.kannotations.ViewListener

/**
 * Created by swift
 */
fun ListEvent.resolveListener(statement: String) =
        if (listener != ViewListener.none) {
            when (listener) {
                ViewListener.none -> ""
                ViewListener.onClick -> """
                    |setOnClickListener {
                    |    $statement
                    |}
                    |
                """.trimMargin()
            }
        } else {
            """
            |$listenerName = {
            |    $statement
            |}
            |
            """.trimMargin()
        }

fun ListEvent.validateCorrectSetup(): Boolean {
    if (listener == ViewListener.none && listenerName.isEmpty()) return false

    return true;
}
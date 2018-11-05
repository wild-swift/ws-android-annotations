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
package name.wildswift.testapp

import android.app.Activity
import android.os.Bundle
import name.wildswift.lib.androidkotlinannotations.ActivityField
import name.wildswift.lib.androidkotlinannotations.RandomFunction
import name.wildswift.lib.androidkotlinannotations.RandomFunctionParameter

@ActivityField(
        name = "id", type = Long::class, nullable = true
)
@RandomFunction(
        parameters = [
            RandomFunctionParameter(name = "context", type = Int::class, nullable = false)
        ],
        dictionary = [
            "name.wildswift.testapp.random1",
            "name.wildswift.testapp.random2",
            "name.wildswift.testapp.random3",
            "name.wildswift.testapp.random4",
            "name.wildswift.testapp.random5",
            "name.wildswift.testapp.random6",
            "name.wildswift.testapp.random7",
            "name.wildswift.testapp.random8",
            "name.wildswift.testapp.random9",
            "name.wildswift.testapp.random10"
        ]
)
internal class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

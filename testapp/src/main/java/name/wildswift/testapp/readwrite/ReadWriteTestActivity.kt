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
package name.wildswift.testapp.readwrite

import android.app.Activity
import android.os.Bundle
import android.util.Log
import name.wildswift.testapp.R

class ReadWriteTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_write_test)

        val testView = findViewById<TestReadWriteModesView>(R.id.testView)
        // check view model behavior
        testView.viewModel = TestReadWriteModesViewModel(
                count = 10, // must change value
                roText = "Test", // must be ignored
                check = true, // must be ignored
                radioSelect = R.id.vtrwmRadioCheck3, // must change value
                rwText = "Test 2" // must change value
        )

        check(testView.viewModel == TestReadWriteModesViewModel(
                count = 10,
                roText = "",
                check = false,
                radioSelect = R.id.vtrwmRadioCheck3,
                rwText = "Test 2"
        ))

        testView.count = 1
        testView.radioSelect = R.id.vtrwmRadioCheck1
        testView.rwText = ""

        // must be read only
//        testView.roText = ""
//        testView.check = true

        // must be expected
//        testView.title = ""

        testView.onCheckChanged = {
            Log.w("TestNotify", "onCheckChanged = $it")
        }
        testView.onRadioSelectChanged = {
            Log.w("TestNotify", "onRadioSelectChanged = $it")
        }
        testView.onRwTextChanged = {
            Log.w("TestNotify", "onRwTextChanged = $it")
        }

        // this fields must not be generated
//        testView.onCountChanged
//        testView.onTitleChanged
//        testView.onRoTextChanged

        testView.reset = {
            testView.viewModel = TestReadWriteModesViewModel(
                    count = testView.viewModel.count + 1, // must change value
                    roText = "Test", // must be ignored
                    check = true, // must be ignored
                    radioSelect = R.id.vtrwmRadioCheck3, // must change value
                    rwText = "Test 2" // must change value
            )
        }
    }
}

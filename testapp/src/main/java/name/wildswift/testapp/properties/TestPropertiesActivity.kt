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
package name.wildswift.testapp.properties

import android.app.Activity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_test_properties.*
import name.wildswift.testapp.R

class TestPropertiesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_properties)
        testView.viewModel = TestPropertiesViewModel(
                textProperty = "Name 1",
                visibilityProperty = View.INVISIBLE,
                textColorProperty = 0x80FFF6EA.toInt(),
                checkedProperty = true,
                timePickerHourProperty = 0,
                timePickerMinuteProperty = 0,
                imageResourceProperty = R.drawable.ic_face_res,
                imageDrawableProperty = getDrawable(R.drawable.ic_airplane_drawable),
                backgroundResourceProperty = R.drawable.ic_pie_chart_bg_res,
                backgroundColorProperty = 0xFF4BE0EE.toInt(),
                backgroundDrawableProperty = getDrawable(R.drawable.ic_plus_one_bg_drawable),
                radioSelectProperty = 0
        )
    }
}

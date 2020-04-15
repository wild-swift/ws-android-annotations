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

import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import name.wildswift.android.kannotations.Fields
import name.wildswift.android.kannotations.ViewField
import name.wildswift.android.kannotations.ViewProperty
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate
import name.wildswift.testapp.IdRNames

@ViewWithDelegate(
        parent = LinearLayout::class
)
@Fields(
        ViewField(name = "textProperty", byProperty = ViewProperty.text, childName = IdRNames.vtpEditText, activeChild = true),
        ViewField(name = "visibilityProperty", byProperty = ViewProperty.visibility, childName = IdRNames.vtpVisibility),
        ViewField(name = "textColorProperty", byProperty = ViewProperty.textColor, childName = IdRNames.vtpCheck),
        ViewField(name = "checkedProperty", byProperty = ViewProperty.checked, activeChild = true, childName = IdRNames.vtpCheck),
        ViewField(name = "timePickerHourProperty", byProperty = ViewProperty.timePickerHour, activeChild = true, childName = IdRNames.vtpTime),
        ViewField(name = "timePickerMinuteProperty", byProperty = ViewProperty.timePickerMinute, activeChild = true, childName = IdRNames.vtpTime),
        ViewField(name = "imageResourceProperty", byProperty = ViewProperty.imageResource, childName = IdRNames.vtpSrcRes),
        ViewField(name = "imageDrawableProperty", byProperty = ViewProperty.imageDrawable, childName = IdRNames.vtpSrcDrawable),
        ViewField(name = "backgroundResourceProperty", byProperty = ViewProperty.backgroundResource, childName = IdRNames.vtpBgRes),
        ViewField(name = "backgroundColorProperty", byProperty = ViewProperty.backgroundColor, childName = IdRNames.vtpBgColor),
        ViewField(name = "backgroundDrawableProperty", byProperty = ViewProperty.backgroundDrawable, childName = IdRNames.vtpBgDrawable),
        ViewField(name = "radioSelectProperty", byProperty = ViewProperty.radioSelect, activeChild = true)
)
class TestPropertiesViewDelegate(view: TestPropertiesView) : ViewDelegate<TestPropertiesView, TestPropertiesViewIntState>(view) {
    override fun setupView() {
        view.orientation = VERTICAL
    }
}
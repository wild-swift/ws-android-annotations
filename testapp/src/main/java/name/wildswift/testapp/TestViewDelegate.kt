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

import android.view.View
import android.widget.FrameLayout
import name.wildswift.android.kannotations.*
import name.wildswift.android.kannotations.interfaces.ViewDelegate

/**
 * Created by swift
 */
@ViewWithDelegate(
        parent = FrameLayout::class,
        layoutResourceName = LayoutRNames.view_test,
        attrs = [
            ViewAttribute(reference = AttrRNames.visibleColor, type = AttributeType.color),
            ViewAttribute(reference = AttrRNames.textIn, type = AttributeType.enum_)
        ],
        fields = [
            ViewField(childName = IdRNames.vtLabel, property = ViewProperty.text, name = "label"),
            ViewField(childName = IdRNames.vtLabel, property = ViewProperty.visibility, name = "visible", publicAccess = false),
            ViewField(childName = IdRNames.vtLabel, property = ViewProperty.textColor, name = "visibleColor"),
            ViewField(childName = IdRNames.vtCheck, property = ViewProperty.checked, name = "check")
        ]
)
class TestViewDelegate(view: TestView) : ViewDelegate<TestView, TestViewIntState>(view) {
    override fun setupView() {

    }

    override fun validateStateForNewInput(data: TestViewIntState): TestViewIntState {
        return data.copy(visible = if (data.check) View.VISIBLE else View.INVISIBLE)
    }

}
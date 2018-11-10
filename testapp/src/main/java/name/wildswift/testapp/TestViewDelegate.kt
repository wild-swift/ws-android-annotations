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

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import name.wildswift.android.kannotations.AttributeType
import name.wildswift.android.kannotations.ViewAttribute
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate

/**
 * Created by swift
 */
@ViewWithDelegate(
        parent = FrameLayout::class,
        layoutResource = R.layout.view_test,
        attrs = [
            ViewAttribute(reference = R.attr.visibleColor, type = AttributeType.color),
            ViewAttribute(reference = R.attr.textIn, type = AttributeType.enum_)
        ]
//        fields = [
//            ViewField(id = R.id.vtLabel, property = ViewProperty.text, name = "label"),
//            ViewField(id = R.id.vtLabel, property = ViewProperty.visibility, name = "visible"),
//            ViewField(id = R.id.vtLabel, property = ViewProperty.textColor, name = "color"),
//            ViewField(id = R.id.vtCheck, property = ViewProperty.checked, name = "check")
//        ]
)
class TestViewDelegate : ViewDelegate<TestView>() {
    override fun setupView(view: TestView) {

    }
//    private var internalState = TestInternalState()
//
//    var model: TestConfigModel = TestConfigModel()
//        set(value) {
//            field = value
//            internalState =
//        }


}

//data class TestInternalState(
//        val label: String,
//        val visible: Boolean,
//        val color: Int,
//        val check: Boolean
//)
//
//data class TestConfigModel(
//        val label: String = "",
//        val visible: Boolean = false,
//        val color: Int = 0xFFFFFFFF.toInt(),
//        val check: Boolean = false
//)
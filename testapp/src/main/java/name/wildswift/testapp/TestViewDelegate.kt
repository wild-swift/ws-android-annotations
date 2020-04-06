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
        layoutResourceName = LayoutRNames.view_test
)
@Events(
        ViewEvent(name = "onLabelClick", childName = IdRNames.vtLabel, listener = ViewListener.onClick)
)
@Attributes(
        ViewAttribute(reference = AttrRNames.visibleColor, type = AttributeType.color),
        ViewAttribute(reference = AttrRNames.textIn, type = AttributeType.enum_)
)
@Fields(
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.text, name = "label", rwType = FieldRWType.readWrite),
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.backgroundResource, name = "labelBGResource", rwType = FieldRWType.readWrite),
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.backgroundColor, name = "labelBGColor", rwType = FieldRWType.readWrite),
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.backgroundDrawable, name = "labelBGDrawable"),
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.visibility, name = "visible", publicAccess = false),
        ViewField(childName = IdRNames.vtLabel, property = ViewProperty.textColor, name = "visibleColor"),
        ViewField(childName = IdRNames.vtCheck, property = ViewProperty.checked, name = "check", rwType = FieldRWType.readWrite),
        ViewField(childName = IdRNames.vtImage, property = ViewProperty.imageDrawable, name = "imageAsDrawable"),
        ViewField(childName = IdRNames.vtImage, property = ViewProperty.imageResource, name = "imageAsResource"),
        ViewField(name = "booleanField", type = Boolean::class),
        ViewField(name = "floatField", type = Float::class),
        ViewField(name = "doubleField", type = Double::class),
        ViewField(name = "intField", type = Int::class),
        ViewField(name = "longField", type = Long::class),
        ViewField(name = "shortField", type = Short::class),
        ViewField(name = "byteField", type = Byte::class),
        ViewField(name = "stringField", type = String::class)
)
class TestViewDelegate(view: TestView) : ViewDelegate<TestView, TestViewIntState>(view) {
    override fun setupView() {

    }

    override fun validateStateForNewInput(data: TestViewIntState): TestViewIntState {
        return data.copy(visible = if (data.check) View.VISIBLE else View.INVISIBLE)
    }

    @Delegated
    internal fun showCustom(t: Int, p: String, a: TestViewDelegate): Int {
        return 0
    }
}
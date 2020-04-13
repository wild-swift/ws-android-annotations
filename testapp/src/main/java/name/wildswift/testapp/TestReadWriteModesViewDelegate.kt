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

import android.widget.LinearLayout
import name.wildswift.android.kannotations.*
import name.wildswift.android.kannotations.interfaces.ViewDelegate

/**
 * Created by swift
 */
@ViewWithDelegate(
        parent = LinearLayout::class
)
@Fields(
        ViewField(name = "count", rwType = ReadWriteMode.Field, type = Int::class),
        ViewField(name = "title", rwType = ReadWriteMode.Private, childName = IdRNames.vtrwmTitle, property = ViewProperty.text),
        ViewField(name = "roText", rwType = ReadWriteMode.Property, childName = IdRNames.vtrwmROEditor, property = ViewProperty.text),
        ViewField(name = "check", rwType = ReadWriteMode.ObservableProperty, childName = IdRNames.vtrwmCheckbox, property = ViewProperty.checked),
        ViewField(name = "radioSelect", rwType = ReadWriteMode.ObservableField, childName = IdRNames.vtrwmSelector, property = ViewProperty.radioSelect),
        ViewField(name = "rwText", rwType = ReadWriteMode.FullObservableField, childName = IdRNames.vtrwmRWEditor, property = ViewProperty.text)
)
@ViewEvent(name = "reset", childName = IdRNames.vtrwmButton, listener = ViewListener.onClick)
class TestReadWriteModesViewDelegate(view: TestReadWriteModesView) : ViewDelegate<TestReadWriteModesView, TestReadWriteModesViewIntState>(view) {
    override fun setupView() {
        view.orientation = LinearLayout.VERTICAL
    }

    override fun validateStateForNewInput(data: TestReadWriteModesViewIntState): TestReadWriteModesViewIntState {
        return data.copy(title = String.format("321 %d", data.count))
    }
}
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

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import name.wildswift.android.kannotations.*
import name.wildswift.android.kannotations.interfaces.ViewDelegate

@ViewWithDelegate(
        parent = RecyclerView::class,
        haveChild = false
)
@CollectionsFields(
        CollectionViewField(name = "items", childName = "this", type = String::class, viewForElementClass = TextView::class, childPropertyName = "text",
                elementEvents = [
                    ListEvent(name = "onItemClick", listener = ViewListener.onClick)
                ]
        )
)
class TestList2ViewDelegate(view: TestList2View) : ViewDelegate<TestList2View, TestList2ViewIntState>(view) {
    override fun setupView() {
    }
}
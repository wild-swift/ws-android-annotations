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

import android.widget.FrameLayout
import name.wildswift.android.kannotations.ListImplementation
import name.wildswift.android.kannotations.ListViewField
import name.wildswift.android.kannotations.ListingsFields
import name.wildswift.android.kannotations.ViewWithDelegate
import name.wildswift.android.kannotations.interfaces.ViewDelegate

@ViewWithDelegate(
        parent = FrameLayout::class
)
@ListingsFields(
        ListViewField(name = "subList1", childListView = IdRNames.vtlList, listImplementation = ListImplementation.ListView, delegateClass = TestViewDelegate::class)
//        ListViewField(name = "subList2", childListView = IdRNames.vtlList, listImplementation = ListImplementation.ListView, elementType = String::class, viewForElementClass = TextView::class, modelFieldName = "text"),
//        ListViewField(name = "subList3", childListView = IdRNames.vtlList, listImplementation = ListImplementation.ListView, elementType = String::class, viewForElementClass = TextView::class, modelFieldName = "text", modelSetterName = "setText")
)
class TestListViewDelegate(view: TestListView) : ViewDelegate<TestListView, TestListViewIntState>(view) {
}
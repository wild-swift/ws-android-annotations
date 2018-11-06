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
import kotlinx.android.synthetic.main.view_test.view.*
import name.wildswift.lib.androidkotlinannotations.ViewField
import name.wildswift.lib.androidkotlinannotations.ViewProperty
import name.wildswift.lib.androidkotlinannotations.ViewWithModel

/**
 * Created by swift
 */
@ViewWithModel(
        layoutResource = R.layout.view_test,
        fields = [
            ViewField(id = R.id.vtLabel, property = ViewProperty.text, name = "label")
        ]
)
class TestView: FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        initialize()
    }

}
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

package name.wildswift.android.kanprocessor.utils

import name.wildswift.android.kannotations.ReadWriteMode

/**
 * Created by swift
 */

val ReadWriteMode.public: Boolean get() = this != ReadWriteMode.Private

val ReadWriteMode.mutablePublic: Boolean get() = this == ReadWriteMode.Field || this == ReadWriteMode.ObservableField || this == ReadWriteMode.FullObservableField

val ReadWriteMode.notifyIntChanges: Boolean get() = this == ReadWriteMode.ObservableProperty || this == ReadWriteMode.ObservableField || this == ReadWriteMode.FullObservableField

val ReadWriteMode.notifyExtChanges: Boolean get() = this == ReadWriteMode.FullObservableField
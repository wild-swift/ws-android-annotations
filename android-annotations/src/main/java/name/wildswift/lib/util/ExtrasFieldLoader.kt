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
package name.wildswift.lib.util

import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by swift
 */

private object UNINITIALIZED_VALUE

class ExtrasFieldLoader<T, out U>(private val loader : T.() -> U): ReadOnlyProperty<T, U> {
    private var value: Any = UNINITIALIZED_VALUE
    private var lastThis: WeakReference<T?> = WeakReference(null)


    override fun getValue(thisRef: T, property: KProperty<*>): U {
        if (value === UNINITIALIZED_VALUE || thisRef != lastThis.get()) {
            value = thisRef.loader()!!
            lastThis = WeakReference(thisRef)
        }
        return value as U
    }
}
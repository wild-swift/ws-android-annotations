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
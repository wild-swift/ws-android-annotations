package name.wildswift.lib.androidkotlinprocessor.utils

/**
 * Created by swift
 */
fun String.toScreamingCase() = this
        .map {
            if (it.isUpperCase()) "_" + it else "" + it.toUpperCase()
        }
        .fold("") { prev, v ->
            prev + v
        }
        .let {
            var result = it
            while (result.startsWith("_")) result = result.substring(1)
            result
        }
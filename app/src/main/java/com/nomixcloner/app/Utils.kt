package com.nomixcloner.app

import android.os.Build
import org.json.JSONArray

fun getBuildField(fieldName: String, defaultValue: String = "unknown"): String {
    return try {
        val field = Build::class.java.getField(fieldName)
        when (val value = field.get(null)) {
            is Boolean -> value.toString()
            is Int -> value.toString()
            is String -> value
            else -> value?.toString() ?: defaultValue
        }
    } catch (e: Exception) {
        defaultValue
    }
}

// Helper function to get Build.VERSION field via reflection
fun getVersionField(fieldName: String, defaultValue: String = "unknown"): String {
    return try {
        val field = Build.VERSION::class.java.getField(fieldName)
        when (val value = field.get(null)) {
            is Boolean -> value.toString()
            is Int -> value.toString()
            is String -> value
            is Array<*> -> value.joinToString(", ")
            is Set<*> -> "{" + value.joinToString(", ") + "}"
            is Collection<*> -> "{" + value.joinToString(", ") + "}"
            else -> value?.toString() ?: defaultValue
        }
    } catch (e: Exception) {
        defaultValue
    }
}

// Helper function to get Build.VERSION array field via reflection
fun getVersionArrayField(fieldName: String): JSONArray {
    return try {
        val field = Build.VERSION::class.java.getField(fieldName)
        val value = field.get(null)
        if (value is Array<*>) {
            JSONArray(value.toList())
        } else {
            JSONArray()
        }
    } catch (e: Exception) {
        JSONArray()
    }
}
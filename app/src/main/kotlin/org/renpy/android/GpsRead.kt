package org.renpy.android

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object GpsRead {
    private const val TAG = "GpsRead"
    private const val FILE_NAME = "gps.txt"
    private const val DEFAULT_VALUE = true

    @Volatile
    private var cachedValue: Boolean? = null

    fun read(context: Context): Boolean {
        cachedValue?.let { return it }

        return synchronized(this) {
            cachedValue?.let { return it }

            val appContext = context.applicationContext ?: context
            val value = try {
                appContext.assets.open(FILE_NAME).use { stream ->
                    parseFirstLine(stream)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read $FILE_NAME from assets, defaulting to $DEFAULT_VALUE: ${e.message}")
                DEFAULT_VALUE
            }

            cachedValue = value
            value
        }
    }

    fun isEnabled(context: Context): Boolean = read(context)

    fun getValue(context: Context): Boolean = read(context)

    internal fun parseFirstLine(inputStream: InputStream): Boolean {
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            val firstLine = reader.readLine()?.trim()
            when (firstLine?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> DEFAULT_VALUE
            }
        }
    }


    fun clearCache() {
        synchronized(this) {
            cachedValue = null
        }
    }
}

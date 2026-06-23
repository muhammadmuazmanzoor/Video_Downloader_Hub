package com.video.avd.data.local.videosDataBase

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    // Convert List<Long> to a String (JSON format)
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        val gson = Gson()
        return gson.toJson(value) // Convert List<Long> to JSON string
    }

    // Convert String to List<Long> (from JSON format)
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        val gson = Gson()
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) // Convert JSON string back to List<Long>
    }
}
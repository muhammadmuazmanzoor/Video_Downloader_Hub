package com.video.avd.data.local

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.reflect.Type
import java.util.*

class DataConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        val listType: Type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }


    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }


    @TypeConverter
    fun toDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }


    @TypeConverter
    fun fromString(value: String?): List<Int>? {
        return value?.split(",")?.map { it.toInt() }
    }

    @TypeConverter
    fun fromList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }



    @TypeConverter
    fun fromLongList(videoIds: MutableList<Long>?): String? {
        // Ensure this logic correctly handles nulls and empty lists
        return videoIds?.joinToString(separator = ",")
    }

    @TypeConverter
    fun toLongList(videoIdsString: String?): MutableList<Long>? {
        // Ensure this logic correctly handles nulls, empty strings, and malformed strings
        if (videoIdsString.isNullOrBlank()) {
            return mutableListOf() // Or null, depending on your preference for empty vs null
        }
        return videoIdsString.split(',')
            .mapNotNull {
                try {
                    it.trim().toLong() // Added trim() for robustness
                } catch (ex: NumberFormatException) {
                    // Log error or handle as appropriate - here, we skip malformed entries
                    null
                }
            }
            .toMutableList()
    }

}

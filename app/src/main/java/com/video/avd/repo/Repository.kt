package com.video.avd.repo

import com.video.avd.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

@Singleton
class Repository(local: AppDatabase) {

    val localRepo = local

    suspend fun isObjectExists(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val video = localRepo.dao().getSongById(userId)
            video != null
        }
    }




}
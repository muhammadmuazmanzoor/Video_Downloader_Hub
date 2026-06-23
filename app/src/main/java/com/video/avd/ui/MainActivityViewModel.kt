package com.video.avd.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.repo.Repository
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppVaultManager
import com.video.avd.utils.InAppPurchases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repository: Repository,
    private val appVaultManager: AppVaultManager,
    val inAppPurchases: InAppPurchases,
    val application: Application
) : ViewModel() {

    var mPosition: Int? = null
    var buttonClick = MutableLiveData<Boolean>()
    var lastdetctedlink=""
    var callTheRateUsPlease = MutableLiveData<Boolean>()

    fun onClick(position: Int) {
        mPosition = position
        buttonClick.value = true
    }

    fun prepareNavigation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                inAppPurchases.getPrice(application.applicationContext)
                inAppPurchases.getInAppPrefs(application.applicationContext)
                inAppPurchases.getSubscriptionPref(application.applicationContext)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } catch (e: ConcurrentModificationException) {
                e.printStackTrace()
            }
        }

    }

    fun isPinSet(): Boolean {
        return appVaultManager.hasPin()
    }

    fun isSecurityQuestionSet(): Boolean {
        return appVaultManager.hasSecurityData()
    }

    suspend fun getEntitiesWithUpdatedTimeStump(): Flow<List<Video>> {
        return withContext(Dispatchers.IO) {
            repository.localRepo.dao().getEntitiesWithUpdatedTimeStump()
        }
    }

    suspend fun deleteVideoFromDb(entities: Video, deleted: (Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            repository.localRepo.dao().deleteEntities(entities.id)
            deleted(true)
        }
    }

    suspend fun clearAllRecentVideos() {
        repository.localRepo.dao().markAllVideosAsNotRecent()
    }

    fun updateUserDataAnsSetUpdatedTime(url: String, updatedTimeStump: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.localRepo.dao().updateByTimeStump(url = url, updatedTimeStump = updatedTimeStump)
        }
    }


    private fun deleteVideoFromDb(title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.localRepo.videosDao().deleteData(title)
        }
    }

    private fun getDataFromDb(): Flow<List<Video>> {
        return repository.localRepo.videosDao().getAllData()
    }

    private fun doesMediaContentExist(context: Context, contentUriString: String): Boolean {
        val contentUri = Uri.parse(contentUriString)
        val contentResolver = context.contentResolver
        try {
            contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }


}
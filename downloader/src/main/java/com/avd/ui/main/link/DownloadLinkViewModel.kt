package com.avd.ui.main.link

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.repository.VideoRepository
import com.avd.ui.main.base.BaseViewModel
import com.avd.util.SingleLiveEvent
import com.avd.util.scheduler.BaseSchedulers
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class DownloadLinkViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val baseSchedulers: BaseSchedulers
) : BaseViewModel() {
    private val disposableContainer = CompositeDisposable()

    val isLoading = ObservableField(false)

    val showDownloadDialogEvent = SingleLiveEvent<VideoInfo>()

    override fun start() {
    }

    override fun stop() {
        disposableContainer.clear()
    }

    fun fetchDownloadInfo(videoUrl: String) {
        isLoading.set(true)
        try {
            viewModelScope.launch(Dispatchers.Default) {
                val req =  Request.Builder().url(videoUrl.trim()).build()
                val info = videoRepository.getVideoInfo(req)
                isLoading.set(false)
                info?.let {
                    showDownloadDialogEvent.value = it
                }
            }
        } catch (e: Throwable) {
            isLoading.set(false)
        }
    }
}
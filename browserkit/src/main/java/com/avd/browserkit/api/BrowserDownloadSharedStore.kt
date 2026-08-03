package com.avd.browserkit.api

import com.avd.browserkit.download.BrowserDownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BrowserSharedDownloadTask(
    val taskId: String,
    val title: String,
    val pageUrl: String,
    val downloadUrl: String,
    val qualityLabel: String,
    val percent: Int,
    val status: BrowserDownloadStatus,
    val filePath: String? = null,
)

object BrowserDownloadSharedStore {
    private val _tasks = MutableStateFlow<List<BrowserSharedDownloadTask>>(emptyList())
    val tasks: StateFlow<List<BrowserSharedDownloadTask>> = _tasks.asStateFlow()

    fun upsert(task: BrowserSharedDownloadTask) {
        _tasks.update { current ->
            val index = current.indexOfFirst { it.taskId == task.taskId }
            if (index >= 0) {
                current.toMutableList().apply { set(index, task) }
            } else {
                current + task
            }
        }
    }

    fun update(
        taskId: String,
        title: String,
        pageUrl: String,
        percent: Int,
        status: BrowserDownloadStatus,
        filePath: String? = null,
        qualityLabel: String = "Default",
    ) {
        val current = _tasks.value.firstOrNull { it.taskId == taskId }
        upsert(
            BrowserSharedDownloadTask(
                taskId = taskId,
                title = title.ifBlank { current?.title.orEmpty() },
                pageUrl = pageUrl.ifBlank { current?.pageUrl.orEmpty() },
                downloadUrl = current?.downloadUrl.orEmpty(),
                qualityLabel = qualityLabel.ifBlank { current?.qualityLabel ?: "Default" },
                percent = percent,
                status = status,
                filePath = filePath ?: current?.filePath,
            ),
        )
    }

    fun remove(taskId: String) {
        _tasks.update { current -> current.filterNot { it.taskId == taskId } }
    }
}

package com.avd.browserkit.ui.browser

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BrowserViewModel : ViewModel() {
    private val _tabs = MutableLiveData(listOf(BrowserTab()))
    val tabs: LiveData<List<BrowserTab>> = _tabs

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _tabsSwitcherVisible = MutableLiveData(false)
    val tabsSwitcherVisible: LiveData<Boolean> = _tabsSwitcherVisible

    private val previews = linkedMapOf<String, Bitmap>()
    private val favicons = linkedMapOf<String, Bitmap>()

    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index.coerceAtLeast(0)
    }

    fun openTab(tab: BrowserTab) {
        val list = _tabs.value.orEmpty().toMutableList()
        list.add(tab)
        _tabs.value = list
        _currentIndex.value = list.lastIndex
    }

    fun closeTab(index: Int) {
        val list = _tabs.value.orEmpty().toMutableList()
        if (index !in list.indices) return
        if (list.size <= 1) {
            val removed = list[index]
            previews.remove(removed.id)
            favicons.remove(removed.id)
            _tabs.value = listOf(BrowserTab())
            _currentIndex.value = 0
            return
        }
        val removed = list.removeAt(index)
        previews.remove(removed.id)
        favicons.remove(removed.id)
        _tabs.value = list
        val current = _currentIndex.value ?: 0
        _currentIndex.value = when {
            current > list.lastIndex -> list.lastIndex
            current > index -> current - 1
            else -> current.coerceAtMost(list.lastIndex)
        }.coerceAtLeast(0)
    }

    fun updateTab(index: Int, title: String, url: String) {
        val list = _tabs.value.orEmpty().toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].copy(title = title.ifBlank { list[index].title }, url = url)
        _tabs.value = list
    }

    fun addNewTab(url: String = "https://www.google.com") {
        openTab(BrowserTab(url = url))
    }

    fun showTabsSwitcher() {
        _tabsSwitcherVisible.value = true
    }

    fun hideTabsSwitcher() {
        _tabsSwitcherVisible.value = false
    }

    fun setPreview(tabId: String, bitmap: Bitmap?) {
        if (bitmap == null) return
        previews[tabId] = bitmap
    }

    fun getPreview(tabId: String): Bitmap? = previews[tabId]

    fun setFavicon(tabId: String, bitmap: Bitmap?) {
        if (bitmap == null) return
        favicons[tabId] = bitmap
    }

    fun getFavicon(tabId: String): Bitmap? = favicons[tabId]

    override fun onCleared() {
        previews.clear()
        favicons.clear()
        super.onCleared()
    }
}

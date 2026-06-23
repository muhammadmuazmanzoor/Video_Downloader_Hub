package com.video.avd.utils

import androidx.annotation.Keep
import androidx.lifecycle.Observer

@Keep
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<ClickEvent<T>> {
    override fun onChanged(value: ClickEvent<T>) {
        value.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}
package com.video.avd.utils

import android.util.Log
import com.video.avd.ui.folder.model.VideoFolder
import com.video.avd.ui.videos.model.Video
import java.lang.ref.WeakReference

class WeakReferenceVideo {

    private var myObjectReffolder: WeakReference<List<VideoFolder>>? = null

    private var myObjectRefsong: WeakReference<List<Video>>? = null

    fun setObjectfolder(obj: List<VideoFolder>) {
        Log.d("FolderSet", obj.size.toString())
        myObjectReffolder = WeakReference(obj)
    }

    fun getObjectfolder(): List<VideoFolder> {
        Log.d("FolderSet get", myObjectReffolder?.get()?.size.toString())
        return myObjectReffolder?.get() ?: emptyList()
    }

    fun setObjectvideo(obj: List<Video>) {
        myObjectRefsong = WeakReference(obj)
    }

    fun getObjectvideo(): List<Video> {
        return myObjectRefsong?.get() ?: emptyList()
    }

}
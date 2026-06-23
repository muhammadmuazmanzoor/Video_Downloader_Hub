package com.video.avd.ui.file_manager

data class DirectoryModel(
    val name :String = "",
    val subFolderCount : String = "", //with hidden folders
    val path : String = "",
    val createdDate :String = "",
    val folderSize : String = "",
    val videoCount: String = "",
    val audioCount : String = "",
    val subFolderCountWithoutHidden : String = "" //without hidden folders
)
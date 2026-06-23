package com.video.avd.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.video.avd.data.local.videosDataBase.Converters
import com.video.avd.data.local.videosDataBase.FoldersDao
import com.video.avd.data.local.videosDataBase.VideosDao
import com.video.avd.ui.folder.model.VideoFolder
import com.video.avd.ui.player.bookmark.VideoBookmark
import com.video.avd.ui.player.subtitle.SubtitleState
import com.video.avd.ui.videos.model.Video

@Database(entities = [Entities::class, VideoEntity::class,Video::class,VideoBookmark::class, SubtitleState::class,VideoFolder::class], version =1, exportSchema = false)
@TypeConverters(DataConverter::class,Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): Dao
    abstract fun videosDao() : VideosDao
    abstract fun folderdao(): FoldersDao

}


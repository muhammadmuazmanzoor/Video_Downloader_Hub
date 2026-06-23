package com.video.avd.utils.chromecast.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.video.avd.utils.chromecast.database.dao.BookmarkDataDao;
import com.video.avd.utils.chromecast.database.dao.HistoryDataDao;
import com.video.avd.utils.chromecast.model.BookmarkData;
import com.video.avd.utils.chromecast.model.HistoryData;


@Database(entities = {HistoryData.class, BookmarkData.class}, version = 2, exportSchema = false)
public abstract class ApplicationDatabase extends RoomDatabase {
    public abstract HistoryDataDao historyDataDao();
    public abstract BookmarkDataDao bookmarkDataDao();
}

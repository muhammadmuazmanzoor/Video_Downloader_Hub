package com.video.avd.utils.chromecast.database;



import com.video.avd.utils.chromecast.model.BookmarkData;
import com.video.avd.utils.chromecast.model.HistoryData;

import java.util.List;

import io.reactivex.Observable;


public interface DatabaseHelperInterface {
//    Observable<Boolean> saveHistory(HistoryData historyData);
    Observable<List<HistoryData>> getListHistory();
    Observable<Boolean> clearHistory(HistoryData historyData);
    Observable<Boolean> clearAllHistory();

    Observable<Boolean> saveHistory(HistoryData historyData);

    Observable<List<HistoryData>> getListHistoryByType(String type);

//    Observable<Boolean> saveBookmark(BookmarkData bookmarkData);

    Observable<Boolean> saveBookmark(BookmarkData bookmarkData);

    Observable<List<BookmarkData>> getListBookmark();
    BookmarkData getBookmarkByPath(String path);
    Observable<Boolean> clearBookmarkByPath(String path);
    Observable<Boolean> clearAllBookmark();
    Observable<List<BookmarkData>> getListBookmarkByType(String type);
}

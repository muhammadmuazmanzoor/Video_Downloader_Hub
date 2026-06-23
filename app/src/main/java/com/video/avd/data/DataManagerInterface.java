package com.video.avd.data;


public interface DataManagerInterface {
    // rate :
    void setRatingUsDone();
    boolean checkRatingUsDone();

    void setShowGuideSelectMultiDone();
    boolean getShowGuideSelectMulti();

    int getTheme();
    void setTheme(int theme);

    String getLastIPAddress();
    void saveLastIPAddress(String ipAddress);

//    Observable<Boolean> saveHistory(HistoryData historyData);
//    Observable<List<HistoryData>> getListHistory();
//    Observable<Boolean> clearHistory(HistoryData historyData);
//    Observable<Boolean> clearAllHistory();
//    Observable<List<HistoryData>> getListHistoryByType(String type);
//
//    Observable<Boolean> saveBookmark(BookmarkData bookmarkData);
//    Observable<List<BookmarkData>> getListBookmark();
//    Observable<Boolean> isPathBookmarked(String path);
//    Observable<Boolean> clearBookmarkByPath(String path);
//    Observable<Boolean> clearAllBookmark();
//    Observable<List<BookmarkData>> getListBookmarkByType(String type);
}

package com.avd.browserkit.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BrowserKitDatabase_Impl extends BrowserKitDatabase {
  private volatile BrowserDownloadDao _browserDownloadDao;

  private volatile BrowserHistoryDao _browserHistoryDao;

  private volatile BrowserBookmarkDao _browserBookmarkDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_download_tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `pageUrl` TEXT NOT NULL, `downloadUrl` TEXT NOT NULL, `qualityLabel` TEXT NOT NULL, `streamType` TEXT NOT NULL, `headersJson` TEXT, `percent` INTEGER NOT NULL, `status` TEXT NOT NULL, `filePath` TEXT, `workerId` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `visitedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `browser_bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '61e7578d83bffdad2fdb5990f1dc416f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `browser_download_tasks`");
        db.execSQL("DROP TABLE IF EXISTS `browser_history`");
        db.execSQL("DROP TABLE IF EXISTS `browser_bookmarks`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBrowserDownloadTasks = new HashMap<String, TableInfo.Column>(13);
        _columnsBrowserDownloadTasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("pageUrl", new TableInfo.Column("pageUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("downloadUrl", new TableInfo.Column("downloadUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("qualityLabel", new TableInfo.Column("qualityLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("streamType", new TableInfo.Column("streamType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("headersJson", new TableInfo.Column("headersJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("percent", new TableInfo.Column("percent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("filePath", new TableInfo.Column("filePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("workerId", new TableInfo.Column("workerId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserDownloadTasks.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBrowserDownloadTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBrowserDownloadTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBrowserDownloadTasks = new TableInfo("browser_download_tasks", _columnsBrowserDownloadTasks, _foreignKeysBrowserDownloadTasks, _indicesBrowserDownloadTasks);
        final TableInfo _existingBrowserDownloadTasks = TableInfo.read(db, "browser_download_tasks");
        if (!_infoBrowserDownloadTasks.equals(_existingBrowserDownloadTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "browser_download_tasks(com.avd.browserkit.data.BrowserDownloadEntity).\n"
                  + " Expected:\n" + _infoBrowserDownloadTasks + "\n"
                  + " Found:\n" + _existingBrowserDownloadTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsBrowserHistory = new HashMap<String, TableInfo.Column>(4);
        _columnsBrowserHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserHistory.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserHistory.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserHistory.put("visitedAt", new TableInfo.Column("visitedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBrowserHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBrowserHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBrowserHistory = new TableInfo("browser_history", _columnsBrowserHistory, _foreignKeysBrowserHistory, _indicesBrowserHistory);
        final TableInfo _existingBrowserHistory = TableInfo.read(db, "browser_history");
        if (!_infoBrowserHistory.equals(_existingBrowserHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "browser_history(com.avd.browserkit.data.BrowserHistoryEntity).\n"
                  + " Expected:\n" + _infoBrowserHistory + "\n"
                  + " Found:\n" + _existingBrowserHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsBrowserBookmarks = new HashMap<String, TableInfo.Column>(4);
        _columnsBrowserBookmarks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserBookmarks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserBookmarks.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBrowserBookmarks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBrowserBookmarks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBrowserBookmarks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBrowserBookmarks = new TableInfo("browser_bookmarks", _columnsBrowserBookmarks, _foreignKeysBrowserBookmarks, _indicesBrowserBookmarks);
        final TableInfo _existingBrowserBookmarks = TableInfo.read(db, "browser_bookmarks");
        if (!_infoBrowserBookmarks.equals(_existingBrowserBookmarks)) {
          return new RoomOpenHelper.ValidationResult(false, "browser_bookmarks(com.avd.browserkit.data.BrowserBookmarkEntity).\n"
                  + " Expected:\n" + _infoBrowserBookmarks + "\n"
                  + " Found:\n" + _existingBrowserBookmarks);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "61e7578d83bffdad2fdb5990f1dc416f", "9c2e3deab92907a545e7b73d237f2916");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "browser_download_tasks","browser_history","browser_bookmarks");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `browser_download_tasks`");
      _db.execSQL("DELETE FROM `browser_history`");
      _db.execSQL("DELETE FROM `browser_bookmarks`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BrowserDownloadDao.class, BrowserDownloadDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BrowserHistoryDao.class, BrowserHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BrowserBookmarkDao.class, BrowserBookmarkDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BrowserDownloadDao downloadDao() {
    if (_browserDownloadDao != null) {
      return _browserDownloadDao;
    } else {
      synchronized(this) {
        if(_browserDownloadDao == null) {
          _browserDownloadDao = new BrowserDownloadDao_Impl(this);
        }
        return _browserDownloadDao;
      }
    }
  }

  @Override
  public BrowserHistoryDao historyDao() {
    if (_browserHistoryDao != null) {
      return _browserHistoryDao;
    } else {
      synchronized(this) {
        if(_browserHistoryDao == null) {
          _browserHistoryDao = new BrowserHistoryDao_Impl(this);
        }
        return _browserHistoryDao;
      }
    }
  }

  @Override
  public BrowserBookmarkDao bookmarkDao() {
    if (_browserBookmarkDao != null) {
      return _browserBookmarkDao;
    } else {
      synchronized(this) {
        if(_browserBookmarkDao == null) {
          _browserBookmarkDao = new BrowserBookmarkDao_Impl(this);
        }
        return _browserBookmarkDao;
      }
    }
  }
}

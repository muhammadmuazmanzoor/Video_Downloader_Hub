package com.avd.browserkit.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BrowserDownloadDao_Impl implements BrowserDownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BrowserDownloadEntity> __insertionAdapterOfBrowserDownloadEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public BrowserDownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBrowserDownloadEntity = new EntityInsertionAdapter<BrowserDownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `browser_download_tasks` (`id`,`title`,`pageUrl`,`downloadUrl`,`qualityLabel`,`streamType`,`headersJson`,`percent`,`status`,`filePath`,`workerId`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BrowserDownloadEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getPageUrl());
        statement.bindString(4, entity.getDownloadUrl());
        statement.bindString(5, entity.getQualityLabel());
        statement.bindString(6, entity.getStreamType());
        if (entity.getHeadersJson() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getHeadersJson());
        }
        statement.bindLong(8, entity.getPercent());
        statement.bindString(9, entity.getStatus());
        if (entity.getFilePath() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getFilePath());
        }
        if (entity.getWorkerId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getWorkerId());
        }
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindLong(13, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM browser_download_tasks WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final BrowserDownloadEntity task,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBrowserDownloadEntity.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<BrowserDownloadEntity>> $completion) {
    final String _sql = "SELECT * FROM browser_download_tasks ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BrowserDownloadEntity>>() {
      @Override
      @NonNull
      public List<BrowserDownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "pageUrl");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadUrl");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BrowserDownloadEntity> _result = new ArrayList<BrowserDownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BrowserDownloadEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPageUrl;
            _tmpPageUrl = _cursor.getString(_cursorIndexOfPageUrl);
            final String _tmpDownloadUrl;
            _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            final String _tmpStreamType;
            _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            final String _tmpHeadersJson;
            if (_cursor.isNull(_cursorIndexOfHeadersJson)) {
              _tmpHeadersJson = null;
            } else {
              _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            }
            final int _tmpPercent;
            _tmpPercent = _cursor.getInt(_cursorIndexOfPercent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpWorkerId;
            if (_cursor.isNull(_cursorIndexOfWorkerId)) {
              _tmpWorkerId = null;
            } else {
              _tmpWorkerId = _cursor.getString(_cursorIndexOfWorkerId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BrowserDownloadEntity(_tmpId,_tmpTitle,_tmpPageUrl,_tmpDownloadUrl,_tmpQualityLabel,_tmpStreamType,_tmpHeadersJson,_tmpPercent,_tmpStatus,_tmpFilePath,_tmpWorkerId,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getActive(final Continuation<? super List<BrowserDownloadEntity>> $completion) {
    final String _sql = "SELECT * FROM browser_download_tasks WHERE status IN ('QUEUED','DOWNLOADING','PAUSED') ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BrowserDownloadEntity>>() {
      @Override
      @NonNull
      public List<BrowserDownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "pageUrl");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadUrl");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BrowserDownloadEntity> _result = new ArrayList<BrowserDownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BrowserDownloadEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPageUrl;
            _tmpPageUrl = _cursor.getString(_cursorIndexOfPageUrl);
            final String _tmpDownloadUrl;
            _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            final String _tmpStreamType;
            _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            final String _tmpHeadersJson;
            if (_cursor.isNull(_cursorIndexOfHeadersJson)) {
              _tmpHeadersJson = null;
            } else {
              _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            }
            final int _tmpPercent;
            _tmpPercent = _cursor.getInt(_cursorIndexOfPercent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpWorkerId;
            if (_cursor.isNull(_cursorIndexOfWorkerId)) {
              _tmpWorkerId = null;
            } else {
              _tmpWorkerId = _cursor.getString(_cursorIndexOfWorkerId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BrowserDownloadEntity(_tmpId,_tmpTitle,_tmpPageUrl,_tmpDownloadUrl,_tmpQualityLabel,_tmpStreamType,_tmpHeadersJson,_tmpPercent,_tmpStatus,_tmpFilePath,_tmpWorkerId,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getFinished(final Continuation<? super List<BrowserDownloadEntity>> $completion) {
    final String _sql = "SELECT * FROM browser_download_tasks WHERE status IN ('COMPLETED','FAILED') ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BrowserDownloadEntity>>() {
      @Override
      @NonNull
      public List<BrowserDownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "pageUrl");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadUrl");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BrowserDownloadEntity> _result = new ArrayList<BrowserDownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BrowserDownloadEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPageUrl;
            _tmpPageUrl = _cursor.getString(_cursorIndexOfPageUrl);
            final String _tmpDownloadUrl;
            _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            final String _tmpStreamType;
            _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            final String _tmpHeadersJson;
            if (_cursor.isNull(_cursorIndexOfHeadersJson)) {
              _tmpHeadersJson = null;
            } else {
              _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            }
            final int _tmpPercent;
            _tmpPercent = _cursor.getInt(_cursorIndexOfPercent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpWorkerId;
            if (_cursor.isNull(_cursorIndexOfWorkerId)) {
              _tmpWorkerId = null;
            } else {
              _tmpWorkerId = _cursor.getString(_cursorIndexOfWorkerId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BrowserDownloadEntity(_tmpId,_tmpTitle,_tmpPageUrl,_tmpDownloadUrl,_tmpQualityLabel,_tmpStreamType,_tmpHeadersJson,_tmpPercent,_tmpStatus,_tmpFilePath,_tmpWorkerId,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String id,
      final Continuation<? super BrowserDownloadEntity> $completion) {
    final String _sql = "SELECT * FROM browser_download_tasks WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BrowserDownloadEntity>() {
      @Override
      @Nullable
      public BrowserDownloadEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "pageUrl");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadUrl");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfWorkerId = CursorUtil.getColumnIndexOrThrow(_cursor, "workerId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final BrowserDownloadEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPageUrl;
            _tmpPageUrl = _cursor.getString(_cursorIndexOfPageUrl);
            final String _tmpDownloadUrl;
            _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            final String _tmpStreamType;
            _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            final String _tmpHeadersJson;
            if (_cursor.isNull(_cursorIndexOfHeadersJson)) {
              _tmpHeadersJson = null;
            } else {
              _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            }
            final int _tmpPercent;
            _tmpPercent = _cursor.getInt(_cursorIndexOfPercent);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpWorkerId;
            if (_cursor.isNull(_cursorIndexOfWorkerId)) {
              _tmpWorkerId = null;
            } else {
              _tmpWorkerId = _cursor.getString(_cursorIndexOfWorkerId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new BrowserDownloadEntity(_tmpId,_tmpTitle,_tmpPageUrl,_tmpDownloadUrl,_tmpQualityLabel,_tmpStreamType,_tmpHeadersJson,_tmpPercent,_tmpStatus,_tmpFilePath,_tmpWorkerId,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

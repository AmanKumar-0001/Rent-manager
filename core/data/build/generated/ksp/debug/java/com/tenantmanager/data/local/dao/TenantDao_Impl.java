package com.tenantmanager.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.tenantmanager.data.local.entity.TenantEntity;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TenantDao_Impl implements TenantDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TenantEntity> __insertionAdapterOfTenantEntity;

  private final EntityDeletionOrUpdateAdapter<TenantEntity> __updateAdapterOfTenantEntity;

  private final SharedSQLiteStatement __preparedStmtOfArchiveTenant;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTenant;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public TenantDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTenantEntity = new EntityInsertionAdapter<TenantEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tenants` (`id`,`propertyId`,`name`,`phone`,`email`,`unitName`,`joinDate`,`leaveDate`,`monthlyRentMinor`,`dueDay`,`securityDepositMinor`,`depositReturnedDate`,`notes`,`isArchived`,`createdAt`,`lastModified`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TenantEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getPropertyId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPropertyId());
        }
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPhone());
        if (entity.getEmail() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getEmail());
        }
        statement.bindString(6, entity.getUnitName());
        statement.bindString(7, entity.getJoinDate());
        if (entity.getLeaveDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLeaveDate());
        }
        statement.bindLong(9, entity.getMonthlyRentMinor());
        statement.bindLong(10, entity.getDueDay());
        statement.bindLong(11, entity.getSecurityDepositMinor());
        if (entity.getDepositReturnedDate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDepositReturnedDate());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNotes());
        }
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getLastModified());
      }
    };
    this.__updateAdapterOfTenantEntity = new EntityDeletionOrUpdateAdapter<TenantEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tenants` SET `id` = ?,`propertyId` = ?,`name` = ?,`phone` = ?,`email` = ?,`unitName` = ?,`joinDate` = ?,`leaveDate` = ?,`monthlyRentMinor` = ?,`dueDay` = ?,`securityDepositMinor` = ?,`depositReturnedDate` = ?,`notes` = ?,`isArchived` = ?,`createdAt` = ?,`lastModified` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TenantEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getPropertyId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPropertyId());
        }
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPhone());
        if (entity.getEmail() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getEmail());
        }
        statement.bindString(6, entity.getUnitName());
        statement.bindString(7, entity.getJoinDate());
        if (entity.getLeaveDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLeaveDate());
        }
        statement.bindLong(9, entity.getMonthlyRentMinor());
        statement.bindLong(10, entity.getDueDay());
        statement.bindLong(11, entity.getSecurityDepositMinor());
        if (entity.getDepositReturnedDate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDepositReturnedDate());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNotes());
        }
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getLastModified());
        statement.bindString(17, entity.getId());
      }
    };
    this.__preparedStmtOfArchiveTenant = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tenants SET isArchived = 1, leaveDate = ?, lastModified = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTenant = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tenants WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tenants";
        return _query;
      }
    };
  }

  @Override
  public Object insertTenant(final TenantEntity tenant,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTenantEntity.insert(tenant);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<TenantEntity> tenants,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTenantEntity.insert(tenants);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTenant(final TenantEntity tenant,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTenantEntity.handle(tenant);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object archiveTenant(final String id, final String leaveDate, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfArchiveTenant.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, leaveDate);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 3;
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
          __preparedStmtOfArchiveTenant.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTenant(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTenant.acquire();
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
          __preparedStmtOfDeleteTenant.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TenantEntity>> getAllTenants() {
    final String _sql = "SELECT * FROM tenants ORDER BY isArchived ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tenants"}, new Callable<List<TenantEntity>>() {
      @Override
      @NonNull
      public List<TenantEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPropertyId = CursorUtil.getColumnIndexOrThrow(_cursor, "propertyId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfUnitName = CursorUtil.getColumnIndexOrThrow(_cursor, "unitName");
          final int _cursorIndexOfJoinDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joinDate");
          final int _cursorIndexOfLeaveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "leaveDate");
          final int _cursorIndexOfMonthlyRentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyRentMinor");
          final int _cursorIndexOfDueDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDay");
          final int _cursorIndexOfSecurityDepositMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "securityDepositMinor");
          final int _cursorIndexOfDepositReturnedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "depositReturnedDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final List<TenantEntity> _result = new ArrayList<TenantEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TenantEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPropertyId;
            if (_cursor.isNull(_cursorIndexOfPropertyId)) {
              _tmpPropertyId = null;
            } else {
              _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpUnitName;
            _tmpUnitName = _cursor.getString(_cursorIndexOfUnitName);
            final String _tmpJoinDate;
            _tmpJoinDate = _cursor.getString(_cursorIndexOfJoinDate);
            final String _tmpLeaveDate;
            if (_cursor.isNull(_cursorIndexOfLeaveDate)) {
              _tmpLeaveDate = null;
            } else {
              _tmpLeaveDate = _cursor.getString(_cursorIndexOfLeaveDate);
            }
            final long _tmpMonthlyRentMinor;
            _tmpMonthlyRentMinor = _cursor.getLong(_cursorIndexOfMonthlyRentMinor);
            final int _tmpDueDay;
            _tmpDueDay = _cursor.getInt(_cursorIndexOfDueDay);
            final long _tmpSecurityDepositMinor;
            _tmpSecurityDepositMinor = _cursor.getLong(_cursorIndexOfSecurityDepositMinor);
            final String _tmpDepositReturnedDate;
            if (_cursor.isNull(_cursorIndexOfDepositReturnedDate)) {
              _tmpDepositReturnedDate = null;
            } else {
              _tmpDepositReturnedDate = _cursor.getString(_cursorIndexOfDepositReturnedDate);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _item = new TenantEntity(_tmpId,_tmpPropertyId,_tmpName,_tmpPhone,_tmpEmail,_tmpUnitName,_tmpJoinDate,_tmpLeaveDate,_tmpMonthlyRentMinor,_tmpDueDay,_tmpSecurityDepositMinor,_tmpDepositReturnedDate,_tmpNotes,_tmpIsArchived,_tmpCreatedAt,_tmpLastModified);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TenantEntity>> getActiveTenants() {
    final String _sql = "SELECT * FROM tenants WHERE isArchived = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tenants"}, new Callable<List<TenantEntity>>() {
      @Override
      @NonNull
      public List<TenantEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPropertyId = CursorUtil.getColumnIndexOrThrow(_cursor, "propertyId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfUnitName = CursorUtil.getColumnIndexOrThrow(_cursor, "unitName");
          final int _cursorIndexOfJoinDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joinDate");
          final int _cursorIndexOfLeaveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "leaveDate");
          final int _cursorIndexOfMonthlyRentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyRentMinor");
          final int _cursorIndexOfDueDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDay");
          final int _cursorIndexOfSecurityDepositMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "securityDepositMinor");
          final int _cursorIndexOfDepositReturnedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "depositReturnedDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final List<TenantEntity> _result = new ArrayList<TenantEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TenantEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPropertyId;
            if (_cursor.isNull(_cursorIndexOfPropertyId)) {
              _tmpPropertyId = null;
            } else {
              _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpUnitName;
            _tmpUnitName = _cursor.getString(_cursorIndexOfUnitName);
            final String _tmpJoinDate;
            _tmpJoinDate = _cursor.getString(_cursorIndexOfJoinDate);
            final String _tmpLeaveDate;
            if (_cursor.isNull(_cursorIndexOfLeaveDate)) {
              _tmpLeaveDate = null;
            } else {
              _tmpLeaveDate = _cursor.getString(_cursorIndexOfLeaveDate);
            }
            final long _tmpMonthlyRentMinor;
            _tmpMonthlyRentMinor = _cursor.getLong(_cursorIndexOfMonthlyRentMinor);
            final int _tmpDueDay;
            _tmpDueDay = _cursor.getInt(_cursorIndexOfDueDay);
            final long _tmpSecurityDepositMinor;
            _tmpSecurityDepositMinor = _cursor.getLong(_cursorIndexOfSecurityDepositMinor);
            final String _tmpDepositReturnedDate;
            if (_cursor.isNull(_cursorIndexOfDepositReturnedDate)) {
              _tmpDepositReturnedDate = null;
            } else {
              _tmpDepositReturnedDate = _cursor.getString(_cursorIndexOfDepositReturnedDate);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _item = new TenantEntity(_tmpId,_tmpPropertyId,_tmpName,_tmpPhone,_tmpEmail,_tmpUnitName,_tmpJoinDate,_tmpLeaveDate,_tmpMonthlyRentMinor,_tmpDueDay,_tmpSecurityDepositMinor,_tmpDepositReturnedDate,_tmpNotes,_tmpIsArchived,_tmpCreatedAt,_tmpLastModified);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTenantById(final String id,
      final Continuation<? super TenantEntity> $completion) {
    final String _sql = "SELECT * FROM tenants WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TenantEntity>() {
      @Override
      @Nullable
      public TenantEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPropertyId = CursorUtil.getColumnIndexOrThrow(_cursor, "propertyId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfUnitName = CursorUtil.getColumnIndexOrThrow(_cursor, "unitName");
          final int _cursorIndexOfJoinDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joinDate");
          final int _cursorIndexOfLeaveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "leaveDate");
          final int _cursorIndexOfMonthlyRentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyRentMinor");
          final int _cursorIndexOfDueDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDay");
          final int _cursorIndexOfSecurityDepositMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "securityDepositMinor");
          final int _cursorIndexOfDepositReturnedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "depositReturnedDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final TenantEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPropertyId;
            if (_cursor.isNull(_cursorIndexOfPropertyId)) {
              _tmpPropertyId = null;
            } else {
              _tmpPropertyId = _cursor.getString(_cursorIndexOfPropertyId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpUnitName;
            _tmpUnitName = _cursor.getString(_cursorIndexOfUnitName);
            final String _tmpJoinDate;
            _tmpJoinDate = _cursor.getString(_cursorIndexOfJoinDate);
            final String _tmpLeaveDate;
            if (_cursor.isNull(_cursorIndexOfLeaveDate)) {
              _tmpLeaveDate = null;
            } else {
              _tmpLeaveDate = _cursor.getString(_cursorIndexOfLeaveDate);
            }
            final long _tmpMonthlyRentMinor;
            _tmpMonthlyRentMinor = _cursor.getLong(_cursorIndexOfMonthlyRentMinor);
            final int _tmpDueDay;
            _tmpDueDay = _cursor.getInt(_cursorIndexOfDueDay);
            final long _tmpSecurityDepositMinor;
            _tmpSecurityDepositMinor = _cursor.getLong(_cursorIndexOfSecurityDepositMinor);
            final String _tmpDepositReturnedDate;
            if (_cursor.isNull(_cursorIndexOfDepositReturnedDate)) {
              _tmpDepositReturnedDate = null;
            } else {
              _tmpDepositReturnedDate = _cursor.getString(_cursorIndexOfDepositReturnedDate);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _result = new TenantEntity(_tmpId,_tmpPropertyId,_tmpName,_tmpPhone,_tmpEmail,_tmpUnitName,_tmpJoinDate,_tmpLeaveDate,_tmpMonthlyRentMinor,_tmpDueDay,_tmpSecurityDepositMinor,_tmpDepositReturnedDate,_tmpNotes,_tmpIsArchived,_tmpCreatedAt,_tmpLastModified);
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

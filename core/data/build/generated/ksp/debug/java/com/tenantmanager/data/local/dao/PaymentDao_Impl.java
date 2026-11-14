package com.tenantmanager.data.local.dao;

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
import com.tenantmanager.data.local.entity.PaymentEntity;
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
public final class PaymentDao_Impl implements PaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PaymentEntity> __insertionAdapterOfPaymentEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeletePayment;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public PaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPaymentEntity = new EntityInsertionAdapter<PaymentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `payments` (`id`,`tenantId`,`amountMinor`,`paidDate`,`periodMonth`,`notes`,`createdAt`,`lastModified`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PaymentEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTenantId());
        statement.bindLong(3, entity.getAmountMinor());
        statement.bindString(4, entity.getPaidDate());
        statement.bindString(5, entity.getPeriodMonth());
        if (entity.getNotes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNotes());
        }
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getLastModified());
      }
    };
    this.__preparedStmtOfDeletePayment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM payments WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM payments";
        return _query;
      }
    };
  }

  @Override
  public Object insertPayment(final PaymentEntity payment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaymentEntity.insert(payment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<PaymentEntity> payments,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaymentEntity.insert(payments);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePayment(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePayment.acquire();
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
          __preparedStmtOfDeletePayment.release(_stmt);
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
  public Flow<List<PaymentEntity>> getAllPayments() {
    final String _sql = "SELECT * FROM payments ORDER BY paidDate DESC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments"}, new Callable<List<PaymentEntity>>() {
      @Override
      @NonNull
      public List<PaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTenantId = CursorUtil.getColumnIndexOrThrow(_cursor, "tenantId");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfPeriodMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "periodMonth");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final List<PaymentEntity> _result = new ArrayList<PaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PaymentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTenantId;
            _tmpTenantId = _cursor.getString(_cursorIndexOfTenantId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpPaidDate;
            _tmpPaidDate = _cursor.getString(_cursorIndexOfPaidDate);
            final String _tmpPeriodMonth;
            _tmpPeriodMonth = _cursor.getString(_cursorIndexOfPeriodMonth);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _item = new PaymentEntity(_tmpId,_tmpTenantId,_tmpAmountMinor,_tmpPaidDate,_tmpPeriodMonth,_tmpNotes,_tmpCreatedAt,_tmpLastModified);
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
  public Flow<List<PaymentEntity>> getPaymentsForTenant(final String tenantId) {
    final String _sql = "SELECT * FROM payments WHERE tenantId = ? ORDER BY paidDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tenantId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments"}, new Callable<List<PaymentEntity>>() {
      @Override
      @NonNull
      public List<PaymentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTenantId = CursorUtil.getColumnIndexOrThrow(_cursor, "tenantId");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfPeriodMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "periodMonth");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final List<PaymentEntity> _result = new ArrayList<PaymentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PaymentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTenantId;
            _tmpTenantId = _cursor.getString(_cursorIndexOfTenantId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpPaidDate;
            _tmpPaidDate = _cursor.getString(_cursorIndexOfPaidDate);
            final String _tmpPeriodMonth;
            _tmpPeriodMonth = _cursor.getString(_cursorIndexOfPeriodMonth);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _item = new PaymentEntity(_tmpId,_tmpTenantId,_tmpAmountMinor,_tmpPaidDate,_tmpPeriodMonth,_tmpNotes,_tmpCreatedAt,_tmpLastModified);
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
  public Object getPaymentById(final String id,
      final Continuation<? super PaymentEntity> $completion) {
    final String _sql = "SELECT * FROM payments WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PaymentEntity>() {
      @Override
      @Nullable
      public PaymentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTenantId = CursorUtil.getColumnIndexOrThrow(_cursor, "tenantId");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfPaidDate = CursorUtil.getColumnIndexOrThrow(_cursor, "paidDate");
          final int _cursorIndexOfPeriodMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "periodMonth");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final PaymentEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTenantId;
            _tmpTenantId = _cursor.getString(_cursorIndexOfTenantId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpPaidDate;
            _tmpPaidDate = _cursor.getString(_cursorIndexOfPaidDate);
            final String _tmpPeriodMonth;
            _tmpPeriodMonth = _cursor.getString(_cursorIndexOfPeriodMonth);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            _result = new PaymentEntity(_tmpId,_tmpTenantId,_tmpAmountMinor,_tmpPaidDate,_tmpPeriodMonth,_tmpNotes,_tmpCreatedAt,_tmpLastModified);
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

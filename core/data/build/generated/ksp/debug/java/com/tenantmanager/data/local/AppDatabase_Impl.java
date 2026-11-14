package com.tenantmanager.data.local;

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
import com.tenantmanager.data.local.dao.AlertHistoryDao;
import com.tenantmanager.data.local.dao.AlertHistoryDao_Impl;
import com.tenantmanager.data.local.dao.PaymentDao;
import com.tenantmanager.data.local.dao.PaymentDao_Impl;
import com.tenantmanager.data.local.dao.PropertyDao;
import com.tenantmanager.data.local.dao.PropertyDao_Impl;
import com.tenantmanager.data.local.dao.TenantDao;
import com.tenantmanager.data.local.dao.TenantDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile PropertyDao _propertyDao;

  private volatile TenantDao _tenantDao;

  private volatile PaymentDao _paymentDao;

  private volatile AlertHistoryDao _alertHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `properties` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `address` TEXT, `defaultRentMinor` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tenants` (`id` TEXT NOT NULL, `propertyId` TEXT, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT, `unitName` TEXT NOT NULL, `joinDate` TEXT NOT NULL, `leaveDate` TEXT, `monthlyRentMinor` INTEGER NOT NULL, `dueDay` INTEGER NOT NULL, `securityDepositMinor` INTEGER NOT NULL, `depositReturnedDate` TEXT, `notes` TEXT, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`propertyId`) REFERENCES `properties`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tenants_propertyId` ON `tenants` (`propertyId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tenants_isArchived` ON `tenants` (`isArchived`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `paidDate` TEXT NOT NULL, `periodMonth` TEXT NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`tenantId`) REFERENCES `tenants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_tenantId` ON `payments` (`tenantId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_periodMonth` ON `payments` (`periodMonth`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `alert_history` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `periodMonth` TEXT NOT NULL, `alertType` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`tenantId`) REFERENCES `tenants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_alert_history_tenantId_periodMonth_alertType` ON `alert_history` (`tenantId`, `periodMonth`, `alertType`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '91ad8a186b56671a0076941a5e85ddf9')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `properties`");
        db.execSQL("DROP TABLE IF EXISTS `tenants`");
        db.execSQL("DROP TABLE IF EXISTS `payments`");
        db.execSQL("DROP TABLE IF EXISTS `alert_history`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsProperties = new HashMap<String, TableInfo.Column>(6);
        _columnsProperties.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("defaultRentMinor", new TableInfo.Column("defaultRentMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("lastModified", new TableInfo.Column("lastModified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProperties = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProperties = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProperties = new TableInfo("properties", _columnsProperties, _foreignKeysProperties, _indicesProperties);
        final TableInfo _existingProperties = TableInfo.read(db, "properties");
        if (!_infoProperties.equals(_existingProperties)) {
          return new RoomOpenHelper.ValidationResult(false, "properties(com.tenantmanager.data.local.entity.PropertyEntity).\n"
                  + " Expected:\n" + _infoProperties + "\n"
                  + " Found:\n" + _existingProperties);
        }
        final HashMap<String, TableInfo.Column> _columnsTenants = new HashMap<String, TableInfo.Column>(16);
        _columnsTenants.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("propertyId", new TableInfo.Column("propertyId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("unitName", new TableInfo.Column("unitName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("joinDate", new TableInfo.Column("joinDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("leaveDate", new TableInfo.Column("leaveDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("monthlyRentMinor", new TableInfo.Column("monthlyRentMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("dueDay", new TableInfo.Column("dueDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("securityDepositMinor", new TableInfo.Column("securityDepositMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("depositReturnedDate", new TableInfo.Column("depositReturnedDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTenants.put("lastModified", new TableInfo.Column("lastModified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTenants = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysTenants.add(new TableInfo.ForeignKey("properties", "SET NULL", "NO ACTION", Arrays.asList("propertyId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesTenants = new HashSet<TableInfo.Index>(2);
        _indicesTenants.add(new TableInfo.Index("index_tenants_propertyId", false, Arrays.asList("propertyId"), Arrays.asList("ASC")));
        _indicesTenants.add(new TableInfo.Index("index_tenants_isArchived", false, Arrays.asList("isArchived"), Arrays.asList("ASC")));
        final TableInfo _infoTenants = new TableInfo("tenants", _columnsTenants, _foreignKeysTenants, _indicesTenants);
        final TableInfo _existingTenants = TableInfo.read(db, "tenants");
        if (!_infoTenants.equals(_existingTenants)) {
          return new RoomOpenHelper.ValidationResult(false, "tenants(com.tenantmanager.data.local.entity.TenantEntity).\n"
                  + " Expected:\n" + _infoTenants + "\n"
                  + " Found:\n" + _existingTenants);
        }
        final HashMap<String, TableInfo.Column> _columnsPayments = new HashMap<String, TableInfo.Column>(8);
        _columnsPayments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("tenantId", new TableInfo.Column("tenantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("amountMinor", new TableInfo.Column("amountMinor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("paidDate", new TableInfo.Column("paidDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("periodMonth", new TableInfo.Column("periodMonth", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("lastModified", new TableInfo.Column("lastModified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPayments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPayments.add(new TableInfo.ForeignKey("tenants", "CASCADE", "NO ACTION", Arrays.asList("tenantId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPayments = new HashSet<TableInfo.Index>(2);
        _indicesPayments.add(new TableInfo.Index("index_payments_tenantId", false, Arrays.asList("tenantId"), Arrays.asList("ASC")));
        _indicesPayments.add(new TableInfo.Index("index_payments_periodMonth", false, Arrays.asList("periodMonth"), Arrays.asList("ASC")));
        final TableInfo _infoPayments = new TableInfo("payments", _columnsPayments, _foreignKeysPayments, _indicesPayments);
        final TableInfo _existingPayments = TableInfo.read(db, "payments");
        if (!_infoPayments.equals(_existingPayments)) {
          return new RoomOpenHelper.ValidationResult(false, "payments(com.tenantmanager.data.local.entity.PaymentEntity).\n"
                  + " Expected:\n" + _infoPayments + "\n"
                  + " Found:\n" + _existingPayments);
        }
        final HashMap<String, TableInfo.Column> _columnsAlertHistory = new HashMap<String, TableInfo.Column>(5);
        _columnsAlertHistory.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertHistory.put("tenantId", new TableInfo.Column("tenantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertHistory.put("periodMonth", new TableInfo.Column("periodMonth", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertHistory.put("alertType", new TableInfo.Column("alertType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertHistory.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAlertHistory = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAlertHistory.add(new TableInfo.ForeignKey("tenants", "CASCADE", "NO ACTION", Arrays.asList("tenantId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAlertHistory = new HashSet<TableInfo.Index>(1);
        _indicesAlertHistory.add(new TableInfo.Index("index_alert_history_tenantId_periodMonth_alertType", true, Arrays.asList("tenantId", "periodMonth", "alertType"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoAlertHistory = new TableInfo("alert_history", _columnsAlertHistory, _foreignKeysAlertHistory, _indicesAlertHistory);
        final TableInfo _existingAlertHistory = TableInfo.read(db, "alert_history");
        if (!_infoAlertHistory.equals(_existingAlertHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "alert_history(com.tenantmanager.data.local.entity.AlertHistoryEntity).\n"
                  + " Expected:\n" + _infoAlertHistory + "\n"
                  + " Found:\n" + _existingAlertHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "91ad8a186b56671a0076941a5e85ddf9", "1593b0cc3e36477f33a54e5be4aad880");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "properties","tenants","payments","alert_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `properties`");
      _db.execSQL("DELETE FROM `tenants`");
      _db.execSQL("DELETE FROM `payments`");
      _db.execSQL("DELETE FROM `alert_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(PropertyDao.class, PropertyDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TenantDao.class, TenantDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PaymentDao.class, PaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AlertHistoryDao.class, AlertHistoryDao_Impl.getRequiredConverters());
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
  public PropertyDao propertyDao() {
    if (_propertyDao != null) {
      return _propertyDao;
    } else {
      synchronized(this) {
        if(_propertyDao == null) {
          _propertyDao = new PropertyDao_Impl(this);
        }
        return _propertyDao;
      }
    }
  }

  @Override
  public TenantDao tenantDao() {
    if (_tenantDao != null) {
      return _tenantDao;
    } else {
      synchronized(this) {
        if(_tenantDao == null) {
          _tenantDao = new TenantDao_Impl(this);
        }
        return _tenantDao;
      }
    }
  }

  @Override
  public PaymentDao paymentDao() {
    if (_paymentDao != null) {
      return _paymentDao;
    } else {
      synchronized(this) {
        if(_paymentDao == null) {
          _paymentDao = new PaymentDao_Impl(this);
        }
        return _paymentDao;
      }
    }
  }

  @Override
  public AlertHistoryDao alertHistoryDao() {
    if (_alertHistoryDao != null) {
      return _alertHistoryDao;
    } else {
      synchronized(this) {
        if(_alertHistoryDao == null) {
          _alertHistoryDao = new AlertHistoryDao_Impl(this);
        }
        return _alertHistoryDao;
      }
    }
  }
}

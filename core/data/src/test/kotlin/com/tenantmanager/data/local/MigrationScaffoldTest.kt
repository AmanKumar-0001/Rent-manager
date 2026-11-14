package com.tenantmanager.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationScaffoldTest {

    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testCreateSchemaVersion1() {
        val db = helper.createDatabase(testDbName, 1)
        assertThat(db).isNotNull()
        assertThat(db.version).isEqualTo(1)

        // Verify tables exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            tableNames.add(cursor.getString(0))
        }
        cursor.close()
        db.close()

        assertThat(tableNames).contains("properties")
        assertThat(tableNames).contains("tenants")
        assertThat(tableNames).contains("payments")
        assertThat(tableNames).contains("alert_history")
    }
}


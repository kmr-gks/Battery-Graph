package com.gukos.battery_graph.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gukos.battery_graph.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsExistingRecords() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO battery_records (timestamp, level) VALUES (1000, 80)")
            db.execSQL("INSERT INTO battery_records (timestamp, level) VALUES (2000, 75)")
        }

        // スキーマが Entity と一致しない場合はここで例外になる
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        db.query(
            "SELECT timestamp, level, isCharging, temperature, voltage " +
                "FROM battery_records ORDER BY timestamp"
        ).use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals(1000L, cursor.getLong(0))
            assertEquals(80, cursor.getInt(1))
            // 追加した列は null
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

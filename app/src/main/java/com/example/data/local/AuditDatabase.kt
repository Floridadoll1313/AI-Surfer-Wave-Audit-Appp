package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_reports")
data class SavedReportEntity(
    @PrimaryKey val id: String,
    val repoSlug: String,
    val runLabel: String,
    val targetUrl: String,
    val payloadJson: String,
    val isBaseline: Boolean,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface ReportDao {
    @Query("SELECT * FROM saved_reports ORDER BY isBaseline DESC, updatedAtMillis DESC")
    fun observeAllReports(): Flow<List<SavedReportEntity>>

    @Query("SELECT COUNT(*) FROM saved_reports")
    suspend fun getReportCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(report: SavedReportEntity)

    @Query("DELETE FROM saved_reports WHERE id = :reportId AND isBaseline = 0")
    suspend fun deleteCustomReport(reportId: String)
}

@Database(entities = [SavedReportEntity::class], version = 1, exportSchema = false)
abstract class SurferAuditDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var instance: SurferAuditDatabase? = null

        fun getInstance(context: Context): SurferAuditDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SurferAuditDatabase::class.java,
                    "surfer_audit_db",
                ).build().also { instance = it }
            }
        }
    }
}

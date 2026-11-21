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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * One Wave Check lead: a client who completed the 5-question quiz.
 * Saved on-device first (offline-safe), then synced to the web endpoint.
 * [syncStatus] is one of "saved", "pending" or "uncertain".
 */
@Entity(tableName = "wave_check_leads")
data class WaveCheckLeadEntity(
    @PrimaryKey val submissionId: String,
    val email: String,
    val businessType: String,
    val teamSize: String,
    val timeDrain: String,
    val lostOpportunity: String,
    val aiPriority: String,
    val score: Int,
    val topCategory: String,
    val opportunitiesJson: String,
    val recommendedAgent: String,
    val confidenceLabel: String,
    val syncStatus: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface WaveCheckLeadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLead(lead: WaveCheckLeadEntity)

    @Query("SELECT * FROM wave_check_leads WHERE submissionId = :submissionId")
    suspend fun getLead(submissionId: String): WaveCheckLeadEntity?

    @Query("SELECT * FROM wave_check_leads WHERE syncStatus IN ('pending', 'uncertain') ORDER BY createdAtMillis ASC")
    suspend fun getUnsyncedLeads(): List<WaveCheckLeadEntity>

    @Query("UPDATE wave_check_leads SET syncStatus = :status, updatedAtMillis = :now WHERE submissionId = :submissionId")
    suspend fun updateSyncStatus(submissionId: String, status: String, now: Long)

    @Query("SELECT * FROM wave_check_leads ORDER BY createdAtMillis DESC")
    fun observeAllLeads(): Flow<List<WaveCheckLeadEntity>>
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `wave_check_leads` (" +
                "`submissionId` TEXT NOT NULL, " +
                "`email` TEXT NOT NULL, " +
                "`businessType` TEXT NOT NULL, " +
                "`teamSize` TEXT NOT NULL, " +
                "`timeDrain` TEXT NOT NULL, " +
                "`lostOpportunity` TEXT NOT NULL, " +
                "`aiPriority` TEXT NOT NULL, " +
                "`score` INTEGER NOT NULL, " +
                "`topCategory` TEXT NOT NULL, " +
                "`opportunitiesJson` TEXT NOT NULL, " +
                "`recommendedAgent` TEXT NOT NULL, " +
                "`confidenceLabel` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, " +
                "`createdAtMillis` INTEGER NOT NULL, " +
                "`updatedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`submissionId`))",
        )
    }
}

@Database(
    entities = [SavedReportEntity::class, WaveCheckLeadEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SurferAuditDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun waveCheckLeadDao(): WaveCheckLeadDao

    companion object {
        @Volatile
        private var instance: SurferAuditDatabase? = null

        fun getInstance(context: Context): SurferAuditDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SurferAuditDatabase::class.java,
                    "surfer_audit_db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}

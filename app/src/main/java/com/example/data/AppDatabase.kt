package com.example.data

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
import com.example.model.CapturedNotification
import com.example.model.FraudScenario
import com.example.model.IndicatorSeverity
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.ThreatFeedItem
import com.example.model.ThreatIndicator
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val scanType: String,
    val originalInput: String,
    val sanitizedInput: String,
    val riskScore: Int,
    val riskLevel: String,
    val scenario: String,
    val summaryExplanation: String,
    val indicatorsJson: String,
    val recommendedActionsJson: String,
    val heuristicScore: Int,
    val aiScore: Int,
    val analysisTimeMs: Long,
    val threatFeedMatch: String?
) {
    fun toDomainModel(): ScanResult {
        val indicatorsList = mutableListOf<ThreatIndicator>()
        try {
            val arr = JSONArray(indicatorsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                indicatorsList.add(
                    ThreatIndicator(
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        severity = IndicatorSeverity.valueOf(obj.getString("severity")),
                        category = obj.getString("category"),
                        technicalDetail = if (obj.has("technicalDetail") && !obj.isNull("technicalDetail")) obj.getString("technicalDetail") else null
                    )
                )
            }
        } catch (_: Exception) {}

        val actionsList = mutableListOf<String>()
        try {
            val arr = JSONArray(recommendedActionsJson)
            for (i in 0 until arr.length()) {
                actionsList.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        return ScanResult(
            id = id,
            timestamp = timestamp,
            scanType = ScanType.valueOf(scanType),
            originalInput = originalInput,
            sanitizedInput = sanitizedInput,
            riskScore = riskScore,
            riskLevel = RiskLevel.valueOf(riskLevel),
            scenario = FraudScenario.valueOf(scenario),
            summaryExplanation = summaryExplanation,
            indicators = indicatorsList,
            recommendedActions = actionsList,
            heuristicScore = heuristicScore,
            aiScore = aiScore,
            analysisTimeMs = analysisTimeMs,
            threatFeedMatch = threatFeedMatch
        )
    }

    companion object {
        fun fromDomainModel(domain: ScanResult): ScanResultEntity {
            val indArray = JSONArray().apply {
                domain.indicators.forEach { ind ->
                    put(JSONObject().apply {
                        put("title", ind.title)
                        put("description", ind.description)
                        put("severity", ind.severity.name)
                        put("category", ind.category)
                        put("technicalDetail", ind.technicalDetail)
                    })
                }
            }
            val actArray = JSONArray().apply {
                domain.recommendedActions.forEach { put(it) }
            }

            return ScanResultEntity(
                id = domain.id,
                timestamp = domain.timestamp,
                scanType = domain.scanType.name,
                originalInput = domain.originalInput,
                sanitizedInput = domain.sanitizedInput,
                riskScore = domain.riskScore,
                riskLevel = domain.riskLevel.name,
                scenario = domain.scenario.name,
                summaryExplanation = domain.summaryExplanation,
                indicatorsJson = indArray.toString(),
                recommendedActionsJson = actArray.toString(),
                heuristicScore = domain.heuristicScore,
                aiScore = domain.aiScore,
                analysisTimeMs = domain.analysisTimeMs,
                threatFeedMatch = domain.threatFeedMatch
            )
        }
    }
}

@Entity(tableName = "threat_feed_items")
data class ThreatFeedEntity(
    @PrimaryKey val id: String,
    val pattern: String,
    val type: String,
    val category: String,
    val severity: String,
    val reason: String,
    val dateAdded: String
) {
    fun toDomain(): ThreatFeedItem {
        return ThreatFeedItem(
            id = id,
            pattern = pattern,
            type = type,
            category = try { FraudScenario.valueOf(category) } catch (_: Exception) { FraudScenario.BANK_KYC_PAN_UPDATE },
            severity = try { IndicatorSeverity.valueOf(severity) } catch (_: Exception) { IndicatorSeverity.CRITICAL },
            reason = reason,
            dateAdded = dateAdded
        )
    }
}

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity)

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ThreatFeedDao {
    @Query("SELECT * FROM threat_feed_items")
    fun getAllThreats(): Flow<List<ThreatFeedEntity>>

    @Query("SELECT * FROM threat_feed_items WHERE pattern LIKE '%' || :query || '%' OR reason LIKE '%' || :query || '%'")
    suspend fun searchThreats(query: String): List<ThreatFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ThreatFeedEntity>)

    @Query("SELECT COUNT(*) FROM threat_feed_items")
    suspend fun getCount(): Int
}

@Entity(tableName = "captured_notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isFlagged: Boolean,
    val flagReason: String?,
    val flaggedAt: Long?,
    val riskScore: Int,
    val riskLevel: String,
    val detectedScamType: String?
) {
    fun toDomain(): CapturedNotification {
        return CapturedNotification(
            id = id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = timestamp,
            isFlagged = isFlagged,
            flagReason = flagReason,
            flaggedAt = flaggedAt,
            riskScore = riskScore,
            riskLevel = try { RiskLevel.valueOf(riskLevel) } catch (_: Exception) { RiskLevel.LOW },
            detectedScamType = detectedScamType
        )
    }

    companion object {
        fun fromDomain(domain: CapturedNotification): NotificationEntity {
            return NotificationEntity(
                id = domain.id,
                packageName = domain.packageName,
                appName = domain.appName,
                title = domain.title,
                content = domain.content,
                timestamp = domain.timestamp,
                isFlagged = domain.isFlagged,
                flagReason = domain.flagReason,
                flaggedAt = domain.flaggedAt,
                riskScore = domain.riskScore,
                riskLevel = domain.riskLevel.name,
                detectedScamType = domain.detectedScamType
            )
        }
    }
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM captured_notifications WHERE isFlagged = 1 ORDER BY timestamp DESC")
    fun getFlaggedNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE captured_notifications SET isFlagged = :isFlagged, flagReason = :reason, flaggedAt = :flaggedAt WHERE id = :id")
    suspend fun updateFlagStatus(id: String, isFlagged: Boolean, reason: String?, flaggedAt: Long?)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM captured_notifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE isFlagged = 1")
    fun getFlaggedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM captured_notifications")
    suspend fun getCount(): Int
}

@Database(
    entities = [ScanResultEntity::class, ThreatFeedEntity::class, NotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun threatFeedDao(): ThreatFeedDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phishguard_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

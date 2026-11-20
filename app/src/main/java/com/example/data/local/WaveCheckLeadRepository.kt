package com.example.data.local

import com.example.data.network.WaveCheckSyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object WaveCheckSyncStatus {
    const val SAVED = "saved"
    const val PENDING = "pending"
    const val UNCERTAIN = "uncertain"
}

/**
 * Wave Check leads: Room first (offline-safe), then sync to the web.
 * Mirrors the web lead record (leadCapture.ts): submission_id, email
 * (trimmed, lowercase), answers, score, top_category, opportunities,
 * recommended_agent, confidence_label, source "wave-audit",
 * report_version 1. The same submissionId is reused on retries, so a
 * retry never creates a second lead for the same Wave Check.
 */
class WaveCheckLeadRepository(
    private val leadDao: WaveCheckLeadDao,
    private val syncClient: WaveCheckSyncClient = WaveCheckSyncClient(),
) {

    fun buildSyncPayload(lead: WaveCheckLeadEntity): String {
        val answers = JSONObject().apply {
            put("businessType", lead.businessType)
            put("teamSize", lead.teamSize)
            put("timeDrain", lead.timeDrain)
            put("lostOpportunity", lead.lostOpportunity)
            put("aiPriority", lead.aiPriority)
        }
        return JSONObject().apply {
            put("submission_id", lead.submissionId)
            put("email", lead.email)
            put("answers", answers)
            put("score", lead.score)
            put("top_category", lead.topCategory)
            put("opportunities", JSONArray(lead.opportunitiesJson))
            put("recommended_agent", lead.recommendedAgent)
            put("confidence_label", lead.confidenceLabel)
            put("source", "wave-audit")
            put("report_version", 1)
        }.toString()
    }

    /**
     * Saves the lead locally as pending, then attempts the server sync.
     * Returns true only when the server confirms the save.
     */
    suspend fun saveAndSync(lead: WaveCheckLeadEntity): Boolean = withContext(Dispatchers.IO) {
        leadDao.upsertLead(lead.copy(syncStatus = WaveCheckSyncStatus.PENDING))
        val saved = syncClient.submitLead(buildSyncPayload(lead))
        leadDao.updateSyncStatus(
            lead.submissionId,
            if (saved) WaveCheckSyncStatus.SAVED else WaveCheckSyncStatus.UNCERTAIN,
            System.currentTimeMillis(),
        )
        saved
    }

    /** Retries the sync for one lead, reusing its submissionId. */
    suspend fun retrySingle(submissionId: String): Boolean = withContext(Dispatchers.IO) {
        val lead = leadDao.getLead(submissionId) ?: return@withContext false
        val saved = syncClient.submitLead(buildSyncPayload(lead))
        leadDao.updateSyncStatus(
            submissionId,
            if (saved) WaveCheckSyncStatus.SAVED else WaveCheckSyncStatus.UNCERTAIN,
            System.currentTimeMillis(),
        )
        saved
    }

    /** Retries every lead whose sync is still pending or uncertain. */
    suspend fun retryPendingSync() = withContext(Dispatchers.IO) {
        leadDao.getUnsyncedLeads().forEach { lead ->
            val saved = runCatching { syncClient.submitLead(buildSyncPayload(lead)) }
                .getOrDefault(false)
            leadDao.updateSyncStatus(
                lead.submissionId,
                if (saved) WaveCheckSyncStatus.SAVED else WaveCheckSyncStatus.UNCERTAIN,
                System.currentTimeMillis(),
            )
        }
    }
}

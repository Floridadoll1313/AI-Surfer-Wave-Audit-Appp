package com.example.data.local

import com.example.data.model.LhrAuditReport
import com.example.data.model.SeedReportProvider
import com.example.data.parser.LhrJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReportRepository(private val reportDao: ReportDao) {

    val reports: Flow<List<LhrAuditReport>> = reportDao.observeAllReports()
        .map { entities ->
            entities.mapNotNull { entity ->
                runCatching { LhrJsonParser.deserializeReport(entity.payloadJson) }.getOrNull()
            }
        }

    suspend fun ensureBaselineSeeded() = withContext(Dispatchers.IO) {
        if (reportDao.getReportCount() == 0) {
            val baseline = SeedReportProvider.createBaselineReport()
            saveReport(baseline)
        }
    }

    suspend fun saveReport(report: LhrAuditReport) = withContext(Dispatchers.IO) {
        val entity = SavedReportEntity(
            id = report.id,
            repoSlug = report.repoSlug,
            runLabel = report.runLabel,
            targetUrl = report.targetUrl,
            payloadJson = LhrJsonParser.serializeReport(report),
            isBaseline = report.isBundledBaseline,
            updatedAtMillis = System.currentTimeMillis(),
        )
        reportDao.upsertReport(entity)
    }

    suspend fun deleteReport(reportId: String) = withContext(Dispatchers.IO) {
        reportDao.deleteCustomReport(reportId)
    }
}

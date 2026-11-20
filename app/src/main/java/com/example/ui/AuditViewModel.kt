package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ReportRepository
import com.example.data.local.SurferAuditDatabase
import com.example.data.model.AuditCategory
import com.example.data.model.CommitFix
import com.example.data.model.LhrAuditReport
import com.example.data.model.SeedReportProvider
import com.example.data.network.PageSpeedClient
import com.example.data.parser.LhrJsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditUiState(
    val reports: List<LhrAuditReport> = emptyList(),
    val activeReport: LhrAuditReport? = null,
    val selectedCategory: AuditCategory = AuditCategory.ALL,
    val searchQuery: String = "",
    val expandedDiffHashes: Set<String> = setOf("730994b"),
    val focusedCommitHash: String? = "730994b",
    val isLoadingScan: Boolean = false,
    val scanError: String? = null,
    val statusMessage: String? = null,
) {
    val visibleCommits: List<CommitFix>
        get() {
            val commits = activeReport?.commits.orEmpty()
            val byCategory = if (selectedCategory == AuditCategory.ALL) {
                commits
            } else {
                commits.filter { it.category == selectedCategory }
            }
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return byCategory
            return byCategory.filter { commit ->
                commit.title.lowercase().contains(q) ||
                    commit.commitHash.lowercase().contains(q) ||
                    commit.description.lowercase().contains(q) ||
                    commit.auditImproved.lowercase().contains(q) ||
                    commit.files.any { it.lowercase().contains(q) } ||
                    commit.targetSelectors.any { it.lowercase().contains(q) }
            }
        }

    val selectedCommits: List<CommitFix>
        get() = activeReport?.commits?.filter { it.isSelected }.orEmpty()
}

private data class TransientControlState(
    val activeReportId: String = SeedReportProvider.BASELINE_REPORT_ID,
    val selectedCategory: AuditCategory = AuditCategory.ALL,
    val searchQuery: String = "",
    val expandedDiffHashes: Set<String> = setOf("730994b"),
    val focusedCommitHash: String? = "730994b",
    val isLoadingScan: Boolean = false,
    val scanError: String? = null,
    val statusMessage: String? = null,
)

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SurferAuditDatabase.getInstance(application)
    private val repository = ReportRepository(database.reportDao())
    private val pageSpeedClient = PageSpeedClient()

    private val controls = MutableStateFlow(TransientControlState())

    val uiState: StateFlow<AuditUiState> = combine(
        repository.reports,
        controls,
    ) { reports, ctrl ->
        val active = reports.firstOrNull { it.id == ctrl.activeReportId } ?: reports.firstOrNull()
        val focused = active?.commits?.firstOrNull { it.commitHash == ctrl.focusedCommitHash }?.commitHash
            ?: active?.commits?.firstOrNull()?.commitHash
        AuditUiState(
            reports = reports,
            activeReport = active,
            selectedCategory = ctrl.selectedCategory,
            searchQuery = ctrl.searchQuery,
            expandedDiffHashes = ctrl.expandedDiffHashes,
            focusedCommitHash = focused,
            isLoadingScan = ctrl.isLoadingScan,
            scanError = ctrl.scanError,
            statusMessage = ctrl.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureBaselineSeeded()
        }
    }

    fun selectCategory(category: AuditCategory) {
        controls.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun toggleDiffExpanded(commitHash: String) {
        controls.update { state ->
            val updated = state.expandedDiffHashes.toMutableSet()
            if (!updated.add(commitHash)) updated.remove(commitHash)
            state.copy(expandedDiffHashes = updated, focusedCommitHash = commitHash)
        }
    }

    fun focusCommit(commitHash: String) {
        controls.update { it.copy(focusedCommitHash = commitHash) }
    }

    fun toggleCommitSelected(commitHash: String) {
        val current = uiState.value.activeReport ?: return
        val updatedCommits = current.commits.map { commit ->
            if (commit.commitHash == commitHash) commit.copy(isSelected = !commit.isSelected) else commit
        }
        viewModelScope.launch {
            repository.saveReport(current.copy(commits = updatedCommits))
        }
    }

    fun selectAllVisible() {
        val current = uiState.value.activeReport ?: return
        val visibleHashes = uiState.value.visibleCommits.map { it.commitHash }.toSet()
        val updatedCommits = current.commits.map { commit ->
            if (commit.commitHash in visibleHashes) commit.copy(isSelected = true) else commit
        }
        viewModelScope.launch {
            repository.saveReport(current.copy(commits = updatedCommits))
        }
    }

    fun clearAllSelections() {
        val current = uiState.value.activeReport ?: return
        val updatedCommits = current.commits.map { it.copy(isSelected = false) }
        viewModelScope.launch {
            repository.saveReport(current.copy(commits = updatedCommits))
        }
    }

    fun activateReport(reportId: String) {
        val target = uiState.value.reports.firstOrNull { it.id == reportId } ?: return
        val firstHash = target.commits.firstOrNull()?.commitHash
        controls.update {
            it.copy(
                activeReportId = reportId,
                selectedCategory = AuditCategory.ALL,
                searchQuery = "",
                expandedDiffHashes = setOfNotNull(firstHash),
                focusedCommitHash = firstHash,
                statusMessage = "Loaded ${target.repoSlug}",
            )
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            controls.update { state ->
                val nextId = if (state.activeReportId == reportId) {
                    SeedReportProvider.BASELINE_REPORT_ID
                } else {
                    state.activeReportId
                }
                state.copy(activeReportId = nextId, statusMessage = "Report removed")
            }
        }
    }

    fun importRawLhrJson(rawJson: String, sourceLabel: String) {
        viewModelScope.launch {
            val result = runCatching { LhrJsonParser.parseLhrJson(rawJson, sourceLabel) }
            result.onSuccess { report ->
                repository.saveReport(report)
                val firstHash = report.commits.firstOrNull()?.commitHash
                controls.update {
                    it.copy(
                        activeReportId = report.id,
                        selectedCategory = AuditCategory.ALL,
                        searchQuery = "",
                        expandedDiffHashes = setOfNotNull(firstHash),
                        focusedCommitHash = firstHash,
                        scanError = null,
                        statusMessage = "Imported ${report.failingAudits.size} failing LHR audits from ${report.repoSlug}",
                    )
                }
            }.onFailure { err ->
                controls.update {
                    it.copy(scanError = err.message ?: "Failed to parse Lighthouse JSON.")
                }
            }
        }
    }

    fun runLivePageSpeedAudit(url: String, strategy: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            controls.update { it.copy(scanError = "Enter a valid website URL (e.g. https://oceantidedrop.com)") }
            return
        }
        controls.update { it.copy(isLoadingScan = true, scanError = null) }
        viewModelScope.launch {
            val result = pageSpeedClient.runAudit(trimmed, strategy)
            result.onSuccess { report ->
                repository.saveReport(report)
                val firstHash = report.commits.firstOrNull()?.commitHash
                controls.update {
                    it.copy(
                        isLoadingScan = false,
                        activeReportId = report.id,
                        selectedCategory = AuditCategory.ALL,
                        searchQuery = "",
                        expandedDiffHashes = setOfNotNull(firstHash),
                        focusedCommitHash = firstHash,
                        scanError = null,
                        statusMessage = "Live Lighthouse audit complete for ${report.repoSlug}",
                    )
                }
            }.onFailure { err ->
                controls.update {
                    it.copy(
                        isLoadingScan = false,
                        scanError = err.message ?: "Unable to fetch PageSpeed Insights audit.",
                    )
                }
            }
        }
    }

    fun showMessage(msg: String) {
        controls.update { it.copy(statusMessage = msg) }
    }

    fun consumeStatusMessage() {
        controls.update { it.copy(statusMessage = null) }
    }

    fun clearScanError() {
        controls.update { it.copy(scanError = null) }
    }

    fun buildCombinedPatch(): String {
        val report = uiState.value.activeReport ?: return ""
        val selected = report.commits.filter { it.isSelected }
        if (selected.isEmpty()) return ""
        return buildString {
            selected.forEachIndexed { idx, commit ->
                if (idx > 0) append("\n")
                append("From ${commit.commitHash}000000000000000000000000000000000 Mon Sep 17 00:00:00 2001\n")
                append("From: Surfer Audit <audit@oceantidedrop.com>\n")
                append("Subject: [PATCH ${commit.indexLabel}/${selected.size.toString().padStart(2, '0')}] ${commit.title}\n")
                append("X-LHR-Category: ${commit.category.label}\n")
                append("X-LHR-Selectors: ${commit.targetSelectors.joinToString(", ")}\n\n")
                append(commit.description).append("\n---\n")
                append(commit.unifiedDiff).append("\n")
            }
        }
    }

    fun buildPullRequestTitle(): String {
        val report = uiState.value.activeReport ?: return "Lighthouse JSON audit fixes"
        val selectedCount = report.selectedCount
        return "perf(lhr): apply $selectedCount targeted Lighthouse JSON fixes (${report.runLabel.substringBefore(" ·")})"
    }

    fun buildPullRequestMarkdown(): String {
        val report = uiState.value.activeReport ?: return ""
        val selected = report.commits.filter { it.isSelected }
        val byCat = selected.groupBy { it.category }
        return buildString {
            append("## Lighthouse JSON Audit Report · ${report.repoSlug}\n\n")
            append("Targeted code commits mapped directly to failing audits and DOM selectors in the Lighthouse Result (LHR) JSON report.\n\n")
            append("### Categories Improved (${selected.size} commits selected)\n")
            AuditCategory.specificCategories().forEach { cat ->
                val count = byCat[cat]?.size ?: 0
                if (count > 0) {
                    append("- **${cat.label}**: $count commit(s)\n")
                }
            }
            append("\n### Commit Coverage & DOM Selectors\n")
            selected.forEach { commit ->
                append("- **`${commit.commitHash}`** (${commit.category.label}): ${commit.title}\n")
                append("  - Audit: ${commit.auditImproved}\n")
                append("  - Selectors: `${commit.targetSelectors.joinToString("`, `")}`\n")
                append("  - Files: `${commit.files.joinToString("`, `")}`\n")
            }
        }
    }
}

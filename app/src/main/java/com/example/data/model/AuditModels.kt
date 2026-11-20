package com.example.data.model

enum class AuditCategory(val label: String, val lhrKey: String) {
    ALL("All", "all"),
    PERFORMANCE("Performance", "performance"),
    ACCESSIBILITY("Accessibility", "accessibility"),
    BEST_PRACTICES("Best practices", "best-practices"),
    SEO("SEO", "seo");

    companion object {
        fun specificCategories(): List<AuditCategory> = listOf(
            PERFORMANCE,
            ACCESSIBILITY,
            BEST_PRACTICES,
            SEO,
        )

        fun fromLhrKey(key: String): AuditCategory = when (key.lowercase()) {
            "performance" -> PERFORMANCE
            "accessibility" -> ACCESSIBILITY
            "best-practices", "best_practices" -> BEST_PRACTICES
            "seo" -> SEO
            else -> BEST_PRACTICES
        }
    }
}

data class CommitFix(
    val indexLabel: String,
    val category: AuditCategory,
    val commitHash: String,
    val title: String,
    val description: String,
    val auditImproved: String,
    val targetSelectors: List<String>,
    val files: List<String>,
    val unifiedDiff: String,
    val isSelected: Boolean = true,
)

data class LhrFailingAudit(
    val id: String,
    val title: String,
    val description: String,
    val category: AuditCategory,
    val scorePercent: Int,
    val displayValue: String,
    val selectors: List<String>,
    val linkedCommitHash: String? = null,
)

data class LhrAuditReport(
    val id: String,
    val repoSlug: String,
    val runLabel: String,
    val targetUrl: String,
    val fetchTime: String,
    val lighthouseVersion: String,
    val strategy: String,
    val categoryScores: Map<AuditCategory, Int>,
    val failingAudits: List<LhrFailingAudit>,
    val commits: List<CommitFix>,
    val isBundledBaseline: Boolean = false,
) {
    fun commitCountFor(category: AuditCategory): Int {
        return if (category == AuditCategory.ALL) {
            commits.size
        } else {
            commits.count { it.category == category }
        }
    }

    val selectedCount: Int
        get() = commits.count { it.isSelected }
}

package com.example.data.parser

import com.example.data.model.AuditCategory
import com.example.data.model.CommitFix
import com.example.data.model.LhrAuditReport
import com.example.data.model.LhrFailingAudit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

object LhrJsonParser {

    fun parseLhrJson(rawJson: String, sourceLabel: String = "Imported LHR JSON"): LhrAuditReport {
        val root = JSONObject(rawJson.trim())
        val lhr = if (root.has("lighthouseResult")) {
            root.getJSONObject("lighthouseResult")
        } else {
            root
        }

        if (!lhr.has("categories") && !lhr.has("audits")) {
            throw IllegalArgumentException("JSON does not appear to be a valid Lighthouse Result (missing 'categories' or 'audits').")
        }

        val finalUrl = lhr.optString("finalDisplayedUrl")
            .ifBlank { lhr.optString("finalUrl") }
            .ifBlank { lhr.optString("requestedUrl") }
            .ifBlank { "https://audited-site.local/" }

        val fetchTime = lhr.optString("fetchTime").ifBlank { "Imported" }
        val lhVersion = lhr.optString("lighthouseVersion").ifBlank { "LHR" }
        val formFactor = lhr.optJSONObject("configSettings")
            ?.optString("formFactor")
            ?.replaceFirstChar { it.uppercase() }
            ?.ifBlank { "Mobile" }
            ?: "Mobile"

        val categoriesJson = lhr.optJSONObject("categories")
        val auditsJson = lhr.optJSONObject("audits")

        val categoryScores = mutableMapOf<AuditCategory, Int>()
        val auditToCategory = mutableMapOf<String, AuditCategory>()

        if (categoriesJson != null) {
            val catKeys = listOf("performance", "accessibility", "best-practices", "seo")
            for (key in catKeys) {
                val catObj = categoriesJson.optJSONObject(key) ?: continue
                val catEnum = AuditCategory.fromLhrKey(key)
                if (!catObj.isNull("score")) {
                    categoryScores[catEnum] = (catObj.optDouble("score", 0.0) * 100.0).roundToInt().coerceIn(0, 100)
                }
                val refs = catObj.optJSONArray("auditRefs")
                if (refs != null) {
                    for (i in 0 until refs.length()) {
                        val refObj = refs.optJSONObject(i) ?: continue
                        val auditId = refObj.optString("id")
                        if (auditId.isNotBlank() && !auditToCategory.containsKey(auditId)) {
                            auditToCategory[auditId] = catEnum
                        }
                    }
                }
            }
        }

        val failingAudits = mutableListOf<LhrFailingAudit>()
        val generatedCommits = mutableListOf<CommitFix>()

        if (auditsJson != null) {
            val keys = auditsJson.keys()
            var commitIndex = 1
            while (keys.hasNext()) {
                val auditId = keys.next()
                val auditObj = auditsJson.optJSONObject(auditId) ?: continue
                val mode = auditObj.optString("scoreDisplayMode", "binary")
                if (mode == "notApplicable" || mode == "informative" || mode == "manual") continue
                if (auditObj.isNull("score")) continue

                val score = auditObj.optDouble("score", 1.0)
                if (score >= 0.9) continue

                val title = auditObj.optString("title", auditId)
                val description = cleanMarkdownLinks(auditObj.optString("description", ""))
                val displayValue = auditObj.optString("displayValue", "")
                val category = auditToCategory[auditId] ?: inferCategoryFromAuditId(auditId)
                val selectors = extractSelectors(auditObj)
                val scorePercent = (score * 100.0).roundToInt().coerceIn(0, 100)
                val hash = buildDeterministicHash(auditId, finalUrl)

                failingAudits.add(
                    LhrFailingAudit(
                        id = auditId,
                        title = title,
                        description = description,
                        category = category,
                        scorePercent = scorePercent,
                        displayValue = displayValue.ifBlank { "Score: $scorePercent/100" },
                        selectors = selectors.ifEmpty { listOf("audit#$auditId") },
                        linkedCommitHash = hash,
                    )
                )

                val idxLabel = commitIndex.toString().padStart(2, '0')
                commitIndex++
                val targetFile = inferTargetFile(category, auditId)
                generatedCommits.add(
                    CommitFix(
                        indexLabel = idxLabel,
                        category = category,
                        commitHash = hash,
                        title = title,
                        description = description.ifBlank {
                            "Remediation mapped from failing Lighthouse audit '$auditId'."
                        },
                        auditImproved = "${category.label} · $auditId (${displayValue.ifBlank { "$scorePercent/100" }})",
                        targetSelectors = selectors.ifEmpty { listOf("audit#$auditId") },
                        files = listOf(targetFile),
                        unifiedDiff = buildDiagnosticDiffSnippet(
                            file = targetFile,
                            auditId = auditId,
                            title = title,
                            selectors = selectors,
                            displayValue = displayValue,
                        ),
                        isSelected = true,
                    )
                )
            }
        }

        val hostName = runCatching {
            java.net.URI(finalUrl).host?.removePrefix("www.") ?: sourceLabel
        }.getOrDefault(sourceLabel)

        return LhrAuditReport(
            id = "lhr-${UUID.randomUUID().toString().take(8)}",
            repoSlug = hostName.ifBlank { sourceLabel },
            runLabel = sourceLabel,
            targetUrl = finalUrl,
            fetchTime = fetchTime,
            lighthouseVersion = lhVersion,
            strategy = formFactor,
            categoryScores = categoryScores,
            failingAudits = failingAudits.sortedBy { it.scorePercent },
            commits = generatedCommits,
            isBundledBaseline = false,
        )
    }

    private fun extractSelectors(auditObj: JSONObject): List<String> {
        val selectors = LinkedHashSet<String>()
        val details = auditObj.optJSONObject("details") ?: return emptyList()
        val items = details.optJSONArray("items") ?: return emptyList()
        for (i in 0 until minOf(items.length(), 6)) {
            val item = items.optJSONObject(i) ?: continue
            val node = item.optJSONObject("node")
            if (node != null) {
                val sel = node.optString("selector").ifBlank { node.optString("nodeLabel") }
                if (sel.isNotBlank()) selectors.add(sel.take(96))
            }
            val url = item.optString("url")
            if (selectors.size < 4 && url.isNotBlank()) {
                val shortUrl = url.substringAfterLast('/').substringBefore('?').ifBlank { url }
                selectors.add("resource[$shortUrl]".take(96))
            }
        }
        return selectors.toList()
    }

    private fun cleanMarkdownLinks(raw: String): String {
        return raw.replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1").trim()
    }

    private fun inferCategoryFromAuditId(auditId: String): AuditCategory {
        return when {
            auditId.contains("aria") || auditId.contains("label") || auditId.contains("contrast") ||
                auditId.contains("alt") || auditId.contains("heading") -> AuditCategory.ACCESSIBILITY
            auditId.contains("meta") || auditId.contains("canonical") || auditId.contains("robots") ||
                auditId.contains("crawlable") || auditId.contains("hreflang") -> AuditCategory.SEO
            auditId.contains("csp") || auditId.contains("console") || auditId.contains("doctype") ||
                auditId.contains("https") -> AuditCategory.BEST_PRACTICES
            else -> AuditCategory.PERFORMANCE
        }
    }

    private fun inferTargetFile(category: AuditCategory, auditId: String): String {
        return when {
            auditId.contains("canonical") || auditId.contains("meta") || auditId.contains("preconnect") -> "index.html"
            category == AuditCategory.ACCESSIBILITY -> "src/components/Layout.tsx"
            category == AuditCategory.SEO -> "index.html"
            category == AuditCategory.BEST_PRACTICES -> "src/App.tsx"
            else -> "src/pages/Home.tsx"
        }
    }

    private fun buildDeterministicHash(auditId: String, url: String): String {
        val raw = (auditId.hashCode() * 31 + url.hashCode()).toUInt().toString(16)
        return raw.padStart(7, 'a').take(7)
    }

    private fun buildDiagnosticDiffSnippet(
        file: String,
        auditId: String,
        title: String,
        selectors: List<String>,
        displayValue: String,
    ): String {
        val selectorComment = selectors.firstOrNull() ?: auditId
        val note = if (displayValue.isNotBlank()) " ($displayValue)" else ""
        return """
diff --git a/$file b/$file
--- a/$file
+++ b/$file
@@ -1,4 +1,6 @@
+<!-- LHR Audit Fix: $auditId$note -->
+<!-- Target Selector: $selectorComment -->
+<!-- Action: $title -->
        """.trimIndent()
    }

    fun serializeReport(report: LhrAuditReport): String {
        val obj = JSONObject().apply {
            put("id", report.id)
            put("repoSlug", report.repoSlug)
            put("runLabel", report.runLabel)
            put("targetUrl", report.targetUrl)
            put("fetchTime", report.fetchTime)
            put("lighthouseVersion", report.lighthouseVersion)
            put("strategy", report.strategy)
            put("isBundledBaseline", report.isBundledBaseline)

            val scoresObj = JSONObject()
            report.categoryScores.forEach { (cat, score) ->
                scoresObj.put(cat.lhrKey, score)
            }
            put("categoryScores", scoresObj)

            val failingArray = JSONArray()
            report.failingAudits.forEach { audit ->
                failingArray.put(
                    JSONObject().apply {
                        put("id", audit.id)
                        put("title", audit.title)
                        put("description", audit.description)
                        put("category", audit.category.lhrKey)
                        put("scorePercent", audit.scorePercent)
                        put("displayValue", audit.displayValue)
                        put("selectors", JSONArray(audit.selectors))
                        put("linkedCommitHash", audit.linkedCommitHash ?: "")
                    }
                )
            }
            put("failingAudits", failingArray)

            val commitsArray = JSONArray()
            report.commits.forEach { commit ->
                commitsArray.put(
                    JSONObject().apply {
                        put("indexLabel", commit.indexLabel)
                        put("category", commit.category.lhrKey)
                        put("commitHash", commit.commitHash)
                        put("title", commit.title)
                        put("description", commit.description)
                        put("auditImproved", commit.auditImproved)
                        put("targetSelectors", JSONArray(commit.targetSelectors))
                        put("files", JSONArray(commit.files))
                        put("unifiedDiff", commit.unifiedDiff)
                        put("isSelected", commit.isSelected)
                    }
                )
            }
            put("commits", commitsArray)
        }
        return obj.toString()
    }

    fun deserializeReport(json: String): LhrAuditReport {
        val obj = JSONObject(json)
        val scores = mutableMapOf<AuditCategory, Int>()
        val scoresObj = obj.optJSONObject("categoryScores")
        if (scoresObj != null) {
            val keys = scoresObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                scores[AuditCategory.fromLhrKey(key)] = scoresObj.optInt(key, 0)
            }
        }

        val failingAudits = mutableListOf<LhrFailingAudit>()
        val failingArray = obj.optJSONArray("failingAudits")
        if (failingArray != null) {
            for (i in 0 until failingArray.length()) {
                val item = failingArray.getJSONObject(i)
                failingAudits.add(
                    LhrFailingAudit(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        description = item.optString("description"),
                        category = AuditCategory.fromLhrKey(item.getString("category")),
                        scorePercent = item.optInt("scorePercent", 0),
                        displayValue = item.optString("displayValue"),
                        selectors = jsonArrayToList(item.optJSONArray("selectors")),
                        linkedCommitHash = item.optString("linkedCommitHash").ifBlank { null },
                    )
                )
            }
        }

        val commits = mutableListOf<CommitFix>()
        val commitsArray = obj.optJSONArray("commits")
        if (commitsArray != null) {
            for (i in 0 until commitsArray.length()) {
                val item = commitsArray.getJSONObject(i)
                commits.add(
                    CommitFix(
                        indexLabel = item.getString("indexLabel"),
                        category = AuditCategory.fromLhrKey(item.getString("category")),
                        commitHash = item.getString("commitHash"),
                        title = item.getString("title"),
                        description = item.optString("description"),
                        auditImproved = item.optString("auditImproved"),
                        targetSelectors = jsonArrayToList(item.optJSONArray("targetSelectors")),
                        files = jsonArrayToList(item.optJSONArray("files")),
                        unifiedDiff = item.optString("unifiedDiff"),
                        isSelected = item.optBoolean("isSelected", true),
                    )
                )
            }
        }

        return LhrAuditReport(
            id = obj.getString("id"),
            repoSlug = obj.getString("repoSlug"),
            runLabel = obj.getString("runLabel"),
            targetUrl = obj.getString("targetUrl"),
            fetchTime = obj.optString("fetchTime"),
            lighthouseVersion = obj.optString("lighthouseVersion"),
            strategy = obj.optString("strategy", "Mobile"),
            categoryScores = scores,
            failingAudits = failingAudits,
            commits = commits,
            isBundledBaseline = obj.optBoolean("isBundledBaseline", false),
        )
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.optString(i))
            }
        }
    }
}

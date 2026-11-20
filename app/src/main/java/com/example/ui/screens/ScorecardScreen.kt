package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AuditCategory
import com.example.data.model.LhrFailingAudit
import com.example.ui.AuditUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.LighthouseScoreGauge
import com.example.ui.components.SelectorChip
import com.example.ui.theme.CoralErrorBright
import com.example.ui.theme.EmeraldSuccessBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAuditColors
import com.example.ui.theme.PerfAmberBright

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScorecardScreen(
    uiState: AuditUiState,
    onJumpToCommit: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = uiState.activeReport ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.scorecard_title),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = "${report.repoSlug} · ${report.strategy} · ${report.targetUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, LocalAuditColors.current.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            maxItemsInEachRow = 4,
                        ) {
                            AuditCategory.specificCategories().forEach { category ->
                                val score = report.categoryScores[category] ?: 0
                                LighthouseScoreGauge(
                                    score = score,
                                    label = category.label,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = "${stringResource(R.string.failing_audits_title)} (${report.failingAudits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (report.failingAudits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, LocalAuditColors.current.cardBorder),
                    ) {
                        Text(
                            text = "No failing audits detected in this Lighthouse LHR report (all scores ≥ 90).",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        } else {
            items(
                items = report.failingAudits,
                key = { it.id },
            ) { audit ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    FailingAuditCard(
                        audit = audit,
                        onJumpToCommit = onJumpToCommit,
                        onCopyText = onCopyText,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FailingAuditCard(
    audit: LhrFailingAudit,
    onJumpToCommit: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreColor = when {
        audit.scorePercent >= 90 -> EmeraldSuccessBright
        audit.scorePercent >= 50 -> PerfAmberBright
        else -> CoralErrorBright
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("failing_audit_${audit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LocalAuditColors.current.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryBadge(category = audit.category)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = scoreColor.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = "Score ${audit.scorePercent}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                }

                if (!audit.linkedCommitHash.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onJumpToCommit(audit.linkedCommitHash) }
                            .testTag("jump_commit_${audit.linkedCommitHash}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = audit.linkedCommitHash,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open commit fix",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = audit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (audit.displayValue.isNotBlank()) {
                Text(
                    text = audit.displayValue,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = scoreColor,
                )
            }

            if (audit.description.isNotBlank()) {
                Text(
                    text = audit.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (audit.selectors.isNotEmpty()) {
                Spacer(modifier = Modifier.size(2.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    audit.selectors.forEach { sel ->
                        SelectorChip(
                            selector = sel,
                            onCopyClick = { copied ->
                                onCopyText(copied, "Selector copied: $copied")
                            },
                        )
                    }
                }
            }
        }
    }
}

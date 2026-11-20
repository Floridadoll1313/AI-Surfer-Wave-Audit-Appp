package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditCategory
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CoralErrorBright
import com.example.ui.theme.DiffAddBg
import com.example.ui.theme.DiffAddText
import com.example.ui.theme.DiffHunkBg
import com.example.ui.theme.DiffHunkText
import com.example.ui.theme.DiffRemoveBg
import com.example.ui.theme.DiffRemoveText
import com.example.ui.theme.EmeraldSuccessBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAuditColors
import com.example.ui.theme.PerfAmberBright
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalHeaderBg
import com.example.ui.theme.TerminalMuted
import com.example.ui.theme.TerminalText

@Composable
fun categoryAccentColor(category: AuditCategory): Color {
    val auditColors = LocalAuditColors.current
    return when (category) {
        AuditCategory.PERFORMANCE -> auditColors.performance
        AuditCategory.ACCESSIBILITY -> auditColors.accessibility
        AuditCategory.BEST_PRACTICES -> auditColors.bestPractices
        AuditCategory.SEO -> auditColors.seo
        AuditCategory.ALL -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun categoryContainerColor(category: AuditCategory): Color {
    val auditColors = LocalAuditColors.current
    return when (category) {
        AuditCategory.PERFORMANCE -> auditColors.performanceContainer
        AuditCategory.ACCESSIBILITY -> auditColors.accessibilityContainer
        AuditCategory.BEST_PRACTICES -> auditColors.bestPracticesContainer
        AuditCategory.SEO -> auditColors.seoContainer
        AuditCategory.ALL -> MaterialTheme.colorScheme.primaryContainer
    }
}

fun categoryIcon(category: AuditCategory): ImageVector {
    return when (category) {
        AuditCategory.PERFORMANCE -> Icons.Filled.Bolt
        AuditCategory.ACCESSIBILITY -> Icons.Outlined.AccessibilityNew
        AuditCategory.BEST_PRACTICES -> Icons.Filled.VerifiedUser
        AuditCategory.SEO -> Icons.Filled.Public
        AuditCategory.ALL -> Icons.Filled.CheckCircle
    }
}

@Composable
fun CategoryBadge(
    category: AuditCategory,
    modifier: Modifier = Modifier,
) {
    val accent = categoryAccentColor(category)
    val container = categoryContainerColor(category)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SelectorChip(
    selector: String,
    onCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCopyClick(selector) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = selector,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy selector $selector",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
fun UnifiedDiffViewer(
    diffText: String,
    fileSummary: String,
    onCopyDiff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = diffText.lines()
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalBg)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalHeaderBg)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = DiffHunkText,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = fileSummary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TerminalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onCopyDiff,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy diff",
                    tint = TerminalMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(vertical = 8.dp),
        ) {
            lines.forEachIndexed { index, rawLine ->
                val isAdd = rawLine.startsWith("+") && !rawLine.startsWith("+++")
                val isRemove = rawLine.startsWith("-") && !rawLine.startsWith("---")
                val isHunk = rawLine.startsWith("@@")
                val isFileHeader = rawLine.startsWith("diff --git") ||
                    rawLine.startsWith("---") ||
                    rawLine.startsWith("+++")

                val rowBg = when {
                    isAdd -> DiffAddBg
                    isRemove -> DiffRemoveBg
                    isHunk -> DiffHunkBg
                    else -> Color.Transparent
                }
                val textColor = when {
                    isAdd -> DiffAddText
                    isRemove -> DiffRemoveText
                    isHunk -> DiffHunkText
                    isFileHeader -> TerminalMuted
                    else -> TerminalText
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(horizontal = 10.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (index + 1).toString().padStart(2, ' '),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TerminalMuted,
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rawLine.ifEmpty { " " },
                        style = CodeTextStyle,
                        color = textColor,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
fun LighthouseScoreGauge(
    score: Int,
    label: String,
    size: Dp = 84.dp,
    modifier: Modifier = Modifier,
) {
    val clamped = score.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 700),
        label = "gauge_sweep",
    )
    val gaugeColor = when {
        clamped >= 90 -> EmeraldSuccessBright
        clamped >= 50 -> PerfAmberBright
        else -> CoralErrorBright
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 8.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = gaugeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Text(
                text = clamped.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = gaugeColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AuditCategory
import com.example.data.model.CommitFix
import com.example.data.model.LhrAuditReport
import com.example.ui.AuditUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.SelectorChip
import com.example.ui.components.UnifiedDiffViewer
import com.example.ui.components.categoryAccentColor
import com.example.ui.components.categoryContainerColor
import com.example.ui.components.categoryIcon
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAuditColors
import com.example.ui.theme.OceanNavyDeep

@Composable
fun CommitsScreen(
    uiState: AuditUiState,
    onSelectCategory: (AuditCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleCommitSelected: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearAll: () -> Unit,
    onToggleDiffExpanded: (String) -> Unit,
    onFocusCommit: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onOpenPatchSheet: () -> Unit,
    onOpenPrSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = uiState.activeReport ?: return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 840.dp

        Column(modifier = Modifier.fillMaxSize()) {
            if (isTwoPane) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            AuditReportHeroBanner(report = report)
                        }
                        item {
                            CategoryCoverageGrid(
                                report = report,
                                selectedCategory = uiState.selectedCategory,
                                onSelectCategory = onSelectCategory,
                            )
                        }
                        item {
                            FilterAndSearchHeader(
                                report = report,
                                selectedCategory = uiState.selectedCategory,
                                searchQuery = uiState.searchQuery,
                                onSelectCategory = onSelectCategory,
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectAllVisible = onSelectAllVisible,
                                onClearAll = onClearAll,
                            )
                        }
                        if (uiState.visibleCommits.isEmpty()) {
                            item {
                                EmptyCommitsCard(onReset = {
                                    onSelectCategory(AuditCategory.ALL)
                                    onSearchQueryChange("")
                                })
                            }
                        } else {
                            items(
                                items = uiState.visibleCommits,
                                key = { it.commitHash },
                            ) { commit ->
                                CommitCard(
                                    commit = commit,
                                    isDiffExpanded = false,
                                    isFocused = uiState.focusedCommitHash == commit.commitHash,
                                    showInlineDiffToggle = false,
                                    onToggleSelected = { onToggleCommitSelected(commit.commitHash) },
                                    onCardClick = { onFocusCommit(commit.commitHash) },
                                    onToggleDiff = { onFocusCommit(commit.commitHash) },
                                    onCopyText = onCopyText,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    val focusedCommit = uiState.visibleCommits.firstOrNull {
                        it.commitHash == uiState.focusedCommitHash
                    } ?: uiState.visibleCommits.firstOrNull()

                    Box(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .padding(16.dp),
                    ) {
                        if (focusedCommit != null) {
                            CommitInspectorPane(
                                commit = focusedCommit,
                                onCopyText = onCopyText,
                            )
                        } else {
                            EmptyCommitsCard(onReset = {
                                onSelectCategory(AuditCategory.ALL)
                                onSearchQueryChange("")
                            })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AuditReportHeroBanner(
                                report = report,
                                modifier = Modifier.widthIn(max = 680.dp),
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CategoryCoverageGrid(
                                report = report,
                                selectedCategory = uiState.selectedCategory,
                                onSelectCategory = onSelectCategory,
                                modifier = Modifier.widthIn(max = 680.dp),
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            FilterAndSearchHeader(
                                report = report,
                                selectedCategory = uiState.selectedCategory,
                                searchQuery = uiState.searchQuery,
                                onSelectCategory = onSelectCategory,
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectAllVisible = onSelectAllVisible,
                                onClearAll = onClearAll,
                                modifier = Modifier.widthIn(max = 680.dp),
                            )
                        }
                    }

                    if (uiState.visibleCommits.isEmpty()) {
                        item {
                            EmptyCommitsCard(onReset = {
                                onSelectCategory(AuditCategory.ALL)
                                onSearchQueryChange("")
                            })
                        }
                    } else {
                        items(
                            items = uiState.visibleCommits,
                            key = { it.commitHash },
                        ) { commit ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CommitCard(
                                    commit = commit,
                                    isDiffExpanded = commit.commitHash in uiState.expandedDiffHashes,
                                    isFocused = false,
                                    showInlineDiffToggle = true,
                                    onToggleSelected = { onToggleCommitSelected(commit.commitHash) },
                                    onCardClick = { onToggleDiffExpanded(commit.commitHash) },
                                    onToggleDiff = { onToggleDiffExpanded(commit.commitHash) },
                                    onCopyText = onCopyText,
                                    modifier = Modifier.widthIn(max = 680.dp),
                                )
                            }
                        }
                    }
                }
            }

            SelectionActionBottomBar(
                selectedCount = report.selectedCount,
                totalCount = report.commits.size,
                onDownloadPatch = onOpenPatchSheet,
                onCreatePr = onOpenPrSheet,
            )
        }
    }
}

@Composable
private fun AuditReportHeroBanner(
    report: LhrAuditReport,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = OceanNavyDeep,
        shadowElevation = 2.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_ocean_lighthouse_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.34f,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                OceanNavyDeep.copy(alpha = 0.72f),
                                OceanNavyDeep.copy(alpha = 0.94f),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = report.runLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7DD3FC),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "LHR ${report.lighthouseVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Text(
                    text = report.repoSlug,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = stringResource(R.string.report_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )

                Text(
                    text = stringResource(R.string.report_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                )
            }
        }
    }
}

@Composable
private fun CategoryCoverageGrid(
    report: LhrAuditReport,
    selectedCategory: AuditCategory,
    onSelectCategory: (AuditCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val auditColors = LocalAuditColors.current
    val categories = AuditCategory.specificCategories()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, auditColors.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.categories_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.categories_verified_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = auditColors.success,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowCats.forEach { cat ->
                            val count = report.commitCountFor(cat)
                            val isSelected = selectedCategory == cat
                            val accent = categoryAccentColor(cat)
                            val container = categoryContainerColor(cat)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectCategory(
                                            if (selectedCategory == cat) AuditCategory.ALL else cat
                                        )
                                    }
                                    .testTag("category_card_${cat.lhrKey}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accent else auditColors.cardBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(cat),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Column {
                                            Text(
                                                text = cat.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = if (count == 1) "1 commit" else "$count commits",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = auditColors.success,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterAndSearchHeader(
    report: LhrAuditReport,
    selectedCategory: AuditCategory,
    searchQuery: String,
    onSelectCategory: (AuditCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_commits_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search",
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_commits_input"),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuditCategory.entries.forEach { category ->
                val count = report.commitCountFor(category)
                val selected = selectedCategory == category
                val accent = categoryAccentColor(category)
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            text = "${category.label} ($count)",
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.16f),
                        selectedLabelColor = accent,
                    ),
                    modifier = Modifier.testTag("filter_chip_${category.lhrKey}"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Commit coverage (${report.commits.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onSelectAllVisible,
                    modifier = Modifier.testTag("select_all_button"),
                ) {
                    Text(stringResource(R.string.action_select_all))
                }
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_button"),
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommitCard(
    commit: CommitFix,
    isDiffExpanded: Boolean,
    isFocused: Boolean,
    showInlineDiffToggle: Boolean,
    onToggleSelected: () -> Unit,
    onCardClick: () -> Unit,
    onToggleDiff: () -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val auditColors = LocalAuditColors.current
    val accent = categoryAccentColor(commit.category)
    val borderColor = when {
        isFocused -> accent
        commit.isSelected -> accent.copy(alpha = 0.45f)
        else -> auditColors.cardBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("commit_card_${commit.commitHash}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 3.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = commit.indexLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                    CategoryBadge(category = commit.category)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onCopyText(commit.commitHash, "Commit ${commit.commitHash} copied") },
                    ) {
                        Text(
                            text = commit.commitHash,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                }

                Checkbox(
                    checked = commit.isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.testTag("checkbox_${commit.commitHash}"),
                )
            }

            Text(
                text = commit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = commit.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_category_audit_improved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = commit.auditImproved,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${stringResource(R.string.label_json_target_node)} · ${stringResource(R.string.label_tap_to_copy_selector)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    commit.targetSelectors.forEach { selector ->
                        SelectorChip(
                            selector = selector,
                            onCopyClick = { sel ->
                                onCopyText(sel, "Selector copied: $sel")
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = commit.files.joinToString(", "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (showInlineDiffToggle) {
                    TextButton(
                        onClick = onToggleDiff,
                        modifier = Modifier.testTag("toggle_diff_${commit.commitHash}"),
                    ) {
                        Text(
                            text = if (isDiffExpanded) {
                                stringResource(R.string.action_hide_diff)
                            } else {
                                stringResource(R.string.action_view_diff)
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isDiffExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (showInlineDiffToggle) {
                AnimatedVisibility(
                    visible = isDiffExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    UnifiedDiffViewer(
                        diffText = commit.unifiedDiff,
                        fileSummary = commit.files.joinToString(" · "),
                        onCopyDiff = {
                            onCopyText(commit.unifiedDiff, "Diff for ${commit.commitHash} copied")
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommitInspectorPane(
    commit: CommitFix,
    onCopyText: (String, String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Commit ${commit.indexLabel} · ${commit.commitHash}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                )
                Spacer(modifier = Modifier.weight(1f))
                CategoryBadge(category = commit.category)
            }

            Text(
                text = commit.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = commit.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.label_json_target_node),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                commit.targetSelectors.forEach { selector ->
                    SelectorChip(
                        selector = selector,
                        onCopyClick = { sel -> onCopyText(sel, "Selector copied: $sel") },
                    )
                }
            }

            UnifiedDiffViewer(
                diffText = commit.unifiedDiff,
                fileSummary = commit.files.joinToString(" · "),
                onCopyDiff = {
                    onCopyText(commit.unifiedDiff, "Diff for ${commit.commitHash} copied")
                },
            )
        }
    }
}

@Composable
private fun EmptyCommitsCard(onReset: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.msg_empty_commits),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = onReset) {
                Text("Reset filters")
            }
        }
    }
}

@Composable
private fun SelectionActionBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onDownloadPatch: () -> Unit,
    onCreatePr: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedCount > 0) {
                        stringResource(R.string.selected_count_format, selectedCount, totalCount)
                    } else {
                        stringResource(R.string.no_commits_selected_hint)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedCount > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDownloadPatch,
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("download_patch_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_download_patch),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onCreatePr,
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("create_pr_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MergeType,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_create_pr),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

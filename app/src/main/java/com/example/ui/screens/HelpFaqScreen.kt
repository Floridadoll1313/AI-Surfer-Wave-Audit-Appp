package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAuditColors

private enum class FaqTopic(val label: String) {
    ALL("All"),
    LHR_JSON("LHR JSON"),
    COMMITS_DIFFS("Commits & Diffs"),
    PATCH_PR("Patches & PRs"),
    STORAGE("Storage"),
}

private data class FaqItem(
    val id: String,
    val topic: FaqTopic,
    val question: String,
    val answer: String,
    val commandHint: String? = null,
)

private val FaqCatalog = listOf(
    FaqItem(
        id = "lhr_mapping",
        topic = FaqTopic.LHR_JSON,
        question = "What is a Lighthouse Result (LHR) JSON report and how are commits mapped to it?",
        answer = "A Lighthouse Result (LHR) JSON file contains category scores (Performance, Accessibility, Best practices, SEO) along with individual audit results and exact DOM node selectors for failing elements. Surfer Audit maps each targeted code commit (such as 730994b or be7cb81) directly to the failing LHR audit ID and CSS/DOM selectors it resolves, so you can verify coverage before applying changes.",
    ),
    FaqItem(
        id = "diff_inspection",
        topic = FaqTopic.COMMITS_DIFFS,
        question = "How do I inspect code diffs and filter commits by audit category?",
        answer = "On the Audit Fixes tab, tap any category card under 'Categories improved with code' or use the horizontal filter chips (Performance, Accessibility, Best practices, SEO) to narrow down commits. You can also search by commit hash, file path, or DOM selector. Tap 'View diff' on any commit card (or select a commit in tablet two-pane mode) to inspect line-by-line additions (+) and removals (-) in the syntax-highlighted diff viewer.",
    ),
    FaqItem(
        id = "download_patch",
        topic = FaqTopic.PATCH_PR,
        question = "How does 'Download .patch' work and how do I apply it to my Git repository?",
        answer = "Use the checkboxes on each commit card (or 'Select all' / 'Clear') to choose which fixes to include. Tapping 'Download .patch' compiles every selected commit into a single standard unified Git patch containing commit headers, LHR metadata, and unified diff hunks. You can save it as a .patch file to device storage, copy it, or share it, then apply it directly to your local repository.",
        commandHint = "git apply --check surfer-lhr-fixes.patch && git am < surfer-lhr-fixes.patch",
    ),
    FaqItem(
        id = "create_pr",
        topic = FaqTopic.PATCH_PR,
        question = "What does 'Create pull request' generate?",
        answer = "Tapping 'Create pull request' builds a structured PR title and GitHub-flavored Markdown summary listing only your selected commits, the count of fixes per Lighthouse category, the failing audits resolved, modified file paths, and target DOM selectors. You can copy the Markdown to your clipboard, share it with your team, or launch GitHub directly via 'Open GitHub'.",
    ),
    FaqItem(
        id = "import_and_scan",
        topic = FaqTopic.LHR_JSON,
        question = "How do I import my own Lighthouse .json file or run a live PageSpeed scan?",
        answer = "Open the 'LHR JSON' tab. To analyze an existing report offline, tap 'Import LHR .json file' to pick a Lighthouse CLI JSON export or PageSpeed Insights JSON file from your device, or tap 'Paste LHR JSON' to paste raw JSON directly. To audit a live site, enter a public http:// or https:// URL, choose Mobile or Desktop strategy, and tap 'Run live audit' to fetch and parse results from Google PageSpeed Insights v5.",
    ),
    FaqItem(
        id = "dom_selectors",
        topic = FaqTopic.COMMITS_DIFFS,
        question = "How can I use the JSON target node selector chips?",
        answer = "Each commit card and Scorecard failing audit displays monospace selector chips (for example, head > link[rel='preconnect'], .lead-leak-visual img, or div[role='dialog']). Tapping any chip copies the exact CSS/DOM selector to your clipboard so you can paste it into Chrome DevTools, grep your codebase, or verify elements in automated tests.",
    ),
    FaqItem(
        id = "local_storage",
        topic = FaqTopic.STORAGE,
        question = "Where are imported reports and commit selections saved?",
        answer = "Surfer Audit stores the otdaisurfer/AI-Surfer- Run 2 baseline report, all imported or live-scanned LHR reports, and your commit selection checkboxes locally on-device in a Room SQLite database. You can switch active reports from the Saved audit reports list on the LHR JSON tab at any time without an internet connection, and remove custom reports with the delete icon.",
    ),
)

@Composable
fun HelpFaqScreen(
    modifier: Modifier = Modifier,
) {
    var selectedTopic by rememberSaveable { mutableStateOf(FaqTopic.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expandedIds by rememberSaveable { mutableStateOf(setOf(FaqCatalog.first().id)) }

    val filteredFaqs = FaqCatalog.filter { item ->
        val matchesTopic = selectedTopic == FaqTopic.ALL || item.topic == selectedTopic
        val q = searchQuery.trim().lowercase()
        val matchesQuery = q.isEmpty() ||
            item.question.lowercase().contains(q) ||
            item.answer.lowercase().contains(q) ||
            (item.commandHint?.lowercase()?.contains(q) == true)
        matchesTopic && matchesQuery
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.help_title),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = stringResource(R.string.help_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.help_search_hint),
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
                                IconButton(onClick = { searchQuery = "" }) {
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
                            .testTag("help_search_input"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FaqTopic.entries.forEach { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { selectedTopic = topic },
                                label = { Text(topic.label) },
                                modifier = Modifier.testTag("faq_topic_${topic.name.lowercase()}"),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${filteredFaqs.size} question(s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { expandedIds = filteredFaqs.map { it.id }.toSet() },
                                modifier = Modifier.testTag("faq_expand_all_button"),
                            ) {
                                Text("Expand all")
                            }
                            TextButton(
                                onClick = { expandedIds = emptySet() },
                                modifier = Modifier.testTag("faq_collapse_all_button"),
                            ) {
                                Text("Collapse all")
                            }
                        }
                    }
                }
            }
        }

        if (filteredFaqs.isEmpty()) {
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
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No FAQ entries match your search.",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            TextButton(onClick = {
                                selectedTopic = FaqTopic.ALL
                                searchQuery = ""
                            }) {
                                Text("Reset filters")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredFaqs,
                key = { it.id },
            ) { item ->
                val isExpanded = item.id in expandedIds
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    FaqCard(
                        item = item,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedIds = if (isExpanded) {
                                expandedIds - item.id
                            } else {
                                expandedIds + item.id
                            }
                        },
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqCard(
    item: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val auditColors = LocalAuditColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("faq_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else auditColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = item.topic.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse answer" else "Expand answer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = item.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!item.commandHint.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "CLI Example",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = item.commandHint,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

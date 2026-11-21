package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.CreatePullRequestSheet
import com.example.ui.components.PatchExportSheet
import com.example.ui.screens.CommitsScreen
import com.example.ui.screens.HelpFaqScreen
import com.example.ui.screens.LhrInspectorScreen
import com.example.ui.screens.ScorecardScreen
import com.example.ui.screens.WaveCheckScreen

const val ROUTE_COMMITS = "commits"
const val ROUTE_SCORECARD = "scorecard"
const val ROUTE_INSPECTOR = "inspector"
const val ROUTE_WAVE_CHECK = "wave_check"
const val ROUTE_HELP = "help"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopLevelDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_COMMITS,
        labelRes = R.string.nav_commits,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
    ),
    NavDestinationItem(
        route = ROUTE_SCORECARD,
        labelRes = R.string.nav_scorecard,
        selectedIcon = Icons.Filled.Speed,
        unselectedIcon = Icons.Outlined.Speed,
    ),
    NavDestinationItem(
        route = ROUTE_INSPECTOR,
        labelRes = R.string.nav_inspector,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code,
    ),
    NavDestinationItem(
        route = ROUTE_WAVE_CHECK,
        labelRes = R.string.nav_wave_check,
        selectedIcon = Icons.Filled.Waves,
        unselectedIcon = Icons.Outlined.Waves,
    ),
    NavDestinationItem(
        route = ROUTE_HELP,
        labelRes = R.string.nav_help,
        selectedIcon = Icons.AutoMirrored.Filled.Help,
        unselectedIcon = Icons.AutoMirrored.Outlined.HelpOutline,
    ),
)

@Composable
fun SurferAuditApp(
    viewModel: AuditViewModel = viewModel(),
    waveCheckViewModel: WaveCheckViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_COMMITS

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPatchSheet by rememberSaveable { mutableStateOf(false) }
    var showPrSheet by rememberSaveable { mutableStateOf(false) }

    val patchSavedMsg = stringResource(R.string.msg_patch_saved)
    val createPatchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-patch"),
    ) { uri: Uri? ->
        if (uri != null) {
            val content = viewModel.buildCombinedPatch()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.showMessage(patchSavedMsg)
                showPatchSheet = false
            }.onFailure { err ->
                viewModel.showMessage(err.message ?: "Unable to write patch file")
            }
        }
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Empty JSON file")
            }.onSuccess { rawText ->
                viewModel.importRawLhrJson(rawText, "Imported LHR File")
            }.onFailure { err ->
                viewModel.showMessage(err.message ?: "Unable to read JSON file")
            }
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeStatusMessage()
        }
    }

    val copyToClipboard: (String, String) -> Unit = { text, toastMsg ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Surfer Audit", text))
        viewModel.showMessage(toastMsg)
    }

    val shareText: (String, String) -> Unit = { subject, text ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    if (showPatchSheet) {
        val patch = viewModel.buildCombinedPatch()
        val count = uiState.activeReport?.selectedCount ?: 0
        PatchExportSheet(
            selectedCount = count,
            patchContent = patch,
            onSaveFile = {
                val slug = (uiState.activeReport?.repoSlug ?: "lighthouse-audit")
                    .replace("/", "-")
                    .lowercase()
                createPatchLauncher.launch("$slug-lhr-fixes.patch")
            },
            onCopyPatch = { copyToClipboard(patch, "Unified .patch copied to clipboard") },
            onSharePatch = { shareText("Lighthouse Audit Patch ($count commits)", patch) },
            onDismiss = { showPatchSheet = false },
        )
    }

    if (showPrSheet) {
        val report = uiState.activeReport
        val prTitle = viewModel.buildPullRequestTitle()
        val prMarkdown = viewModel.buildPullRequestMarkdown()
        CreatePullRequestSheet(
            repoSlug = report?.repoSlug ?: "repository",
            prTitle = prTitle,
            prMarkdown = prMarkdown,
            selectedCount = report?.selectedCount ?: 0,
            onCopyMarkdown = {
                copyToClipboard("$prTitle\n\n$prMarkdown", "PR summary copied to clipboard")
            },
            onSharePr = {
                shareText(prTitle, "$prTitle\n\n$prMarkdown")
            },
            onOpenGitHub = {
                val slug = report?.repoSlug.orEmpty()
                val targetUrl = if (slug.contains("/")) {
                    "https://github.com/$slug/pulls"
                } else {
                    "https://github.com"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                context.startActivity(intent)
            },
            onDismiss = { showPrSheet = false },
        )
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_COMMITS,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_COMMITS) {
                        CommitsScreen(
                            uiState = uiState,
                            onSelectCategory = viewModel::selectCategory,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onToggleCommitSelected = viewModel::toggleCommitSelected,
                            onSelectAllVisible = viewModel::selectAllVisible,
                            onClearAll = viewModel::clearAllSelections,
                            onToggleDiffExpanded = viewModel::toggleDiffExpanded,
                            onFocusCommit = viewModel::focusCommit,
                            onCopyText = copyToClipboard,
                            onOpenPatchSheet = { showPatchSheet = true },
                            onOpenPrSheet = { showPrSheet = true },
                        )
                    }

                    composable(ROUTE_SCORECARD) {
                        ScorecardScreen(
                            uiState = uiState,
                            onJumpToCommit = { hash ->
                                viewModel.focusCommit(hash)
                                viewModel.toggleDiffExpanded(hash)
                                navigateTo(ROUTE_COMMITS)
                            },
                            onCopyText = copyToClipboard,
                        )
                    }

                    composable(ROUTE_INSPECTOR) {
                        LhrInspectorScreen(
                            uiState = uiState,
                            onPickJsonFile = {
                                openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            onPasteJson = { raw ->
                                viewModel.importRawLhrJson(raw, "Pasted LHR JSON")
                                navigateTo(ROUTE_COMMITS)
                            },
                            onRunLiveScan = viewModel::runLivePageSpeedAudit,
                            onActivateReport = { id ->
                                viewModel.activateReport(id)
                                navigateTo(ROUTE_COMMITS)
                            },
                            onDeleteReport = viewModel::deleteReport,
                            onClearError = viewModel::clearScanError,
                        )
                    }

                    composable(ROUTE_WAVE_CHECK) {
                        WaveCheckScreen(viewModel = waveCheckViewModel)
                    }

                    composable(ROUTE_HELP) {
                        HelpFaqScreen()
                    }
                }
            }
        }
    }
}

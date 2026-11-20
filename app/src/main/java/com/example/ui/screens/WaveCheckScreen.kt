package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.WaveCheckSyncStatus
import com.example.data.model.WAVE_CHECK_OFFERS
import com.example.data.model.WAVE_CHECK_QUESTIONS
import com.example.data.model.WaveCheckOffer
import com.example.data.model.WaveCheckQuestion
import com.example.data.model.recommendOffer
import com.example.ui.WaveCheckStep
import com.example.ui.WaveCheckUiState
import com.example.ui.WaveCheckViewModel
import com.example.ui.theme.LocalAuditColors
import kotlinx.coroutines.delay

/**
 * Client Wave Check flow, mirroring the web screens in
 * src/pages/wave-audit/WaveAudit.tsx and FullWaveReport.tsx:
 * intro -> 5 questions -> teaser result -> email gate -> full report.
 */
@Composable
fun WaveCheckScreen(viewModel: WaveCheckViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.step) {
        WaveCheckStep.INTRO -> WaveCheckIntro(onStart = viewModel::startQuiz)
        WaveCheckStep.QUESTIONS -> WaveCheckQuestions(
            uiState = uiState,
            onSelectOption = viewModel::selectOption,
            onBack = viewModel::goBack,
            onShowScore = viewModel::showScore,
        )
        WaveCheckStep.TEASER -> WaveCheckTeaser(
            uiState = uiState,
            onUnlockReport = viewModel::goToEmailGate,
            onRestart = viewModel::restart,
        )
        WaveCheckStep.EMAIL -> WaveCheckEmailGate(
            uiState = uiState,
            onEmailChange = viewModel::updateEmail,
            onSubmit = viewModel::submitEmail,
            onBack = viewModel::backToTeaser,
        )
        WaveCheckStep.REPORT -> WaveCheckFullReport(
            uiState = uiState,
            onRetrySave = viewModel::retrySave,
            onRestart = viewModel::restart,
        )
    }
}

@Composable
private fun WaveCheckIntro(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Waves,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.wave_check_intro_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = stringResource(R.string.wave_check_intro_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.wave_check_intro_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wave_check_start"),
        ) {
            Text(stringResource(R.string.wave_check_start))
        }
    }
}

@Composable
private fun WaveCheckQuestions(
    uiState: WaveCheckUiState,
    onSelectOption: (String) -> Unit,
    onBack: () -> Unit,
    onShowScore: () -> Unit,
) {
    val question: WaveCheckQuestion = WAVE_CHECK_QUESTIONS[uiState.questionIndex]
    val selectedValue = uiState.answers.valueFor(question.key)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.wave_check_question_of_format,
                    uiState.questionIndex + 1,
                    WAVE_CHECK_QUESTIONS.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${((uiState.questionIndex + 1) * 100) / WAVE_CHECK_QUESTIONS.size}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = (uiState.questionIndex + 1).toFloat() / WAVE_CHECK_QUESTIONS.size,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = question.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        question.options.forEach { option ->
            val selected = selectedValue == option.value
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectOption(option.value) }
                    .testTag("wave_check_option_${option.value}"),
                border = if (selected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, LocalAuditColors.current.cardBorder)
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (uiState.questionIndex == WAVE_CHECK_QUESTIONS.lastIndex && uiState.showScoreButton) {
            Button(
                onClick = onShowScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wave_check_see_score"),
            ) {
                Text(stringResource(R.string.wave_check_see_score))
            }
        }
        if (uiState.questionIndex > 0) {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.wave_check_back))
            }
        }
    }
}

@Composable
private fun WaveCheckTeaser(
    uiState: WaveCheckUiState,
    onUnlockReport: () -> Unit,
    onRestart: () -> Unit,
) {
    val result = uiState.result ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.wave_check_score_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = result.score.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 64.sp,
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.wave_check_score_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = LocalAuditColors.current.performanceContainer,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.wave_check_opportunity_level),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalAuditColors.current.performance,
                        )
                        Text(
                            text = result.confidenceLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.wave_check_teaser_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.wave_check_biggest_wave_format, result.topCategory),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(12.dp))
                result.opportunities.forEach { opportunity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = opportunity,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.wave_check_recommended_agent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = result.recommendedAgent,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
        Button(
            onClick = onUnlockReport,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wave_check_unlock"),
        ) {
            Text(stringResource(R.string.wave_check_unlock_button))
        }
        TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wave_check_start_over))
        }
    }
}

@Composable
private fun WaveCheckEmailGate(
    uiState: WaveCheckUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.wave_check_unlock_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.wave_check_unlock_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.wave_check_unlock_copy),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.wave_check_email_label)) },
            placeholder = { Text(stringResource(R.string.wave_check_email_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wave_check_email"),
        )
        uiState.saveErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wave_check_submit_email"),
        ) {
            Text(
                if (uiState.isSubmitting) {
                    stringResource(R.string.wave_check_saving)
                } else {
                    stringResource(R.string.wave_check_open_report)
                },
            )
        }
        Text(
            text = stringResource(R.string.wave_check_email_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.wave_check_back))
        }
    }
}

@Composable
private fun WaveCheckFullReport(
    uiState: WaveCheckUiState,
    onRetrySave: () -> Unit,
    onRestart: () -> Unit,
) {
    val result = uiState.result ?: return
    val report = uiState.report ?: return
    val context = LocalContext.current
    val recommendedOfferId = recommendOffer(uiState.answers)
    var copied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    val copyReport: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("AI Wave Report", uiState.reportText),
        )
        copied = true
    }

    val openLink: (String) -> Unit = { href ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href)))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.wave_check_report_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.businessSnapshot,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wave_check_score_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = report.score.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 48.sp,
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                        Text(
                            text = report.opportunityLevel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LocalAuditColors.current.success,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = LocalAuditColors.current.successContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.wave_check_saved_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.wave_check_saved_subtitle_format, uiState.email),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (uiState.syncStatus == WaveCheckSyncStatus.UNCERTAIN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.wave_check_save_error),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onRetrySave) {
                            Text(stringResource(R.string.wave_check_retry_save))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.wave_check_what_means)) {
                Text(
                    text = report.diagnosis,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.wave_check_priority_actions)) {
                report.priorityActions.forEachIndexed { index, action ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.wave_check_plan_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            report.plan.forEach { phase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = phase.window,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = phase.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = phase.action,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (result.revenueLeaks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.wave_check_leaks_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuditColors.current.performance,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.wave_check_leaks_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.revenueLeaks.forEach { leak ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = leak.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = LocalAuditColors.current.performanceContainer,
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.wave_check_leak_impact_format,
                                            leak.impact,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuditColors.current.performance,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = leak.signal,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text(
                                    text = stringResource(R.string.wave_check_fix_label) + " ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = leak.recommendedFix,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${leak.recommendedAgent} · ${leak.recommendedOffer}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.wave_check_signals_title)) {
                report.metrics.forEach { metric ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(text = "🏄 ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = metric,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.wave_check_agent_title)) {
                Text(
                    text = report.agentName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.agentFit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = LocalAuditColors.current.performanceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.wave_check_first_move),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuditColors.current.performance,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.firstRecommendation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.wave_check_offer_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wave_check_offer_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wave_check_offer_copy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            WAVE_CHECK_OFFERS.forEach { offer ->
                OfferCard(
                    offer = offer,
                    recommended = offer.id == recommendedOfferId,
                    onOpen = { openLink(offer.href) },
                )
            }
        }

        item {
            OutlinedButton(
                onClick = copyReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wave_check_copy_report"),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (copied) {
                        stringResource(R.string.wave_check_copied)
                    } else {
                        stringResource(R.string.wave_check_copy_report)
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wave_check_receipt_format, uiState.submissionId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.wave_check_start_over))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun OfferCard(
    offer: WaveCheckOffer,
    recommended: Boolean,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        border = if (recommended) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, LocalAuditColors.current.cardBorder)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (recommended) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (recommended) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = stringResource(R.string.wave_check_offer_recommended),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = offer.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = offer.price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = offer.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(offer.cta)
            }
        }
    }
}

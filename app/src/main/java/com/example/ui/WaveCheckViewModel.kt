package com.example.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.SurferAuditDatabase
import com.example.data.local.WaveCheckLeadEntity
import com.example.data.local.WaveCheckLeadRepository
import com.example.data.local.WaveCheckSyncStatus
import com.example.data.model.WAVE_CHECK_QUESTIONS
import com.example.data.model.WaveAuditReport
import com.example.data.model.WaveAuditResult
import com.example.data.model.WaveCheckAnswers
import com.example.data.model.buildWaveAuditReport
import com.example.data.model.calculateWaveAuditResult
import com.example.data.model.formatWaveAuditReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

enum class WaveCheckStep {
    INTRO,
    QUESTIONS,
    TEASER,
    EMAIL,
    REPORT,
}

data class WaveCheckUiState(
    val step: WaveCheckStep = WaveCheckStep.INTRO,
    val questionIndex: Int = 0,
    val answers: WaveCheckAnswers = WaveCheckAnswers(),
    val showScoreButton: Boolean = false,
    val email: String = "",
    val isSubmitting: Boolean = false,
    /** String resource id for the current error, or null. */
    val saveErrorRes: Int? = null,
    val result: WaveAuditResult? = null,
    val report: WaveAuditReport? = null,
    val submissionId: String = "",
    val syncStatus: String = WaveCheckSyncStatus.PENDING,
    val reportText: String = "",
)

/**
 * Client Wave Check flow, mirroring the web screens in
 * src/pages/wave-audit/WaveAudit.tsx: intro -> 5 questions -> teaser
 * result (free, no email) -> email gate -> full report. The lead is
 * saved to Room first, then synced to the web endpoint.
 */
class WaveCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SurferAuditDatabase.getInstance(application)
    private val repository = WaveCheckLeadRepository(database.waveCheckLeadDao())

    private val _uiState = MutableStateFlow(WaveCheckUiState())
    val uiState: StateFlow<WaveCheckUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null

    init {
        // Retry any leads whose sync was still pending or uncertain.
        viewModelScope.launch {
            runCatching { repository.retryPendingSync() }
        }
    }

    fun startQuiz() {
        advanceJob?.cancel()
        _uiState.value = WaveCheckUiState(step = WaveCheckStep.QUESTIONS)
    }

    fun restart() {
        advanceJob?.cancel()
        _uiState.value = WaveCheckUiState()
    }

    /** Selects an option; advances after a short delay, as on the web. */
    fun selectOption(value: String) {
        val state = _uiState.value
        if (state.step != WaveCheckStep.QUESTIONS) return
        val question = WAVE_CHECK_QUESTIONS[state.questionIndex]
        val updated = state.answers.withAnswer(question.key, value)
        advanceJob?.cancel()
        if (state.questionIndex < WAVE_CHECK_QUESTIONS.lastIndex) {
            _uiState.update { it.copy(answers = updated) }
            advanceJob = viewModelScope.launch {
                delay(180)
                _uiState.update { it.copy(questionIndex = it.questionIndex + 1) }
            }
        } else {
            _uiState.update { it.copy(answers = updated, showScoreButton = true) }
        }
    }

    fun goBack() {
        val state = _uiState.value
        if (state.step != WaveCheckStep.QUESTIONS || state.questionIndex == 0) return
        advanceJob?.cancel()
        _uiState.update {
            it.copy(questionIndex = it.questionIndex - 1, showScoreButton = false)
        }
    }

    /** Computes the score and opens the free teaser result. */
    fun showScore() {
        val state = _uiState.value
        if (!state.answers.isComplete()) return
        val result = calculateWaveAuditResult(state.answers)
        _uiState.update {
            it.copy(
                result = result,
                report = buildWaveAuditReport(state.answers, result),
                step = WaveCheckStep.TEASER,
            )
        }
    }

    fun goToEmailGate() {
        _uiState.update { it.copy(step = WaveCheckStep.EMAIL, saveErrorRes = null) }
    }

    fun backToTeaser() {
        _uiState.update { it.copy(step = WaveCheckStep.TEASER, saveErrorRes = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, saveErrorRes = null) }
    }

    /** Validates the email, saves the lead locally, then syncs it. */
    fun submitEmail() {
        val state = _uiState.value
        val result = state.result ?: return
        val report = state.report ?: return
        val email = state.email.trim().lowercase()
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(saveErrorRes = R.string.wave_check_email_invalid) }
            return
        }
        val submissionId = state.submissionId.ifBlank { UUID.randomUUID().toString() }
        val entity = WaveCheckLeadEntity(
            submissionId = submissionId,
            email = email,
            businessType = state.answers.businessType,
            teamSize = state.answers.teamSize,
            timeDrain = state.answers.timeDrain,
            lostOpportunity = state.answers.lostOpportunity,
            aiPriority = state.answers.aiPriority,
            score = result.score,
            topCategory = result.topCategory,
            opportunitiesJson = JSONArray(result.opportunities).toString(),
            recommendedAgent = result.recommendedAgent,
            confidenceLabel = result.confidenceLabel,
            syncStatus = WaveCheckSyncStatus.PENDING,
        )
        _uiState.update {
            it.copy(
                isSubmitting = true,
                saveErrorRes = null,
                submissionId = submissionId,
                email = email,
            )
        }
        viewModelScope.launch {
            val saved = runCatching { repository.saveAndSync(entity) }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    syncStatus = if (saved) WaveCheckSyncStatus.SAVED else WaveCheckSyncStatus.UNCERTAIN,
                    saveErrorRes = if (saved) null else R.string.wave_check_save_error,
                    step = if (saved) WaveCheckStep.REPORT else it.step,
                    reportText = if (saved) formatWaveAuditReport(report, submissionId, result) else it.reportText,
                )
            }
        }
    }

    /** Retries the sync for the current submission, reusing its id. */
    fun retrySave() {
        val state = _uiState.value
        val result = state.result ?: return
        val report = state.report ?: return
        if (state.submissionId.isBlank()) return
        _uiState.update { it.copy(isSubmitting = true, saveErrorRes = null) }
        viewModelScope.launch {
            val saved = runCatching { repository.retrySingle(state.submissionId) }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    syncStatus = if (saved) WaveCheckSyncStatus.SAVED else WaveCheckSyncStatus.UNCERTAIN,
                    saveErrorRes = if (saved) null else R.string.wave_check_save_error,
                    step = if (saved) WaveCheckStep.REPORT else it.step,
                    reportText = if (saved) formatWaveAuditReport(report, state.submissionId, result) else it.reportText,
                )
            }
        }
    }
}

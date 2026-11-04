package com.metro.feedback.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.feedback.data.GitHubIssueLauncher
import com.metro.feedback.data.LogcatCapture
import com.metro.feedback.data.LogcatCollector
import com.metro.feedback.data.MetricsRepository
import com.metro.feedback.data.MetricsSnapshot
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FeedbackPivot(val index: Int) {
    Feedback(0),
    Metrics(1),
    ;

    companion object {
        fun fromIndex(index: Int): FeedbackPivot =
            entries.firstOrNull { it.index == index } ?: Feedback
    }
}

sealed interface LogcatStatus {
    data object Idle : LogcatStatus
    data object Collecting : LogcatStatus
    data class Ready(val capture: LogcatCapture) : LogcatStatus
    data class Empty(val file: File) : LogcatStatus
    data class Error(val message: String) : LogcatStatus
}

sealed interface SubmitStatus {
    data object Idle : SubmitStatus
    data object Opening : SubmitStatus
    data object Opened : SubmitStatus
    data class Error(val message: String) : SubmitStatus
}

class FeedbackState(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    var pivot by mutableStateOf(FeedbackPivot.Feedback)
        private set

    var title by mutableStateOf("")
        private set

    var logcatStatus by mutableStateOf<LogcatStatus>(LogcatStatus.Idle)
        private set

    var submitStatus by mutableStateOf<SubmitStatus>(SubmitStatus.Idle)
        private set

    var metrics by mutableStateOf<MetricsSnapshot?>(null)
        private set

    var metricsLoading by mutableStateOf(false)
        private set

    private var collectJob: Job? = null
    private var metricsJob: Job? = null

    fun setPivot(index: Int) {
        pivot = FeedbackPivot.fromIndex(index)
        if (pivot == FeedbackPivot.Metrics) {
            refreshMetrics()
        }
    }

    fun updateTitle(value: String) {
        title = value
        if (submitStatus !is SubmitStatus.Idle) {
            submitStatus = SubmitStatus.Idle
        }
    }

    fun collectLogcat() {
        if (collectJob?.isActive == true) return
        logcatStatus = LogcatStatus.Collecting
        collectJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { LogcatCollector.collect(appContext) }
            }
            logcatStatus = result.fold(
                onSuccess = { capture ->
                    if (capture.lineCount == 0) {
                        LogcatStatus.Empty(capture.file)
                    } else {
                        LogcatStatus.Ready(capture)
                    }
                },
                onFailure = { error ->
                    LogcatStatus.Error(error.message ?: error.toString())
                },
            )
        }
    }

    fun submit() {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            submitStatus = SubmitStatus.Error("title required")
            return
        }
        submitStatus = SubmitStatus.Opening
        scope.launch {
            val file = when (val status = logcatStatus) {
                is LogcatStatus.Ready -> status.capture.file
                is LogcatStatus.Empty -> status.file
                else -> null
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitHubIssueLauncher.openIssue(appContext, trimmed, file)
                }
            }
            submitStatus = result.fold(
                onSuccess = { SubmitStatus.Opened },
                onFailure = { error ->
                    SubmitStatus.Error(error.message ?: error.toString())
                },
            )
        }
    }

    fun refreshMetrics() {
        if (metricsJob?.isActive == true) return
        metricsLoading = true
        metricsJob = scope.launch {
            val snap = withContext(Dispatchers.IO) {
                runCatching { MetricsRepository.snapshot(appContext) }.getOrNull()
            }
            metrics = snap
            metricsLoading = false
        }
    }

    fun openUsageAccess() {
        MetricsRepository.openUsageAccessSettings(appContext)
    }

    fun attachedLogcatFile(): File? = when (val status = logcatStatus) {
        is LogcatStatus.Ready -> status.capture.file
        is LogcatStatus.Empty -> status.file
        else -> null
    }
}

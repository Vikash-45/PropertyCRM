package com.propertycrm.app.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.propertycrm.app.data.local.AppDatabase
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingManager(
    private val context: Context,
    private val aiClient: AIGatewayClient,
    private val customerRepository: CustomerRepository,
    private val preferenceManager: PreferenceManager
) {
    private var mediaRecorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var activeCustomerId: Long = 0L
    private var activeCustomerName: String = ""
    private var activeCustomerPhone: String = ""
    private var isRecordingActive = false

    fun isRecording(): Boolean = isRecordingActive
    fun getActiveCustomerName(): String = activeCustomerName

    /**
     * Starts recording a call or in-person audio consultation for a customer.
     */
    fun startRecording(customerId: Long, customerName: String, customerPhone: String): Boolean {
        if (isRecordingActive) return true

        try {
            val dir = File(context.filesDir, "call_recordings").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "call_${customerId}_$timestamp.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            activeRecordingFile = file
            activeCustomerId = customerId
            activeCustomerName = customerName
            activeCustomerPhone = customerPhone
            isRecordingActive = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecordingActive = false
            mediaRecorder?.release()
            mediaRecorder = null
            return false
        }
    }

    /**
     * Stops the active recording and returns the saved audio file.
     */
    fun stopRecording(): File? {
        if (!isRecordingActive) return null
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecordingActive = false
        }
        return activeRecordingFile
    }

    /**
     * Auto-summarizes the call using the AI Gateway, creates a CustomerInteraction
     * of type AI_CALL_SUMMARY, and links it directly into the customer's timeline.
     */
    suspend fun autoSummarizeAndImport(
        customerId: Long,
        customerName: String,
        customerPhone: String,
        audioFile: File?,
        callNotesOrTranscript: String = ""
    ): Result<CustomerInteraction> = withContext(Dispatchers.IO) {
        val notesToAnalyze = if (callNotesOrTranscript.isNotBlank()) {
            callNotesOrTranscript
        } else {
            "Phone call conducted with $customerName ($customerPhone). Duration recorded in audio file: ${audioFile?.name ?: "None"}."
        }

        // Check if user has AI Gateway configured
        val apiKey = aiClient.getActiveApiKey()
        val summaryResult = if (apiKey.isNotBlank()) {
            aiClient.summarizeCall(notesToAnalyze, customerName, customerPhone).getOrNull()
        } else null

        val summaryNotesFormatted = if (summaryResult != null) {
            buildString {
                appendLine("🤖 AI Call Summary:")
                appendLine(summaryResult.summaryText)
                appendLine()
                appendLine("• Sentiment: ${summaryResult.sentiment}")
                if (!summaryResult.agreedBudget.isNullOrBlank()) {
                    appendLine("• Agreed Budget: ₹${summaryResult.agreedBudget}")
                }
                if (!summaryResult.objections.isNullOrBlank()) {
                    appendLine("• Client Concerns: ${summaryResult.objections}")
                }
                if (summaryResult.actionItems.isNotEmpty()) {
                    appendLine("• Next Steps:")
                    summaryResult.actionItems.forEach { appendLine("  - $it") }
                }
            }.trim()
        } else {
            "Call completed with $customerName. Notes: $notesToAnalyze"
        }

        val interaction = CustomerInteraction(
            customerId = customerId,
            type = InteractionType.AI_CALL_SUMMARY,
            notes = summaryNotesFormatted,
            audioFilePath = audioFile?.absolutePath,
            aiSummary = summaryResult?.summaryText,
            timestamp = System.currentTimeMillis()
        )

        val database = AppDatabase.getInstance(context)
        val id = database.customerInteractionDao().insertInteraction(interaction)
        Result.success(interaction.copy(id = id))
    }
}

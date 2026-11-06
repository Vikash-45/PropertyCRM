package com.propertycrm.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InteractionType {
    CALL,
    SITE_VISIT,
    NOTE,
    STAGE_CHANGE,
    FOLLOW_UP_CREATED,
    RESCHEDULED,
    DO_NOT_CALL_TOGGLED,
    AI_CALL_SUMMARY,
    DOCUMENT_IMPORT,
    SYSTEM;

    fun getDisplayName(): String {
        return when (this) {
            CALL -> "Call"
            SITE_VISIT -> "Site Visit"
            NOTE -> "Note"
            STAGE_CHANGE -> "Stage Changed"
            FOLLOW_UP_CREATED -> "Follow-up Created"
            RESCHEDULED -> "Rescheduled"
            DO_NOT_CALL_TOGGLED -> "Do Not Call Updated"
            AI_CALL_SUMMARY -> "AI Call Summary"
            DOCUMENT_IMPORT -> "Document Ingestion"
            SYSTEM -> "System"
        }
    }
}

enum class CallOutcome {
    ANSWERED,
    NO_ANSWER,
    BUSY,
    REQUESTED_CALLBACK,
    INTERESTED,
    NOT_INTERESTED,
    SITE_VISIT_REQUESTED,
    NEGOTIATION,
    WRONG_NUMBER;

    fun getDisplayName(): String {
        return when (this) {
            ANSWERED -> "Answered"
            NO_ANSWER -> "No Answer"
            BUSY -> "Busy"
            REQUESTED_CALLBACK -> "Requested Callback"
            INTERESTED -> "Interested"
            NOT_INTERESTED -> "Not Interested"
            SITE_VISIT_REQUESTED -> "Site Visit Requested"
            NEGOTIATION -> "Negotiation"
            WRONG_NUMBER -> "Wrong Number"
        }
    }
}

@Entity(
    tableName = "customer_interactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["timestamp"])
    ]
)
data class CustomerInteraction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "customer_id")
    val customerId: Long,

    @ColumnInfo(name = "type")
    val type: InteractionType,

    @ColumnInfo(name = "call_outcome")
    val callOutcome: CallOutcome? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String? = null,

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

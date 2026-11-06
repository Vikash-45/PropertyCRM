package com.propertycrm.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FollowUpStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    fun getDisplayName(): String {
        return when (this) {
            PENDING -> "Pending"
            COMPLETED -> "Completed"
            CANCELLED -> "Cancelled"
        }
    }
}

@Entity(
    tableName = "follow_ups",
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
        Index(value = ["follow_up_date"]),
        Index(value = ["status"])
    ]
)
data class FollowUp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "customer_id")
    val customerId: Long,

    @ColumnInfo(name = "follow_up_date")
    val followUpDate: Long, // Epoch millis (normalized to start of day or specific time)

    @ColumnInfo(name = "follow_up_time")
    val followUpTime: String? = null, // e.g. "10:30 AM"

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "status")
    val status: FollowUpStatus = FollowUpStatus.PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

package com.propertycrm.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    RESCHEDULED;

    fun getDisplayName(): String {
        return when (this) {
            SCHEDULED -> "Scheduled"
            COMPLETED -> "Completed"
            CANCELLED -> "Cancelled"
            RESCHEDULED -> "Rescheduled"
        }
    }
}

@Entity(
    tableName = "site_visits",
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
        Index(value = ["visit_date"]),
        Index(value = ["status"])
    ]
)
data class SiteVisit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "customer_id")
    val customerId: Long,

    @ColumnInfo(name = "property_location")
    val propertyLocation: String,

    @ColumnInfo(name = "visit_date")
    val visitDate: Long, // Epoch millis (start of day)

    @ColumnInfo(name = "visit_time")
    val visitTime: String? = null, // e.g. "04:30 PM"

    @ColumnInfo(name = "status")
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

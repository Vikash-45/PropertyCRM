package com.propertycrm.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PropertyType {
    PLOT,
    FLAT,
    HOUSE,
    LAND,
    COMMERCIAL,
    OTHER;

    fun getDisplayName(): String {
        return when (this) {
            PLOT -> "Plot"
            FLAT -> "Flat"
            HOUSE -> "House"
            LAND -> "Land"
            COMMERCIAL -> "Commercial"
            OTHER -> "Other"
        }
    }
}

enum class CustomerStage {
    NEW,
    CONTACTED,
    SITE_VISIT,
    NEGOTIATION,
    WON,
    LOST;

    fun getDisplayName(): String {
        return when (this) {
            NEW -> "New"
            CONTACTED -> "Contacted"
            SITE_VISIT -> "Site Visit"
            NEGOTIATION -> "Negotiation"
            WON -> "Won"
            LOST -> "Lost"
        }
    }
}

enum class RequirementType {
    BUY,
    SELL,
    RENT;

    fun getDisplayName(): String {
        return when (this) {
            BUY -> "Buy"
            SELL -> "Sell"
            RENT -> "Rent"
        }
    }
}

enum class NextAction {
    CALL,
    SITE_VISIT,
    NEGOTIATION,
    OTHER;

    fun getDisplayName(): String {
        return when (this) {
            CALL -> "Call"
            SITE_VISIT -> "Site Visit"
            NEGOTIATION -> "Negotiation"
            OTHER -> "Other"
        }
    }
}

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["stage"]),
        Index(value = ["is_archived"]),
        Index(value = ["is_do_not_call"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "inquiry_reason")
    val inquiryReason: String,

    @ColumnInfo(name = "property_type")
    val propertyType: PropertyType = PropertyType.OTHER,

    @ColumnInfo(name = "preferred_location")
    val preferredLocation: String = "",

    @ColumnInfo(name = "budget")
    val budget: String? = null,

    @ColumnInfo(name = "lead_source")
    val leadSource: String? = null,

    @ColumnInfo(name = "first_visit_date")
    val firstVisitDate: Long? = null,

    @ColumnInfo(name = "stage")
    val stage: CustomerStage = CustomerStage.NEW,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "requirement_type")
    val requirementType: RequirementType = RequirementType.BUY,

    @ColumnInfo(name = "preferred_size")
    val preferredSize: String? = null,

    @ColumnInfo(name = "preferred_contact_time")
    val preferredContactTime: String? = null,

    @ColumnInfo(name = "next_action")
    val nextAction: NextAction = NextAction.CALL,

    @ColumnInfo(name = "assigned_person")
    val assignedPerson: String? = null,

    @ColumnInfo(name = "is_do_not_call")
    val isDoNotCall: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

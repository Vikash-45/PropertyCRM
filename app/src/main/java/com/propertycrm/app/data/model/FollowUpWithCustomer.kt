package com.propertycrm.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class FollowUpWithCustomer(
    @Embedded
    val followUp: FollowUp,

    @Relation(
        parentColumn = "customer_id",
        entityColumn = "id"
    )
    val customer: Customer
)

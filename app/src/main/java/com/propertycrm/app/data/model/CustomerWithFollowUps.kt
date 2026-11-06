package com.propertycrm.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class CustomerWithFollowUps(
    @Embedded
    val customer: Customer,

    @Relation(
        parentColumn = "id",
        entityColumn = "customer_id"
    )
    val followUps: List<FollowUp> = emptyList()
)

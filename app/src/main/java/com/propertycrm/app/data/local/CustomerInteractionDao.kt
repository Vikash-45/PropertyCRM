package com.propertycrm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.propertycrm.app.data.model.CustomerInteraction
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerInteractionDao {

    @Query("SELECT * FROM customer_interactions WHERE customer_id = :customerId ORDER BY timestamp DESC, id DESC")
    fun getInteractionsForCustomer(customerId: Long): Flow<List<CustomerInteraction>>

    @Query("SELECT * FROM customer_interactions ORDER BY id ASC")
    suspend fun getAllInteractionsList(): List<CustomerInteraction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: CustomerInteraction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(interactions: List<CustomerInteraction>): List<Long>
}

package com.propertycrm.app.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.propertycrm.app.data.local.CustomerDao
import com.propertycrm.app.data.local.FollowUpDao
import com.propertycrm.app.data.model.BackupData
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.ImportResult
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

class BackupRepository(
    private val customerDao: CustomerDao,
    private val followUpDao: FollowUpDao
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Exports all customer records into CSV format suitable for Excel / Google Sheets.
     */
    suspend fun exportCustomersToCsv(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customers = customerDao.getAllCustomersList()
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                // Write CSV Header
                writer.write("ID,Name,Phone,Inquiry Reason,Property Type,Preferred Location,Budget,Lead Source,First Visit Date,Stage,Archived,Notes,Created Date,Updated Date\n")

                for (c in customers) {
                    val line = listOf(
                        c.id.toString(),
                        escapeCsv(c.name),
                        escapeCsv(c.phone),
                        escapeCsv(c.inquiryReason),
                        escapeCsv(c.propertyType.name),
                        escapeCsv(c.preferredLocation),
                        escapeCsv(c.budget ?: ""),
                        escapeCsv(c.leadSource ?: ""),
                        escapeCsv(DateUtils.formatDateIso(c.firstVisitDate)),
                        escapeCsv(c.stage.name),
                        if (c.isArchived) "Yes" else "No",
                        escapeCsv(c.notes),
                        escapeCsv(DateUtils.formatDateIso(c.createdAt)),
                        escapeCsv(DateUtils.formatDateIso(c.updatedAt))
                    ).joinToString(",")
                    writer.write(line + "\n")
                }
                writer.flush()
            }
            Result.success(customers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports complete database (customers and follow-ups) to structured JSON.
     */
    suspend fun exportCompleteBackupToJson(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customers = customerDao.getAllCustomersList()
            val followUps = followUpDao.getAllFollowUpsList()
            val backup = BackupData(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                appVersion = "1.0.0",
                customers = customers,
                followUps = followUps
            )
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                gson.toJson(backup, writer)
                writer.flush()
            }
            Result.success(customers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores customers and follow-ups from a JSON backup.
     * Validates input, prevents orphan foreign keys, and handles duplicates gracefully.
     */
    suspend fun importBackupFromJson(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        try {
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            val backupData = gson.fromJson(reader, BackupData::class.java)

            if (backupData == null || backupData.customers.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    errorMessage = "The selected file is empty or does not contain valid customer records."
                )
            }

            var customersImported = 0
            var customersSkipped = 0
            var followUpsImported = 0

            // Old customer ID -> New customer ID map
            val customerIdMap = mutableMapOf<Long, Long>()

            for (incomingCustomer in backupData.customers) {
                if (incomingCustomer.name.isBlank() || incomingCustomer.phone.isBlank()) {
                    customersSkipped++
                    continue
                }

                // Check duplicate by phone number
                val existing = customerDao.findCustomersByPhone(incomingCustomer.phone.trim())
                val targetCustomerId: Long

                if (existing.isNotEmpty()) {
                    // Reuse existing customer or update
                    targetCustomerId = existing.first().id
                    customerIdMap[incomingCustomer.id] = targetCustomerId
                    customersSkipped++
                } else {
                    // Insert customer with new autogenerated ID
                    val newCustomer = incomingCustomer.copy(id = 0L)
                    targetCustomerId = customerDao.insertCustomer(newCustomer)
                    customerIdMap[incomingCustomer.id] = targetCustomerId
                    customersImported++
                }
            }

            // Restore follow-ups mapped to the resolved customer IDs
            for (fu in backupData.followUps) {
                val resolvedCustomerId = customerIdMap[fu.customerId]
                if (resolvedCustomerId != null && resolvedCustomerId > 0L) {
                    val newFollowUp = fu.copy(
                        id = 0L,
                        customerId = resolvedCustomerId
                    )
                    followUpDao.insertFollowUp(newFollowUp)
                    followUpsImported++
                }
            }

            ImportResult(
                success = true,
                customersImported = customersImported,
                followUpsImported = followUpsImported,
                customersSkipped = customersSkipped
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                errorMessage = "Failed to parse backup file: ${e.localizedMessage}"
            )
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\r", " ").replace("\n", " ")
        if (str.contains(",") || str.contains("\"") || str.contains(";")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}

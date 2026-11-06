package com.propertycrm.app.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.propertycrm.app.data.local.AppDatabase
import com.propertycrm.app.data.model.BackupData
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.ImportResult
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import com.propertycrm.app.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

class BackupRepository(
    private val database: AppDatabase
) {

    private val customerDao = database.customerDao()
    private val followUpDao = database.followUpDao()
    private val siteVisitDao = database.siteVisitDao()
    private val customerInteractionDao = database.customerInteractionDao()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Plain CSV export for spreadsheet viewing.
     */
    suspend fun exportCustomersToCsv(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customers = customerDao.getAllCustomersList()
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write("ID,Name,Phone,Requirement,Inquiry Reason,Property Type,Preferred Location,Budget,Lead Source,Next Action,DNC,Stage,Archived,Notes,Created Date,Updated Date\n")

                for (c in customers) {
                    val line = listOf(
                        c.id.toString(),
                        escapeCsv(c.name),
                        escapeCsv(c.phone),
                        escapeCsv(c.requirementType.name),
                        escapeCsv(c.inquiryReason),
                        escapeCsv(c.propertyType.name),
                        escapeCsv(c.preferredLocation),
                        escapeCsv(c.budget ?: ""),
                        escapeCsv(c.leadSource ?: ""),
                        escapeCsv(c.nextAction.name),
                        if (c.isDoNotCall) "Yes" else "No",
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
     * Password-Protected Authenticated AES-256-GCM Encrypted JSON Backup (Item 13)
     */
    suspend fun exportEncryptedBackup(
        password: String,
        outputStream: OutputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (password.length < 4) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
            }

            val customers = customerDao.getAllCustomersList()
            val followUps = followUpDao.getAllFollowUpsList()
            val siteVisits = siteVisitDao.getAllSiteVisitsList()
            val interactions = customerInteractionDao.getAllInteractionsList()

            val backup = BackupData(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                appVersion = "1.1.0",
                customers = customers,
                followUps = followUps,
                siteVisits = siteVisits,
                interactions = interactions
            )

            val jsonString = gson.toJson(backup)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            SecurityUtils.encryptBackup(jsonBytes, password, outputStream)
            Result.success(customers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Previews encrypted backup data without altering the database.
     */
    suspend fun previewEncryptedBackup(
        inputStream: InputStream,
        password: String
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val decryptedBytes = SecurityUtils.decryptBackup(inputStream, password)
            val reader = InputStreamReader(ByteArrayInputStream(decryptedBytes), Charsets.UTF_8)
            val backupData = gson.fromJson(reader, BackupData::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Backup payload is empty or invalid."))

            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transactional Restoration with Rollback on Failure (Item 13)
     */
    suspend fun restoreEncryptedBackup(
        inputStream: InputStream,
        password: String
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val decryptedBytes = SecurityUtils.decryptBackup(inputStream, password)
            val reader = InputStreamReader(ByteArrayInputStream(decryptedBytes), Charsets.UTF_8)
            val backupData = gson.fromJson(reader, BackupData::class.java)

            if (backupData == null || backupData.customers.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    errorMessage = "The backup file is empty or does not contain valid customer records."
                )
            }

            var customersImported = 0
            var customersSkipped = 0
            var followUpsImported = 0
            var siteVisitsImported = 0
            var interactionsImported = 0

            // Atomic Transaction
            database.withTransaction {
                val customerIdMap = mutableMapOf<Long, Long>()

                for (incoming in backupData.customers) {
                    if (incoming.name.isBlank() || incoming.phone.isBlank()) {
                        customersSkipped++
                        continue
                    }

                    val existing = customerDao.findCustomersByPhone(incoming.phone.trim())
                    val targetId: Long

                    if (existing.isNotEmpty()) {
                        targetId = existing.first().id
                        customerIdMap[incoming.id] = targetId
                        customersSkipped++
                    } else {
                        val newCustomer = incoming.copy(id = 0L)
                        targetId = customerDao.insertCustomer(newCustomer)
                        customerIdMap[incoming.id] = targetId
                        customersImported++
                    }
                }

                // Restore follow-ups mapped to resolved customer IDs
                for (fu in backupData.followUps) {
                    val resolvedId = customerIdMap[fu.customerId]
                    if (resolvedId != null && resolvedId > 0L) {
                        followUpDao.insertFollowUp(fu.copy(id = 0L, customerId = resolvedId))
                        followUpsImported++
                    }
                }

                // Restore site visits mapped to resolved customer IDs
                for (sv in backupData.siteVisits) {
                    val resolvedId = customerIdMap[sv.customerId]
                    if (resolvedId != null && resolvedId > 0L) {
                        siteVisitDao.insertSiteVisit(sv.copy(id = 0L, customerId = resolvedId))
                        siteVisitsImported++
                    }
                }

                // Restore interaction timeline
                for (it in backupData.interactions) {
                    val resolvedId = customerIdMap[it.customerId]
                    if (resolvedId != null && resolvedId > 0L) {
                        customerInteractionDao.insertInteraction(it.copy(id = 0L, customerId = resolvedId))
                        interactionsImported++
                    }
                }
            }

            ImportResult(
                success = true,
                customersImported = customersImported,
                followUpsImported = followUpsImported,
                siteVisitsImported = siteVisitsImported,
                interactionsImported = interactionsImported,
                customersSkipped = customersSkipped
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                errorMessage = e.message ?: "Restoration failed."
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

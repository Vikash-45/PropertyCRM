package com.propertycrm.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader

data class DocumentImportResult(
    val candidates: List<ExtractedCustomerCandidate>,
    val sourceName: String,
    val extractedUsingAi: Boolean,
    val infoMessage: String
)

class SmartDocumentImporter(
    private val context: Context,
    private val aiClient: AIGatewayClient,
    private val customerRepository: CustomerRepository
) {

    /**
     * Ingests any document (PDF, Text, JSON, CSV, or Image) from Uri, extracts text/visuals,
     * structures customer records using AI (or heuristic fallback), and returns extracted candidates.
     */
    suspend fun parseDocument(uri: Uri, fileNameHint: String? = null): Result<DocumentImportResult> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
        val fileName = fileNameHint ?: uri.lastPathSegment ?: "document"

        try {
            when {
                // Case 1: Image (PNG, JPG, WEBP)
                mimeType.startsWith("image/") || isImageExtension(fileName) -> {
                    parseImageDocument(uri, fileName)
                }

                // Case 2: PDF Document
                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    parsePdfDocument(uri, fileName)
                }

                // Case 3: JSON
                mimeType == "application/json" || fileName.endsWith(".json", ignoreCase = true) -> {
                    val text = readTextFromUri(uri)
                    parseTextDocument(text, fileName, forceJson = true)
                }

                // Case 4: CSV / TSV / Plain Text
                else -> {
                    val text = readTextFromUri(uri)
                    parseTextDocument(text, fileName, forceJson = false)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to ingest document '$fileName': ${e.message}"))
        }
    }

    private suspend fun parseTextDocument(
        text: String,
        fileName: String,
        forceJson: Boolean
    ): Result<DocumentImportResult> {
        if (text.isBlank()) {
            return Result.failure(Exception("Document is empty."))
        }

        // Try AI extraction first if key is present
        val apiKey = aiClient.getActiveApiKey()
        if (apiKey.isNotBlank()) {
            val aiResult = aiClient.extractCustomersFromText(text)
            if (aiResult.isSuccess && aiResult.getOrNull()?.isNotEmpty() == true) {
                return Result.success(
                    DocumentImportResult(
                        candidates = aiResult.getOrNull()!!,
                        sourceName = fileName,
                        extractedUsingAi = true,
                        infoMessage = "Extracted ${aiResult.getOrNull()!!.size} lead(s) using AI Model (${aiClient.getActiveModel()})."
                    )
                )
            }
        }

        // Offline Fallback Heuristics
        val offlineCandidates = if (forceJson) {
            parseJsonDirectly(text)
        } else {
            parseCsvOrUnstructuredText(text)
        }

        if (offlineCandidates.isNotEmpty()) {
            return Result.success(
                DocumentImportResult(
                    candidates = offlineCandidates,
                    sourceName = fileName,
                    extractedUsingAi = false,
                    infoMessage = "Extracted ${offlineCandidates.size} lead(s) via offline document parser."
                )
            )
        }

        return Result.failure(Exception("No valid customer profiles or phone numbers identified in $fileName."))
    }

    private suspend fun parsePdfDocument(uri: Uri, fileName: String): Result<DocumentImportResult> = withContext(Dispatchers.IO) {
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            return@withContext Result.failure(Exception("Could not open PDF file descriptor."))
        }

        pfd.use { descriptor ->
            val pdfRenderer = PdfRenderer(descriptor)
            val pageCount = pdfRenderer.pageCount
            val pagesToScan = minOf(pageCount, 3) // Scan up to first 3 pages

            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until pagesToScan) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
            pdfRenderer.close()

            // If AI is available, send first page as multimodal vision base64
            val apiKey = aiClient.getActiveApiKey()
            if (apiKey.isNotBlank() && bitmaps.isNotEmpty()) {
                val visionResult = extractLeadsFromBitmapUsingVision(bitmaps[0])
                if (visionResult.isSuccess && visionResult.getOrNull()?.isNotEmpty() == true) {
                    return@withContext Result.success(
                        DocumentImportResult(
                            candidates = visionResult.getOrNull()!!,
                            sourceName = fileName,
                            extractedUsingAi = true,
                            infoMessage = "Extracted ${visionResult.getOrNull()!!.size} lead(s) from PDF pages via AI Vision."
                        )
                    )
                }
            }

            // Fallback: Check if there's plain text in the PDF stream
            val rawPdfBytes = readBytesFromUri(uri)
            val extractedStrings = extractAsciiStringsFromBytes(rawPdfBytes)
            val candidates = parseCsvOrUnstructuredText(extractedStrings)
            if (candidates.isNotEmpty()) {
                return@withContext Result.success(
                    DocumentImportResult(
                        candidates = candidates,
                        sourceName = fileName,
                        extractedUsingAi = false,
                        infoMessage = "Extracted ${candidates.size} lead(s) from PDF text stream."
                    )
                )
            }

            Result.failure(Exception("Could not extract text from PDF. Set an AI API Key in Settings to enable AI Vision OCR for scanned PDFs."))
        }
    }

    private suspend fun parseImageDocument(uri: Uri, fileName: String): Result<DocumentImportResult> = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(Exception("Could not open image stream."))
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (bitmap == null) {
            return@withContext Result.failure(Exception("Failed to decode image."))
        }

        val apiKey = aiClient.getActiveApiKey()
        if (apiKey.isNotBlank()) {
            val visionResult = extractLeadsFromBitmapUsingVision(bitmap)
            if (visionResult.isSuccess && visionResult.getOrNull()?.isNotEmpty() == true) {
                return@withContext Result.success(
                    DocumentImportResult(
                        candidates = visionResult.getOrNull()!!,
                        sourceName = fileName,
                        extractedUsingAi = true,
                        infoMessage = "Extracted ${visionResult.getOrNull()!!.size} lead(s) from image via AI Vision."
                    )
                )
            }
        }

        Result.failure(Exception("AI API Key required in Settings to read customer info from images / business cards."))
    }

    private suspend fun extractLeadsFromBitmapUsingVision(bitmap: Bitmap): Result<List<ExtractedCustomerCandidate>> {
        val scaled = if (bitmap.width > 1200 || bitmap.height > 1200) {
            val ratio = minOf(1200f / bitmap.width, 1200f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64Image"

        val prompt = """
Examine this image (which may be a property brochure, business card, lead sheet, or client message).
Extract all real estate client/lead records into a JSON array:
[
  {
    "name": "Customer Name",
    "phone": "+91 9876543210",
    "requirementType": "BUY",
    "propertyType": "PLOT",
    "preferredLocation": "Sector 150",
    "budget": "1.2 Cr",
    "inquiryReason": "Inquiry details",
    "stage": "NEW"
  }
]
Output ONLY the JSON array, no extra commentary.
        """.trimIndent()

        // Construct multimodal content message
        val contentArray = JsonArray()
        val textObj = JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", prompt)
        }
        val imgObj = JsonObject().apply {
            addProperty("type", "image_url")
            val urlObj = JsonObject().apply { addProperty("url", dataUri) }
            add("image_url", urlObj)
        }
        contentArray.add(textObj)
        contentArray.add(imgObj)

        val messages = listOf(
            ChatMessage("system", "You are an AI document vision parser for real estate CRM."),
            ChatMessage("user", contentArray.toString())
        )

        return aiClient.extractCustomersFromText("Image processed via Vision: " + contentArray.toString())
    }

    /**
     * Heuristic parser for CSV, TSV, or unstructured contact notes.
     */
    fun parseCsvOrUnstructuredText(text: String): List<ExtractedCustomerCandidate> {
        val candidates = mutableListOf<ExtractedCustomerCandidate>()
        val lines = text.lines()

        val phoneRegex = "(\\+?\\d{1,4}[-\\s]?)?([6-9]\\d{9})".toRegex()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 5) continue

            val match = phoneRegex.find(trimmed)
            if (match != null) {
                val phone = match.value.replace("[\\s-]".toRegex(), "")
                val remaining = trimmed.replace(match.value, "").trim(',', ' ', ';', '-')

                val parts = remaining.split(',', '\t', '|').map { it.trim() }.filter { it.isNotBlank() }
                val name = parts.firstOrNull()?.takeIf { it.length > 1 } ?: "Client ${phone.takeLast(4)}"
                val location = parts.getOrNull(1) ?: ""
                val budget = parts.firstOrNull { it.contains("cr", ignoreCase = true) || it.contains("lakh", ignoreCase = true) || it.contains("₹") }
                val inquiry = if (parts.size > 2) parts.drop(2).joinToString(", ") else "Imported lead"

                val reqType = when {
                    remaining.contains("sell", ignoreCase = true) -> RequirementType.SELL
                    remaining.contains("rent", ignoreCase = true) -> RequirementType.RENT
                    else -> RequirementType.BUY
                }

                val propType = when {
                    remaining.contains("plot", ignoreCase = true) -> PropertyType.PLOT
                    remaining.contains("flat", ignoreCase = true) -> PropertyType.FLAT
                    remaining.contains("house", ignoreCase = true) || remaining.contains("villa", ignoreCase = true) -> PropertyType.HOUSE
                    remaining.contains("commercial", ignoreCase = true) || remaining.contains("shop", ignoreCase = true) || remaining.contains("office", ignoreCase = true) -> PropertyType.COMMERCIAL
                    remaining.contains("land", ignoreCase = true) -> PropertyType.LAND
                    else -> PropertyType.OTHER
                }

                candidates.add(
                    ExtractedCustomerCandidate(
                        name = name,
                        phone = phone,
                        requirementType = reqType,
                        propertyType = propType,
                        preferredLocation = location,
                        budget = budget,
                        inquiryReason = inquiry,
                        stage = CustomerStage.NEW
                    )
                )
            }
        }
        return candidates
    }

    private fun parseJsonDirectly(json: String): List<ExtractedCustomerCandidate> {
        val list = mutableListOf<ExtractedCustomerCandidate>()
        try {
            val elem = com.google.gson.JsonParser.parseString(json)
            val array = if (elem.isJsonArray) elem.asJsonArray else {
                val obj = elem.asJsonObject
                if (obj.has("customers")) obj.getAsJsonArray("customers")
                else if (obj.has("leads")) obj.getAsJsonArray("leads")
                else JsonArray()
            }

            array.forEach { item ->
                val o = item.asJsonObject
                val name = o.get("name")?.asString ?: "Unknown"
                val phone = o.get("phone")?.asString ?: ""
                if (phone.isNotBlank()) {
                    list.add(
                        ExtractedCustomerCandidate(
                            name = name,
                            phone = phone,
                            preferredLocation = o.get("preferredLocation")?.asString ?: o.get("location")?.asString ?: "",
                            budget = o.get("budget")?.asString,
                            inquiryReason = o.get("inquiryReason")?.asString ?: "Imported via JSON",
                            stage = CustomerStage.NEW
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Commits selected candidates directly into Room database.
     */
    suspend fun commitCandidatesToDatabase(
        candidates: List<ExtractedCustomerCandidate>,
        sourceFileName: String
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (c in candidates) {
            val customer = Customer(
                name = c.name,
                phone = c.phone,
                requirementType = c.requirementType,
                propertyType = c.propertyType,
                preferredLocation = c.preferredLocation,
                budget = c.budget,
                inquiryReason = c.inquiryReason,
                stage = c.stage,
                notes = "Auto-imported from document: $sourceFileName"
            )
            customerRepository.saveCustomerWithInitialFollowUp(
                customer = customer,
                initialFollowUpDate = System.currentTimeMillis() + 86400000L,
                initialFollowUpTime = "11:00 AM",
                initialFollowUpNotes = "Initial follow-up from document import"
            )
            count++
        }
        count
    }

    private fun readTextFromUri(uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    sb.appendLine(line)
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }

    private fun readBytesFromUri(uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            return stream.readBytes()
        }
        return ByteArray(0)
    }

    private fun extractAsciiStringsFromBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() || c in " \t\n\r,.:;+-/()[]₹@") {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun isImageExtension(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }
}

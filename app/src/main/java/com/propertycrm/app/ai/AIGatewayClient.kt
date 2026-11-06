package com.propertycrm.app.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "system", "user", "assistant", "tool"
    val content: String,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCall>? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class AIChatResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ExtractedCustomerCandidate(
    val name: String,
    val phone: String,
    val requirementType: RequirementType = RequirementType.BUY,
    val propertyType: PropertyType = PropertyType.FLAT,
    val preferredLocation: String = "",
    val budget: String? = null,
    val inquiryReason: String = "",
    val stage: CustomerStage = CustomerStage.NEW,
    val notes: String = ""
)

data class CallSummaryResult(
    val summaryText: String,
    val sentiment: String,
    val agreedBudget: String?,
    val objections: String?,
    val actionItems: List<String>,
    val suggestedFollowUpDays: Int = 1
)

class AIGatewayClient(
    private val preferenceManager: PreferenceManager
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun getActiveGatewayUrl(): String = preferenceManager.aiGatewayUrl.first()
    suspend fun getActiveApiKey(): String = preferenceManager.aiApiKey.first()
    suspend fun getActiveModel(): String = preferenceManager.aiModel.first()
    suspend fun getActiveSystemPrompt(): String = preferenceManager.aiSystemPrompt.first()
    suspend fun getActiveTemperature(): Float = preferenceManager.aiTemperature.first()

    /**
     * Pings the OpenAI-compatible gateway with a fast completion or models test.
     */
    suspend fun testConnection(
        customUrl: String? = null,
        customKey: String? = null,
        customModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = (customUrl ?: getActiveGatewayUrl()).trimEnd('/')
        val apiKey = customKey ?: getActiveApiKey()
        val model = customModel ?: getActiveModel()

        val endpoint = "$baseUrl/chat/completions"

        val bodyJson = JsonObject().apply {
            addProperty("model", model)
            val messagesArray = JsonArray()
            val msg = JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", "Respond only with: 'AI Connection Verified Successfully'")
            }
            messagesArray.add(msg)
            add("messages", messagesArray)
            addProperty("max_tokens", 30)
            addProperty("temperature", 0.1)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val startTime = System.currentTimeMillis()
        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val latencyMs = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Gateway returned HTTP ${response.code}: $responseBody")
                )
            }

            val parsed = JsonParser.parseString(responseBody).asJsonObject
            val choices = parsed.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val messageObj = choices[0].asJsonObject.getAsJsonObject("message")
                val text = messageObj?.get("content")?.asString ?: "Connected"
                Result.success("Success (${latencyMs}ms): ${text.trim()}")
            } else {
                Result.success("Connected (${latencyMs}ms) to $model")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    /**
     * Executes a chat completion call with optional tool calling definitions.
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        customModel: String? = null,
        customTemperature: Float? = null,
        toolsJson: JsonArray? = null
    ): Result<AIChatResponse> = withContext(Dispatchers.IO) {
        val baseUrl = getActiveGatewayUrl().trimEnd('/')
        val apiKey = getActiveApiKey()
        val model = customModel ?: getActiveModel()
        val temperature = customTemperature ?: getActiveTemperature()

        val endpoint = "$baseUrl/chat/completions"

        val bodyJson = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", temperature)

            val messagesArray = JsonArray()
            messages.forEach { msg ->
                val m = JsonObject().apply {
                    addProperty("role", msg.role)
                    addProperty("content", msg.content)
                    if (msg.toolCallId != null) {
                        addProperty("tool_call_id", msg.toolCallId)
                    }
                    if (!msg.toolCalls.isNullOrEmpty()) {
                        val tcArray = JsonArray()
                        msg.toolCalls.forEach { tc ->
                            val tcObj = JsonObject().apply {
                                addProperty("id", tc.id)
                                addProperty("type", "function")
                                val fnObj = JsonObject().apply {
                                    addProperty("name", tc.name)
                                    addProperty("arguments", tc.argumentsJson)
                                }
                                add("function", fnObj)
                            }
                            tcArray.add(tcObj)
                        }
                        add("tool_calls", tcArray)
                    }
                }
                messagesArray.add(m)
            }
            add("messages", messagesArray)

            if (toolsJson != null && toolsJson.size() > 0) {
                add("tools", toolsJson)
                addProperty("tool_choice", "auto")
            }
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("AI Gateway Error HTTP ${response.code}: $responseBody")
                )
            }

            val parsed = JsonParser.parseString(responseBody).asJsonObject
            val choices = parsed.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                return@withContext Result.failure(Exception("Empty choices in AI Gateway response"))
            }

            val messageObj = choices[0].asJsonObject.getAsJsonObject("message")
            val content = messageObj.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""

            val extractedToolCalls = mutableListOf<ToolCall>()
            if (messageObj.has("tool_calls") && !messageObj.get("tool_calls").isJsonNull) {
                val tcArray = messageObj.getAsJsonArray("tool_calls")
                tcArray.forEach { elem ->
                    val tc = elem.asJsonObject
                    val id = tc.get("id").asString
                    val fn = tc.getAsJsonObject("function")
                    val name = fn.get("name").asString
                    val args = fn.get("arguments").asString
                    extractedToolCalls.add(ToolCall(id, name, args))
                }
            }

            Result.success(AIChatResponse(content, extractedToolCalls))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts structured customer lead entries from raw text (from PDF, image OCR, CSV, or text).
     */
    suspend fun extractCustomersFromText(rawText: String): Result<List<ExtractedCustomerCandidate>> = withContext(Dispatchers.IO) {
        val prompt = """
You are a real estate CRM data extraction specialist.
Analyze the following document/text and extract all customer/lead profiles into a clean JSON array.
For each lead, extract:
- "name": full customer name
- "phone": phone number (clean digits, format with country code if available)
- "requirementType": "BUY", "SELL", or "RENT"
- "propertyType": "PLOT", "FLAT", "HOUSE", "LAND", "COMMERCIAL", or "OTHER"
- "preferredLocation": city, locality, or sector
- "budget": budget or price mentioned (e.g. "75 Lakh", "1.5 Cr", "40k/mo")
- "inquiryReason": summary of customer's requirement/interest
- "stage": "NEW", "CONTACTED", "SITE_VISIT", "NEGOTIATION", "WON", or "LOST"

Return ONLY valid JSON array with no markdown ticks, no preamble, and no explanation. Example:
[
  {
    "name": "Rajesh Kumar",
    "phone": "+91 9876543210",
    "requirementType": "BUY",
    "propertyType": "PLOT",
    "preferredLocation": "Sector 150 Noida",
    "budget": "1.2 Cr",
    "inquiryReason": "Looking for 250 sq yd plot",
    "stage": "NEW"
  }
]

Document content:
$rawText
        """.trimIndent()

        val messages = listOf(
            ChatMessage("system", "You are an automated JSON extraction parser for real estate CRM data."),
            ChatMessage("user", prompt)
        )

        val res = chatCompletion(messages, customTemperature = 0.1f)
        if (res.isFailure) {
            return@withContext Result.failure(res.exceptionOrNull()!!)
        }

        var jsonStr = res.getOrNull()?.content?.trim() ?: "[]"
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substringAfter("```").substringBeforeLast("```").trim()
        }

        try {
            val jsonArray = JsonParser.parseString(jsonStr).asJsonArray
            val candidates = mutableListOf<ExtractedCustomerCandidate>()

            jsonArray.forEach { elem ->
                val obj = elem.asJsonObject
                val name = obj.get("name")?.asString ?: "Unknown"
                val phone = obj.get("phone")?.asString ?: ""

                val reqTypeStr = obj.get("requirementType")?.asString?.uppercase() ?: "BUY"
                val reqType = try { RequirementType.valueOf(reqTypeStr) } catch (_: Exception) { RequirementType.BUY }

                val propTypeStr = obj.get("propertyType")?.asString?.uppercase() ?: "OTHER"
                val propType = try { PropertyType.valueOf(propTypeStr) } catch (_: Exception) { PropertyType.OTHER }

                val location = obj.get("preferredLocation")?.asString ?: ""
                val budget = obj.get("budget")?.asString
                val inquiry = obj.get("inquiryReason")?.asString ?: ""

                val stageStr = obj.get("stage")?.asString?.uppercase() ?: "NEW"
                val stage = try { CustomerStage.valueOf(stageStr) } catch (_: Exception) { CustomerStage.NEW }

                candidates.add(
                    ExtractedCustomerCandidate(
                        name = name,
                        phone = phone,
                        requirementType = reqType,
                        propertyType = propType,
                        preferredLocation = location,
                        budget = budget,
                        inquiryReason = inquiry,
                        stage = stage
                    )
                )
            }
            Result.success(candidates)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse extracted JSON: ${e.message}\nRaw: $jsonStr"))
        }
    }

    /**
     * Generates an actionable call summary from notes or audio transcript.
     */
    suspend fun summarizeCall(
        transcriptOrNotes: String,
        customerName: String,
        customerPhone: String
    ): Result<CallSummaryResult> = withContext(Dispatchers.IO) {
        val prompt = """
You are an expert real estate sales coach and CRM analyst.
Analyze the following phone conversation transcript / call notes with customer "$customerName" ($customerPhone).

Produce a structured JSON summary containing:
- "summaryText": 2-3 sentences concise summary of what was discussed.
- "sentiment": "POSITIVE", "NEUTRAL", or "HESITANT"
- "agreedBudget": budget discussed or null
- "objections": concerns or hurdles raised (e.g. registry delay, loan approval) or null
- "actionItems": list of next steps agreed during call
- "suggestedFollowUpDays": recommended days until next follow-up (1 to 7)

Return ONLY valid JSON with no markdown wrapping. Example:
{
  "summaryText": "Client inquired about 3BHK options in Sector 62. Showed high interest in ready-to-move projects.",
  "sentiment": "POSITIVE",
  "agreedBudget": "1.1 Cr",
  "objections": "Needs bank loan assistance with HDFC",
  "actionItems": ["Send floor plan brochures", "Schedule site visit on Saturday"],
  "suggestedFollowUpDays": 2
}

Transcript / Notes:
$transcriptOrNotes
        """.trimIndent()

        val messages = listOf(
            ChatMessage("system", "You are an analytical real estate sales assistant producing structured call summaries."),
            ChatMessage("user", prompt)
        )

        val res = chatCompletion(messages, customTemperature = 0.2f)
        if (res.isFailure) {
            return@withContext Result.failure(res.exceptionOrNull()!!)
        }

        var jsonStr = res.getOrNull()?.content?.trim() ?: "{}"
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substringAfter("```").substringBeforeLast("```").trim()
        }

        try {
            val obj = JsonParser.parseString(jsonStr).asJsonObject
            val summaryText = obj.get("summaryText")?.asString ?: "Call conducted with client."
            val sentiment = obj.get("sentiment")?.asString ?: "NEUTRAL"
            val agreedBudget = obj.get("agreedBudget")?.takeIf { !it.isJsonNull }?.asString
            val objections = obj.get("objections")?.takeIf { !it.isJsonNull }?.asString

            val actionItems = mutableListOf<String>()
            if (obj.has("actionItems") && obj.get("actionItems").isJsonArray) {
                obj.getAsJsonArray("actionItems").forEach { item ->
                    actionItems.add(item.asString)
                }
            }

            val days = obj.get("suggestedFollowUpDays")?.asInt ?: 1

            Result.success(
                CallSummaryResult(
                    summaryText = summaryText,
                    sentiment = sentiment,
                    agreedBudget = agreedBudget,
                    objections = objections,
                    actionItems = actionItems,
                    suggestedFollowUpDays = days
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse call summary JSON: ${e.message}\nRaw: $jsonStr"))
        }
    }
}

package com.propertycrm.app.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.propertycrm.app.data.local.AppDatabase
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import com.propertycrm.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AgentExecutionResult(
    val replyText: String,
    val executedActions: List<String> = emptyList(),
    val error: String? = null
)

class AICopilotEngine(
    private val aiClient: AIGatewayClient,
    private val customerRepository: CustomerRepository,
    private val followUpRepository: FollowUpRepository,
    private val appDatabase: AppDatabase
) {
    private val gson = Gson()

    /**
     * Constructs the full OpenAI function calling schema for all CRM tools.
     */
    fun getToolsJson(): JsonArray {
        val tools = JsonArray()

        // Tool 1: add_customer
        tools.add(createToolObject(
            name = "add_customer",
            description = "Registers a new real estate customer/lead into the CRM database.",
            properties = JsonObject().apply {
                add("name", createProperty("string", "Full name of the customer"))
                add("phone", createProperty("string", "Phone number with optional country code"))
                add("requirement_type", createProperty("string", "Deal type: BUY, SELL, or RENT", listOf("BUY", "SELL", "RENT")))
                add("property_type", createProperty("string", "Property category: PLOT, FLAT, HOUSE, LAND, COMMERCIAL, OTHER", listOf("PLOT", "FLAT", "HOUSE", "LAND", "COMMERCIAL", "OTHER")))
                add("preferred_location", createProperty("string", "Sector, locality, or city"))
                add("budget", createProperty("string", "Budget or price range (e.g. '80 Lakh', '1.5 Cr')"))
                add("inquiry_reason", createProperty("string", "Specific property requirement or notes"))
                add("stage", createProperty("string", "Initial stage: NEW, CONTACTED, SITE_VISIT, NEGOTIATION, WON, LOST"))
            },
            required = listOf("name", "phone")
        ))

        // Tool 2: search_customers
        tools.add(createToolObject(
            name = "search_customers",
            description = "Search customers by name, phone number, location, or inquiry keywords.",
            properties = JsonObject().apply {
                add("query", createProperty("string", "Search keyword (name, phone, locality)"))
                add("property_type", createProperty("string", "Optional property filter: PLOT, FLAT, etc."))
                add("requirement_type", createProperty("string", "Optional deal filter: BUY, SELL, RENT"))
            },
            required = emptyList()
        ))

        // Tool 3: schedule_site_visit
        tools.add(createToolObject(
            name = "schedule_site_visit",
            description = "Schedules an on-site property tour/visit for a specific customer.",
            properties = JsonObject().apply {
                add("customer_id", createProperty("integer", "ID of the customer"))
                add("customer_name_hint", createProperty("string", "Customer name if ID is not known"))
                add("property_location", createProperty("string", "Location or project name for the site visit"))
                add("visit_date", createProperty("string", "Visit date: 'today', 'tomorrow', or 'YYYY-MM-DD'"))
                add("visit_time", createProperty("string", "Visit time (e.g. '11:00 AM', '04:30 PM')"))
                add("notes", createProperty("string", "Instructions or property details"))
            },
            required = listOf("property_location")
        ))

        // Tool 4: record_call_outcome
        tools.add(createToolObject(
            name = "record_call_outcome",
            description = "Logs the outcome and notes of a phone call with a customer.",
            properties = JsonObject().apply {
                add("customer_id", createProperty("integer", "Customer ID"))
                add("customer_name_hint", createProperty("string", "Customer name if ID is not known"))
                add("outcome", createProperty("string", "Outcome: ANSWERED, INTERESTED, NOT_INTERESTED, SITE_VISIT_REQUESTED, NEGOTIATION, NO_ANSWER, BUSY, REQUESTED_CALLBACK, WRONG_NUMBER"))
                add("notes", createProperty("string", "Call summary notes"))
                add("schedule_next_follow_up", createProperty("boolean", "Whether to schedule a subsequent follow-up task"))
                add("next_follow_up_days", createProperty("integer", "Days until next follow-up (e.g. 1, 2, 3)"))
            },
            required = listOf("outcome")
        ))

        // Tool 5: update_customer_stage
        tools.add(createToolObject(
            name = "update_customer_stage",
            description = "Updates the sales pipeline stage for a customer.",
            properties = JsonObject().apply {
                add("customer_id", createProperty("integer", "Customer ID"))
                add("customer_name_hint", createProperty("string", "Customer name if ID is not known"))
                add("stage", createProperty("string", "New stage: NEW, CONTACTED, SITE_VISIT, NEGOTIATION, WON, LOST"))
            },
            required = listOf("stage")
        ))

        // Tool 6: get_crm_summary
        tools.add(createToolObject(
            name = "get_crm_summary",
            description = "Retrieves live counts of active clients, overdue calls, and pending tasks in the CRM.",
            properties = JsonObject(),
            required = emptyList()
        ))

        return tools
    }

    /**
     * Executes an autonomous turn: sends prompt to AI, inspects tool calls, executes them
     * against Room DB, feeds results back to AI, and returns final answer to user.
     */
    suspend fun processUserCommand(
        userInstruction: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        val systemPrompt = aiClient.getActiveSystemPrompt()
        val toolsJson = getToolsJson()

        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage("system", systemPrompt))

        // Include last 6 turns of conversation history if any
        if (conversationHistory.isNotEmpty()) {
            messages.addAll(conversationHistory.takeLast(6))
        }
        messages.add(ChatMessage("user", userInstruction))

        val initialCall = aiClient.chatCompletion(messages, toolsJson = toolsJson)
        if (initialCall.isFailure) {
            return@withContext AgentExecutionResult(
                replyText = "Error connecting to AI Gateway: ${initialCall.exceptionOrNull()?.message}",
                error = initialCall.exceptionOrNull()?.message
            )
        }

        val response = initialCall.getOrNull()!!
        if (response.toolCalls.isEmpty()) {
            // Direct conversational reply
            return@withContext AgentExecutionResult(
                replyText = response.content.ifBlank { "Command processed successfully." }
            )
        }

        // Execute tool calls
        val executedActionDescriptions = mutableListOf<String>()
        val toolResponseMessages = mutableListOf<ChatMessage>()

        // Add assistant's tool-call message
        messages.add(ChatMessage("assistant", response.content, toolCalls = response.toolCalls))

        for (toolCall in response.toolCalls) {
            val (resultStr, actionDesc) = executeTool(toolCall.name, toolCall.argumentsJson)
            executedActionDescriptions.add(actionDesc)
            toolResponseMessages.add(
                ChatMessage(
                    role = "tool",
                    content = resultStr,
                    toolCallId = toolCall.id
                )
            )
        }

        messages.addAll(toolResponseMessages)

        // Second turn: ask model to synthesize the final reply acknowledging the executed actions
        val followUpCall = aiClient.chatCompletion(messages)
        val finalContent = if (followUpCall.isSuccess) {
            followUpCall.getOrNull()?.content?.trim() ?: "Actions executed successfully."
        } else {
            "Actions executed: ${executedActionDescriptions.joinToString("; ")}"
        }

        AgentExecutionResult(
            replyText = finalContent,
            executedActions = executedActionDescriptions
        )
    }

    private suspend fun executeTool(name: String, argsJson: String): Pair<String, String> {
        val args = try {
            JsonParser.parseString(argsJson).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }

        return when (name) {
            "add_customer" -> {
                val customerName = args.get("name")?.asString ?: "Unknown"
                val phone = args.get("phone")?.asString ?: ""
                val reqStr = args.get("requirement_type")?.asString?.uppercase() ?: "BUY"
                val propStr = args.get("property_type")?.asString?.uppercase() ?: "FLAT"
                val location = args.get("preferred_location")?.asString ?: ""
                val budget = args.get("budget")?.asString
                val inquiry = args.get("inquiry_reason")?.asString ?: "Added via AI Copilot"
                val stageStr = args.get("stage")?.asString?.uppercase() ?: "NEW"

                val reqType = try { RequirementType.valueOf(reqStr) } catch (_: Exception) { RequirementType.BUY }
                val propType = try { PropertyType.valueOf(propStr) } catch (_: Exception) { PropertyType.FLAT }
                val stage = try { CustomerStage.valueOf(stageStr) } catch (_: Exception) { CustomerStage.NEW }

                val customer = Customer(
                    name = customerName,
                    phone = phone,
                    requirementType = reqType,
                    propertyType = propType,
                    preferredLocation = location,
                    budget = budget,
                    inquiryReason = inquiry,
                    stage = stage
                )
                val newId = customerRepository.saveCustomerWithInitialFollowUp(
                    customer = customer,
                    initialFollowUpDate = System.currentTimeMillis() + 86400000L,
                    initialFollowUpTime = "11:00 AM",
                    initialFollowUpNotes = "AI initial follow-up task"
                )

                val reply = "Customer '$customerName' created with ID $newId ($reqType $propType in $location)."
                reply to reply
            }

            "search_customers" -> {
                val query = args.get("query")?.asString ?: ""
                val allActive = customerRepository.getActiveCustomers().first()
                val filtered = allActive.filter { c ->
                    query.isBlank() ||
                    c.name.contains(query, ignoreCase = true) ||
                    c.phone.contains(query) ||
                    c.preferredLocation.contains(query, ignoreCase = true) ||
                    c.inquiryReason.contains(query, ignoreCase = true)
                }

                val resultList = filtered.take(5).map { c ->
                    mapOf(
                        "id" to c.id,
                        "name" to c.name,
                        "phone" to c.phone,
                        "deal" to "${c.requirementType} ${c.propertyType}",
                        "stage" to c.stage.name,
                        "location" to c.preferredLocation,
                        "budget" to (c.budget ?: "Not set")
                    )
                }
                val json = gson.toJson(resultList)
                json to "Found ${filtered.size} matching customer(s)."
            }

            "schedule_site_visit" -> {
                var customerId = args.get("customer_id")?.asLong ?: 0L
                val hint = args.get("customer_name_hint")?.asString
                if (customerId == 0L && !hint.isNullOrBlank()) {
                    val match = customerRepository.getActiveCustomers().first().firstOrNull {
                        it.name.contains(hint, ignoreCase = true)
                    }
                    if (match != null) customerId = match.id
                }

                if (customerId == 0L) {
                    return "Error: Customer not found. Please specify customer name or ID." to "Site visit failed: Customer not identified"
                }

                val location = args.get("property_location")?.asString ?: "Project Site"
                val dateStr = args.get("visit_date")?.asString?.lowercase() ?: "tomorrow"
                val time = args.get("visit_time")?.asString ?: "11:00 AM"
                val notes = args.get("notes")?.asString ?: "Scheduled via AI Copilot"

                val targetDateMillis = when (dateStr) {
                    "today" -> DateUtils.getStartOfDay()
                    "tomorrow" -> DateUtils.getStartOfDay() + 86400000L
                    else -> try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.parse(dateStr)?.time ?: (DateUtils.getStartOfDay() + 86400000L)
                    } catch (_: Exception) {
                        DateUtils.getStartOfDay() + 86400000L
                    }
                }

                customerRepository.scheduleSiteVisit(
                    SiteVisit(
                        customerId = customerId,
                        propertyLocation = location,
                        visitDate = targetDateMillis,
                        visitTime = time,
                        notes = notes
                    )
                )

                val reply = "Site visit scheduled for customer #$customerId at $location on ${DateUtils.formatDate(targetDateMillis)} at $time."
                reply to reply
            }

            "record_call_outcome" -> {
                var customerId = args.get("customer_id")?.asLong ?: 0L
                val hint = args.get("customer_name_hint")?.asString
                if (customerId == 0L && !hint.isNullOrBlank()) {
                    val match = customerRepository.getActiveCustomers().first().firstOrNull {
                        it.name.contains(hint, ignoreCase = true)
                    }
                    if (match != null) customerId = match.id
                }

                if (customerId == 0L) {
                    return "Error: Customer not found." to "Call outcome logging failed"
                }

                val outcomeStr = args.get("outcome")?.asString?.uppercase() ?: "ANSWERED"
                val outcome = try { CallOutcome.valueOf(outcomeStr) } catch (_: Exception) { CallOutcome.ANSWERED }
                val notes = args.get("notes")?.asString ?: "Call logged by AI"
                val scheduleNext = args.get("schedule_next_follow_up")?.asBoolean ?: true
                val nextDays = args.get("next_follow_up_days")?.asInt ?: 2

                val nextDate = if (scheduleNext) DateUtils.getStartOfDay() + (nextDays * 86400000L) else null

                customerRepository.recordCallOutcome(
                    customerId = customerId,
                    outcome = outcome,
                    notes = notes,
                    nextFollowUpDate = nextDate,
                    nextFollowUpTime = "11:00 AM"
                )

                val reply = "Logged call outcome: '${outcome.getDisplayName()}' for customer #$customerId."
                reply to reply
            }

            "update_customer_stage" -> {
                var customerId = args.get("customer_id")?.asLong ?: 0L
                val hint = args.get("customer_name_hint")?.asString
                if (customerId == 0L && !hint.isNullOrBlank()) {
                    val match = customerRepository.getActiveCustomers().first().firstOrNull {
                        it.name.contains(hint, ignoreCase = true)
                    }
                    if (match != null) customerId = match.id
                }

                if (customerId == 0L) {
                    return "Error: Customer not found." to "Stage update failed"
                }

                val stageStr = args.get("stage")?.asString?.uppercase() ?: "NEW"
                val stage = try { CustomerStage.valueOf(stageStr) } catch (_: Exception) { CustomerStage.NEW }

                customerRepository.updateCustomerStage(customerId, stage)
                val reply = "Customer #$customerId updated to stage '${stage.getDisplayName()}'."
                reply to reply
            }

            "get_crm_summary" -> {
                val totalActive = customerRepository.countActiveCustomers().first()
                val summary = followUpRepository.getNotificationSummary()
                val reply = mapOf(
                    "activeCustomers" to totalActive,
                    "todayDueCalls" to summary.totalTodayTasks,
                    "overdueCalls" to summary.totalOverdueTasks,
                    "uniqueCustomersDueToday" to summary.uniqueTodayCustomers
                )
                gson.toJson(reply) to "Fetched CRM summary."
            }

            else -> "Unknown tool '$name'" to "Unknown action"
        }
    }

    private fun createToolObject(
        name: String,
        description: String,
        properties: JsonObject,
        required: List<String>
    ): JsonObject {
        val tool = JsonObject()
        tool.addProperty("type", "function")

        val fn = JsonObject()
        fn.addProperty("name", name)
        fn.addProperty("description", description)

        val params = JsonObject()
        params.addProperty("type", "object")
        params.add("properties", properties)

        val reqArray = JsonArray()
        required.forEach { reqArray.add(it) }
        params.add("required", reqArray)

        fn.add("parameters", params)
        tool.add("function", fn)
        return tool
    }

    private fun createProperty(
        type: String,
        description: String,
        enumList: List<String>? = null
    ): JsonObject {
        val prop = JsonObject()
        prop.addProperty("type", type)
        prop.addProperty("description", description)
        if (!enumList.isNullOrEmpty()) {
            val enumArray = JsonArray()
            enumList.forEach { enumArray.add(it) }
            prop.add("enum", enumArray)
        }
        return prop
    }
}

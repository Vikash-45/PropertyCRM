package com.propertycrm.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.NextAction
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditCustomerUiState(
    val customerId: Long? = null,
    val name: String = "",
    val phone: String = "+91 ",
    val inquiryReason: String = "",
    val propertyType: PropertyType = PropertyType.PLOT,
    val requirementType: RequirementType = RequirementType.BUY,
    val preferredLocation: String = "",
    val preferredSize: String = "",
    val preferredContactTime: String = "",
    val budget: String = "",
    val leadSource: String = "",
    val nextAction: NextAction = NextAction.CALL,
    val assignedPerson: String = "",
    val isDoNotCall: Boolean = false,
    val firstVisitDate: Long? = null,
    val initialFollowUpDate: Long? = DateUtils.getStartOfDay(),
    val initialFollowUpTime: String = "10:00 AM",
    val stage: CustomerStage = CustomerStage.NEW,
    val notes: String = "",
    val isArchived: Boolean = false,
    val isDuplicatePhone: Boolean = false,
    val validationError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false
)

sealed class AddEditEvent {
    object Saved : AddEditEvent()
    data class Error(val message: String) : AddEditEvent()
}

class AddEditCustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val existingCustomerId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCustomerUiState())
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddEditEvent>()
    val eventFlow: SharedFlow<AddEditEvent> = _eventFlow.asSharedFlow()

    init {
        if (existingCustomerId != null && existingCustomerId > 0L) {
            loadCustomer(existingCustomerId)
        }
    }

    private fun loadCustomer(id: Long) {
        viewModelScope.launch {
            val customer = customerRepository.getCustomerByIdSync(id)
            if (customer != null) {
                _uiState.update {
                    it.copy(
                        customerId = customer.id,
                        name = customer.name,
                        phone = customer.phone,
                        inquiryReason = customer.inquiryReason,
                        propertyType = customer.propertyType,
                        requirementType = customer.requirementType,
                        preferredLocation = customer.preferredLocation,
                        preferredSize = customer.preferredSize ?: "",
                        preferredContactTime = customer.preferredContactTime ?: "",
                        budget = customer.budget ?: "",
                        leadSource = customer.leadSource ?: "",
                        nextAction = customer.nextAction,
                        assignedPerson = customer.assignedPerson ?: "",
                        isDoNotCall = customer.isDoNotCall,
                        firstVisitDate = customer.firstVisitDate,
                        initialFollowUpDate = null,
                        stage = customer.stage,
                        notes = customer.notes,
                        isArchived = customer.isArchived,
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, validationError = null) }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, validationError = null) }
        checkDuplicatePhone(phone)
    }

    private fun checkDuplicatePhone(phone: String) {
        viewModelScope.launch {
            val isDup = customerRepository.checkForDuplicatePhone(phone, _uiState.value.customerId)
            _uiState.update { it.copy(isDuplicatePhone = isDup) }
        }
    }

    fun onInquiryReasonChange(reason: String) =
        _uiState.update { it.copy(inquiryReason = reason, validationError = null) }

    fun onPropertyTypeChange(type: PropertyType) =
        _uiState.update { it.copy(propertyType = type) }

    fun onRequirementTypeChange(req: RequirementType) =
        _uiState.update { it.copy(requirementType = req) }

    fun onPreferredLocationChange(loc: String) =
        _uiState.update { it.copy(preferredLocation = loc) }

    fun onPreferredSizeChange(size: String) =
        _uiState.update { it.copy(preferredSize = size) }

    fun onPreferredContactTimeChange(time: String) =
        _uiState.update { it.copy(preferredContactTime = time) }

    fun onBudgetChange(budget: String) =
        _uiState.update { it.copy(budget = budget) }

    fun onLeadSourceChange(source: String) =
        _uiState.update { it.copy(leadSource = source) }

    fun onNextActionChange(action: NextAction) =
        _uiState.update { it.copy(nextAction = action) }

    fun onAssignedPersonChange(person: String) =
        _uiState.update { it.copy(assignedPerson = person) }

    fun onDoNotCallToggle(dnc: Boolean) =
        _uiState.update { it.copy(isDoNotCall = dnc) }

    fun onFirstVisitDateChange(date: Long?) =
        _uiState.update { it.copy(firstVisitDate = date) }

    fun onInitialFollowUpDateChange(date: Long?) =
        _uiState.update { it.copy(initialFollowUpDate = date) }

    fun onInitialFollowUpTimeChange(time: String) =
        _uiState.update { it.copy(initialFollowUpTime = time) }

    fun onStageChange(stage: CustomerStage) =
        _uiState.update { it.copy(stage = stage) }

    fun onNotesChange(notes: String) =
        _uiState.update { it.copy(notes = notes) }

    fun saveCustomer() {
        val s = _uiState.value
        if (s.name.isBlank() || s.phone.isBlank() || s.inquiryReason.isBlank()) {
            _uiState.update { it.copy(validationError = "Please fill all required fields (Name, Phone, Inquiry Reason).") }
            return
        }

        if (!PhoneUtils.isValidPhone(s.phone)) {
            _uiState.update { it.copy(validationError = "Please enter a valid phone number (at least 7 digits).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (s.isEditMode && s.customerId != null) {
                    val customer = Customer(
                        id = s.customerId,
                        name = s.name.trim(),
                        phone = s.phone.trim(),
                        inquiryReason = s.inquiryReason.trim(),
                        propertyType = s.propertyType,
                        requirementType = s.requirementType,
                        preferredLocation = s.preferredLocation.trim(),
                        preferredSize = s.preferredSize.trim().takeIf { it.isNotBlank() },
                        preferredContactTime = s.preferredContactTime.trim().takeIf { it.isNotBlank() },
                        budget = s.budget.trim().takeIf { it.isNotBlank() },
                        leadSource = s.leadSource.trim().takeIf { it.isNotBlank() },
                        nextAction = s.nextAction,
                        assignedPerson = s.assignedPerson.trim().takeIf { it.isNotBlank() },
                        isDoNotCall = s.isDoNotCall,
                        firstVisitDate = s.firstVisitDate,
                        stage = s.stage,
                        notes = s.notes.trim(),
                        isArchived = s.isArchived,
                        updatedAt = System.currentTimeMillis()
                    )
                    customerRepository.updateCustomer(customer)
                } else {
                    val newCustomer = Customer(
                        name = s.name.trim(),
                        phone = s.phone.trim(),
                        inquiryReason = s.inquiryReason.trim(),
                        propertyType = s.propertyType,
                        requirementType = s.requirementType,
                        preferredLocation = s.preferredLocation.trim(),
                        preferredSize = s.preferredSize.trim().takeIf { it.isNotBlank() },
                        preferredContactTime = s.preferredContactTime.trim().takeIf { it.isNotBlank() },
                        budget = s.budget.trim().takeIf { it.isNotBlank() },
                        leadSource = s.leadSource.trim().takeIf { it.isNotBlank() },
                        nextAction = s.nextAction,
                        assignedPerson = s.assignedPerson.trim().takeIf { it.isNotBlank() },
                        isDoNotCall = s.isDoNotCall,
                        firstVisitDate = s.firstVisitDate,
                        stage = s.stage,
                        notes = s.notes.trim()
                    )
                    customerRepository.saveCustomerWithInitialFollowUp(
                        customer = newCustomer,
                        initialFollowUpDate = s.initialFollowUpDate,
                        initialFollowUpTime = s.initialFollowUpTime,
                        initialFollowUpNotes = "Initial follow-up on registration"
                    )
                }
                _eventFlow.emit(AddEditEvent.Saved)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, validationError = e.message) }
                _eventFlow.emit(AddEditEvent.Error(e.message ?: "Failed to save customer"))
            }
        }
    }
}

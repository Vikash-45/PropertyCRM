package com.propertycrm.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class CustomerSortOrder(val displayName: String) {
    RECENTLY_UPDATED("Recently Updated"),
    NAME_ASC("Name (A to Z)"),
    NEWEST_FIRST("Newest First")
}

data class CustomerListStats(
    val totalActive: Int = 0,
    val buyCount: Int = 0,
    val sellCount: Int = 0,
    val rentCount: Int = 0,
    val categoryCounts: Map<PropertyType, Int> = emptyMap()
)

class CustomerListViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _selectedPropertyType = MutableStateFlow<PropertyType?>(null)
    val selectedPropertyType: StateFlow<PropertyType?> = _selectedPropertyType.asStateFlow()

    private val _selectedRequirementType = MutableStateFlow<RequirementType?>(null)
    val selectedRequirementType: StateFlow<RequirementType?> = _selectedRequirementType.asStateFlow()

    private val _selectedStage = MutableStateFlow<CustomerStage?>(null)
    val selectedStage: StateFlow<CustomerStage?> = _selectedStage.asStateFlow()

    private val _sortOrder = MutableStateFlow(CustomerSortOrder.RECENTLY_UPDATED)
    val sortOrder: StateFlow<CustomerSortOrder> = _sortOrder.asStateFlow()

    // Real-time live count metrics across active customers
    val stats: StateFlow<CustomerListStats> = customerRepository.getActiveCustomers().map { list ->
        val propCounts = mutableMapOf<PropertyType, Int>()
        PropertyType.values().forEach { type ->
            propCounts[type] = list.count { it.propertyType == type }
        }
        CustomerListStats(
            totalActive = list.size,
            buyCount = list.count { it.requirementType == RequirementType.BUY },
            sellCount = list.count { it.requirementType == RequirementType.SELL },
            rentCount = list.count { it.requirementType == RequirementType.RENT },
            categoryCounts = propCounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomerListStats()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = _showArchived.flatMapLatest { showArchived ->
        if (showArchived) {
            customerRepository.getArchivedCustomers()
        } else {
            customerRepository.getActiveCustomers()
        }
    }.combine(_searchQuery) { list, query ->
        list to query
    }.combine(_selectedPropertyType) { (list, query), propType ->
        Triple(list, query, propType)
    }.combine(_selectedRequirementType) { (list, query, propType), reqType ->
        FourTuple(list, query, propType, reqType)
    }.combine(_selectedStage) { tuple, stage ->
        FiveTuple(tuple.list, tuple.query, tuple.propType, tuple.reqType, stage)
    }.combine(_sortOrder) { tuple, sortOrder ->
        var result = tuple.list

        // 1. Filter by search query
        if (tuple.query.isNotBlank()) {
            val q = tuple.query.trim().lowercase()
            result = result.filter { customer ->
                customer.name.lowercase().contains(q) ||
                customer.phone.contains(q) ||
                customer.preferredLocation.lowercase().contains(q) ||
                customer.inquiryReason.lowercase().contains(q) ||
                (customer.budget != null && customer.budget.lowercase().contains(q))
            }
        }

        // 2. Filter by property type / category
        if (tuple.propType != null) {
            result = result.filter { it.propertyType == tuple.propType }
        }

        // 3. Filter by requirement type (Buy / Sell / Rent)
        if (tuple.reqType != null) {
            result = result.filter { it.requirementType == tuple.reqType }
        }

        // 4. Filter by customer stage
        if (tuple.stage != null) {
            result = result.filter { it.stage == tuple.stage }
        }

        // 5. Sort
        when (sortOrder) {
            CustomerSortOrder.RECENTLY_UPDATED -> result.sortedByDescending { it.updatedAt }
            CustomerSortOrder.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            CustomerSortOrder.NEWEST_FIRST -> result.sortedByDescending { it.createdAt }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setShowArchived(show: Boolean) {
        _showArchived.value = show
    }

    fun setSelectedPropertyType(type: PropertyType?) {
        _selectedPropertyType.value = type
    }

    fun setSelectedRequirementType(type: RequirementType?) {
        _selectedRequirementType.value = type
    }

    fun setSelectedStage(stage: CustomerStage?) {
        _selectedStage.value = stage
    }

    fun setSortOrder(order: CustomerSortOrder) {
        _sortOrder.value = order
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedPropertyType.value = null
        _selectedRequirementType.value = null
        _selectedStage.value = null
    }

    fun hasActiveFilters(): Boolean {
        return _searchQuery.value.isNotBlank() ||
               _selectedPropertyType.value != null ||
               _selectedRequirementType.value != null ||
               _selectedStage.value != null
    }
}

private data class FourTuple<A, B, C, D>(val list: A, val query: B, val propType: C, val reqType: D)
private data class FiveTuple<A, B, C, D, E>(val list: A, val query: B, val propType: C, val reqType: D, val stage: E)

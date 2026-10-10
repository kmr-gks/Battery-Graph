package com.gukos.battery_graph.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.BatterySnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BatteryViewModel(
    private val repository: BatteryRepository
) : ViewModel() {

    val records: StateFlow<List<BatteryRecord>> =
        repository.getRecords()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )

    fun recordNow() {
        viewModelScope.launch {
            repository.recordBatteryLevel()
        }
    }

    fun onBatteryChanged(snapshot: BatterySnapshot) {
        viewModelScope.launch {
            repository.recordIfChanged(snapshot)
        }
    }
}

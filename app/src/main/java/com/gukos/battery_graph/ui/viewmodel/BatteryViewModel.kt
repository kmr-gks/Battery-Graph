package com.gukos.battery_graph.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.BatterySnapshot
import com.gukos.battery_graph.util.MonitorSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BatteryViewModel(
    private val repository: BatteryRepository,
    private val monitorSettings: MonitorSettings
) : ViewModel() {

    val highPrecisionEnabled: StateFlow<Boolean> = monitorSettings.highPrecisionEnabled

    fun setHighPrecisionEnabled(enabled: Boolean) {
        monitorSettings.setHighPrecisionEnabled(enabled)
    }

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

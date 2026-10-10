package com.gukos.battery_graph.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.ui.graph.GraphPeriod
import com.gukos.battery_graph.util.BatterySnapshot
import com.gukos.battery_graph.util.MonitorSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _period = MutableStateFlow(GraphPeriod.HOURS_24)
    val period: StateFlow<GraphPeriod> = _period.asStateFlow()

    fun selectPeriod(period: GraphPeriod) {
        _period.value = period
    }

    /** 選択中の期間の記録（古い順） */
    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<BatteryRecord>> =
        _period
            .flatMapLatest { repository.getRecordsSince(System.currentTimeMillis() - it.durationMillis) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    // 画面表示中に受け取った最新のバッテリー状態
    private val _currentStatus = MutableStateFlow<BatterySnapshot?>(null)
    val currentStatus: StateFlow<BatterySnapshot?> = _currentStatus.asStateFlow()

    fun onBatteryChanged(snapshot: BatterySnapshot) {
        _currentStatus.value = snapshot
        viewModelScope.launch {
            repository.recordIfChanged(snapshot)
        }
    }
}

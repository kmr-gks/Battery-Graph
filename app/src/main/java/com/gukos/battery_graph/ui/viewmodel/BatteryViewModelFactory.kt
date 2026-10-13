package com.gukos.battery_graph.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.GraphSettings
import com.gukos.battery_graph.util.MonitorSettings

class BatteryViewModelFactory(
    private val repository: BatteryRepository,
    private val monitorSettings: MonitorSettings,
    private val graphSettings: GraphSettings
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BatteryViewModel(repository, monitorSettings, graphSettings) as T
    }
}

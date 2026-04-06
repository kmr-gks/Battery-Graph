package com.gukos.battery_graph.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gukos.battery_graph.data.database.AppDatabase
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.BatteryManagerWrapper

class BatteryViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(context)
        val dao = db.batteryDao()
        val wrapper = BatteryManagerWrapper(context)
        val repository = BatteryRepository(dao, wrapper)

        return BatteryViewModel(repository) as T
    }
}
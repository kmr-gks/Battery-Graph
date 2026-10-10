package com.gukos.battery_graph.ui.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel


@Composable
fun GraphScreen(viewModel: BatteryViewModel) {

    val records by viewModel.records.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {

        if (records.size < 2) {
            Text("loading graph...")
        } else {
            BatteryGraph(records)
        }
    }
}

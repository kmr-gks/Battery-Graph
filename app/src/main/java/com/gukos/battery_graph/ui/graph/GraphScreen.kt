package com.gukos.battery_graph.ui.graph

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel


@Composable
fun GraphScreen(viewModel: BatteryViewModel) {

    val records by viewModel.records.collectAsState(initial = emptyList())

    Log.e("GraphScreen", "records: ${records.size}")

    Box(modifier = Modifier.fillMaxSize()) {

        if (records.size < 2) {
            Text("loading graph...")
        } else {
            BatteryGraph(records)
        }
    }
}
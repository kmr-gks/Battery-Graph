package com.gukos.battery_graph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gukos.battery_graph.ui.theme.BatteryGraphTheme
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModelFactory

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        setContent {
            val viewModel: BatteryViewModel = viewModel(
                factory = BatteryViewModelFactory(applicationContext)
            )

            BatteryScreen(viewModel)
        }
    }

	@Composable
fun BatteryScreen(viewModel: BatteryViewModel) {
    val records by viewModel.records.collectAsState()

    Column {
        Button(onClick = { viewModel.recordNow() }) {
            Text("記録する")
        }

        LazyColumn {
            items(records) { record ->
                Text("${record.timestamp} : ${record.level}%")
            }
        }
    }
}
}
package ai.accelera.spmlibrary.demo

import ai.accelera.library.banners.AcceleraBanners
import ai.accelera.library.banners.AcceleraContentHandle
import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun StressTab(
    modifier: Modifier,
    showError: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var container by remember { mutableStateOf<ViewGroup?>(null) }
    var handle by remember { mutableStateOf<AcceleraContentHandle?>(null) }
    var running by rememberSaveable { mutableStateOf(false) }
    var cycle by rememberSaveable { mutableIntStateOf(0) }
    var stressData by rememberSaveable { mutableStateOf(defaultBannerJson) }

    LaunchedEffect(running, container, stressData) {
        if (!running) return@LaunchedEffect
        val target = container ?: run {
            showError("Stress container is not ready")
            running = false
            return@LaunchedEffect
        }
        val data = validateJson(stressData, showError) ?: run {
            running = false
            return@LaunchedEffect
        }
        while (running) {
            handle?.detach()
            handle = AcceleraBanners.attachContentPlaceholder(target, data)
            cycle += 1
            DemoEvents.log("stress attach cycle=$cycle")
            delay(900)
            handle?.refresh()
            DemoEvents.log("stress refresh cycle=$cycle")
            delay(900)
            if (cycle % 3 == 0) {
                handle?.detach()
                DemoEvents.log("stress detach cycle=$cycle")
                delay(400)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            running = false
            handle?.detach()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StressCard(
                stressData = stressData,
                cycle = cycle,
                running = running,
                onStressDataChange = { stressData = it },
                onToggle = { running = !running },
                onRefresh = { handle?.refresh() ?: showError("No stress handle") },
                onDetach = {
                    handle?.detach()
                    handle = null
                },
                placeholder = { PlaceholderBox { container = it } }
            )
        }
    }
}

@Composable
private fun StressCard(
    stressData: String,
    cycle: Int,
    running: Boolean,
    onStressDataChange: (String) -> Unit,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
    onDetach: () -> Unit,
    placeholder: @Composable () -> Unit
) {
    SectionCard(title = "Stress test") {
        Text(
            "This test repeatedly attaches, refreshes and detaches a View placeholder. It is useful for catching lifecycle races, duplicate network requests, stale Div2View instances, leaked video players and crashes from fast user actions."
        )
        JsonTextField("Stress banner data JSON", stressData, onStressDataChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onToggle
            ) {
                Text(if (running) "Stop" else "Start")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onRefresh
            ) {
                Text("Refresh")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onDetach
            ) {
                Text("Detach")
            }
        }
        Text("Cycles: $cycle")
        placeholder()
    }
}

@Preview(showBackground = true)
@Composable
private fun StressCardPreview() {
    SpmLibraryTheme {
        StressCard(
            stressData = defaultBannerJson.trim(),
            cycle = 12,
            running = false,
            onStressDataChange = {},
            onToggle = {},
            onRefresh = {},
            onDetach = {},
            placeholder = { PlaceholderPreviewBox() }
        )
    }
}

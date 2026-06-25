package ai.accelera.spmlibrary.demo

import ai.accelera.library.compose.AcceleraBanner
import ai.accelera.library.compose.AcceleraStories
import ai.accelera.library.compose.rememberAcceleraBannerController
import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ComposeTab(
    modifier: Modifier,
    showError: (String) -> Unit
) {
    var storiesData by remember { mutableStateOf(defaultStoriesJson) }
    var bannerData by remember { mutableStateOf(defaultBannerJson) }
    val bannerController = rememberAcceleraBannerController()

    val storiesMap = remember(storiesData) { jsonToMapOrNull(storiesData) }
    val bannerMap = remember(bannerData) { jsonToMapOrNull(bannerData) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard(title = "Compose stories") {
                JsonTextField("Stories data JSON", storiesData, { storiesData = it })
                Button(onClick = { validateJson(storiesData, showError) }) {
                    Text("Validate stories JSON")
                }
                if (storiesMap != null) {
                    AcceleraStories(
                        data = storiesMap,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Stories JSON is invalid.", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            SectionCard(title = "Compose banner") {
                JsonTextField("Banner data JSON", bannerData, { bannerData = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { bannerController.refresh() }) {
                        Text("Refresh")
                    }
                    OutlinedButton(onClick = { bannerController.detach() }) {
                        Text("Detach")
                    }
                }
                if (bannerMap != null) {
                    AcceleraBanner(
                        data = bannerMap,
                        controller = bannerController,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Banner JSON is invalid.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposeControlsPreview() {
    SpmLibraryTheme {
        SectionCard(title = "Compose banner") {
            JsonTextField("Banner data JSON", defaultBannerJson.trim(), {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) {
                    Text("Refresh")
                }
                OutlinedButton(onClick = {}) {
                    Text("Detach")
                }
            }
            Text("DivKit content appears here after running the app.", color = Color.Gray)
        }
    }
}


package ai.accelera.spmlibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.accelera.library.Accelera
import ai.accelera.library.AcceleraConfig
import ai.accelera.library.DefaultAcceleraDelegate
import ai.accelera.library.compose.AcceleraBanner
import ai.accelera.library.compose.AcceleraStories
import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Accelera SDK
        initializeAccelera()

        enableEdgeToEdge()
        setContent {
            SpmLibraryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExampleScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun initializeAccelera() {
        Accelera.shared.configure(
            config = AcceleraConfig(
                url = BuildConfig.ACCELERA_URL,
                systemToken = BuildConfig.ACCELERA_TOKEN
            )
        )

        Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
            override fun action(action: String) {
                android.util.Log.d("Accelera", "Action received: $action")
            }

            override fun handleUrl(url: android.net.Uri) {
                android.util.Log.d("Accelera", "Handle URL: $url")
            }
        })

        Accelera.shared.setUserInfo(
            """
            {
                "client_id": "asd",
                "language": "KZ"
            }
            """.trimIndent()
        )
    }
}

/**
 * Demo screen using LazyColumn to reproduce and verify the scroll-disappear bug.
 *
 * Reproduction steps:
 *  1. Run the app — stories and banner appear at the top.
 *  2. Scroll down past the filler cards so stories/banner leave the screen.
 *  3. Scroll back up — content must render immediately without navigating away.
 */
@Composable
fun ExampleScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Text(
                text = "Accelera SDK Example",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scroll down and back up — banners must stay visible.",
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        // ── Stories ──────────────────────────────────────────────────────────
        item(key = "stories") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Stories", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Tap a story to open fullscreen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AcceleraStories(
                        data = mapOf(
                            "type" to "stories",
                            "slot" to "story",
                            "channel" to "test_channel"
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Banner ───────────────────────────────────────────────────────────
        item(key = "banner") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Banner", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Loads banner content from API.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AcceleraBanner(
                        data = mapOf(
                            "type" to "banner",
                            "slot" to "messages_top_banner",
                            "channel" to "dev"
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Filler items — scroll these to push stories off-screen ───────────
        items(count = 10, key = { "filler_$it" }) { index ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Filler card ${index + 1} — scroll past this to hide stories",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item(key = "footer") {
            Text(
                text = "Scroll back up to verify stories & banner are still visible.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

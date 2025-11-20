package ai.accelera.spmlibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        // Configure Accelera with your API endpoint and token
        // For demo purposes, using empty config - you can provide custom API via delegate
        Accelera.shared.configure(
            config = AcceleraConfig(
                url = "https://mcp.accelera.ai", // Example URL for banner loading
                systemToken = "your-system-token" // Replace with your token
            )
        )
        
        // Set delegate for logging and handling events
        Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
            override fun action(action: String) {
                android.util.Log.d("Accelera", "Action received: $action")
                // Handle custom actions here
            }
            
            override fun handleUrl(url: android.net.Uri) {
                android.util.Log.d("Accelera", "Handle URL: $url")
                // Handle URLs (e.g., open in browser, deep links, etc.)
            }
        })
        
        // Set user info (optional)
        Accelera.shared.setUserInfo(
            """
            {
                "clientId": "123",
                "email": "user@example.com",
                "theme": "dark"
            }
            """.trimIndent()
        )
    }
}

@Composable
fun ExampleScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Accelera SDK Example",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "This example demonstrates how to use Accelera SDK with Compose",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Divider()
        
        // Example: Banner
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Banner Example",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Loads banner content from API",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Banner composable
                AcceleraBanner(
                    data = mapOf(
                        "type" to "banner",
                        "category" to "main_screen"
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Example: Stories
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Stories Example",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Loads stories content from API. Tap on a story to open fullscreen.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Stories composable - displays horizontal ribbon of stories
                // Click handling for fullscreen is done via div-action://fullscreen in AcceleraUrlHandler
                AcceleraStories(
                    data = mapOf(
                        "type" to "stories"
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

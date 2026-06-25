package ai.accelera.spmlibrary

import ai.accelera.library.Accelera
import ai.accelera.library.DefaultAcceleraDelegate
import ai.accelera.spmlibrary.demo.DemoApp
import ai.accelera.spmlibrary.demo.DemoEvents
import ai.accelera.spmlibrary.demo.DemoLogLevel
import ai.accelera.spmlibrary.demo.configureAccelera
import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAccelera(
            url = BuildConfig.ACCELERA_URL,
            token = BuildConfig.ACCELERA_TOKEN
        )
        installDelegate()

        enableEdgeToEdge()
        setContent {
            SpmLibraryTheme {
                DemoApp()
            }
        }
    }

    private fun installDelegate() {
        Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
            override fun action(action: String) {
                DemoEvents.log("action: $action", DemoLogLevel.Action)
            }

            override fun action(actionName: String, params: Map<String, String>, meta: Any?) {
                DemoEvents.log("action: $actionName params=$params meta=$meta", DemoLogLevel.Action)
            }

            override fun handleUrl(url: Uri) {
                DemoEvents.log("url: $url", DemoLogLevel.Url)
            }

            override fun log(message: String) {
                DemoEvents.log(message)
            }

            override fun error(error: String) {
                DemoEvents.error(error)
            }
        })
    }
}

package ai.accelera.spmlibrary.demo

import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@Composable
fun DemoApp() {
    var selectedTab by remember { mutableStateOf(DemoTab.Home) }
    var configState by remember {
        mutableStateOf(
            DemoConfigState(
                appliedUrl = ai.accelera.spmlibrary.BuildConfig.ACCELERA_URL,
                appliedToken = ai.accelera.spmlibrary.BuildConfig.ACCELERA_TOKEN
            )
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(snackbarHostState) {
        DemoEvents.snackbarSink = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
        onDispose { DemoEvents.snackbarSink = null }
    }

    DemoScaffold(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        snackbarHostState = snackbarHostState,
        configState = configState,
        onConfigStateChange = { configState = it }
    )
}

@Composable
private fun DemoScaffold(
    selectedTab: DemoTab,
    onTabSelected: (DemoTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    configState: DemoConfigState,
    onConfigStateChange: (DemoConfigState) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                DemoTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            DemoTab.Home -> HomeTab(
                modifier = contentModifier,
                configState = configState,
                onConfigStateChange = onConfigStateChange,
                showError = DemoEvents::error
            )

            DemoTab.Log -> LogTab(
                entries = DemoEvents.logEntries,
                onClear = DemoEvents::clear,
                modifier = contentModifier
            )

            DemoTab.Compose -> ComposeTab(
                modifier = contentModifier,
                showError = DemoEvents::error
            )

            DemoTab.Stress -> StressTab(
                modifier = contentModifier,
                showError = DemoEvents::error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoNavigationPreview() {
    SpmLibraryTheme {
        DemoScaffold(
            selectedTab = DemoTab.Home,
            onTabSelected = {},
            snackbarHostState = SnackbarHostState(),
            configState = DemoConfigState(
                appliedUrl = "https://api.example.com",
                appliedToken = "token"
            ),
            onConfigStateChange = {}
        )
    }
}

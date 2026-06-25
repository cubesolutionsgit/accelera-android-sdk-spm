package ai.accelera.spmlibrary.demo

import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun LogTab(
    entries: List<DemoLogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(DemoLogLevel.All) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(text.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open selected document")
            }.onSuccess {
                DemoEvents.log("Exported visible log entries")
            }.onFailure { error ->
                DemoEvents.error("Export failed: ${error.message}")
            }
        }
    }
    val filteredEntries = remember(entries, query, selectedLevel) {
        entries.filter { entry ->
            val matchesLevel = selectedLevel == DemoLogLevel.All || entry.level == selectedLevel
            val matchesQuery = query.isBlank() || entry.text.contains(query, ignoreCase = true)
            matchesLevel && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Log", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
        item(key = "filters") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Filter logs") }
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoLogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { selectedLevel = level },
                        label = { Text(level.name) }
                    )
                }
            }
            Text(
                "${filteredEntries.size} of ${entries.size} entries",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    enabled = filteredEntries.isNotEmpty(),
                    onClick = {
                        clipboard.setText(AnnotatedString(filteredEntries.toExportText()))
                        DemoEvents.log("Copied ${filteredEntries.size} visible log entries")
                    }
                ) {
                    Text("Copy visible")
                }
                OutlinedButton(
                    enabled = entries.isNotEmpty(),
                    onClick = {
                        clipboard.setText(AnnotatedString(entries.toExportText()))
                        DemoEvents.log("Copied all log entries")
                    }
                ) {
                    Text("Copy all")
                }
                OutlinedButton(
                    enabled = filteredEntries.isNotEmpty(),
                    onClick = { context.shareLogs(filteredEntries) }
                ) {
                    Text("Share visible")
                }
                OutlinedButton(
                    enabled = filteredEntries.isNotEmpty(),
                    onClick = {
                        pendingExportText = filteredEntries.toExportText()
                        exportLauncher.launch(logFileName())
                    }
                ) {
                    Text("Export .txt")
                }
            }
        }
        if (filteredEntries.isEmpty()) {
            item(key = "empty") {
                Text("No matching log entries.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(
                items = filteredEntries,
                key = { entry -> entry.id }
            ) { entry ->
                SelectionContainer {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

private fun List<DemoLogEntry>.toExportText(): String {
    return joinToString(separator = "\n") { it.text }
}

private fun Context.shareLogs(entries: List<DemoLogEntry>) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Accelera demo logs")
        putExtra(Intent.EXTRA_TEXT, entries.toExportText())
    }
    startActivity(Intent.createChooser(sendIntent, "Share logs"))
}

private fun logFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "accelera-demo-logs-$stamp.txt"
}

@Preview(showBackground = true)
@Composable
private fun LogTabPreview() {
    SpmLibraryTheme {
        LogTab(
            entries = listOf(
                DemoLogEntry(1, DemoLogLevel.Error, "18:55:21  [Accelera] Error: Failed to parse DivData"),
                DemoLogEntry(2, DemoLogLevel.Error, "18:55:21  [Accelera] Error: Failed to parse DivData")
            ),
            onClear = {}
        )
    }
}

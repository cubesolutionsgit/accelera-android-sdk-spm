package ai.accelera.spmlibrary.demo

import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
internal fun JsonTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    secret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    )
}

@Composable
internal fun ActionRow(
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
    tertiary: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = primary.second
        ) {
            Text(primary.first)
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = secondary.second
        ) {
            Text(secondary.first)
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = tertiary.second
        ) {
            Text(tertiary.first)
        }
    }
}

@Composable
internal fun PlaceholderBox(
    modifier: Modifier = Modifier,
    onReady: (ViewGroup) -> Unit
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                minimumHeight = (128 * resources.displayMetrics.density).toInt()
                onReady(this)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .background(Color(0xFFF4F6F8))
    )
}

@Composable
internal fun PlaceholderPreviewBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .background(Color(0xFFF4F6F8))
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionCardPreview() {
    SpmLibraryTheme {
        SectionCard(title = "Configuration") {
            JsonTextField(
                label = "Data JSON",
                value = defaultBannerJson.trim(),
                onValueChange = {}
            )
            ActionRow(
                primary = "Attach" to {},
                secondary = "Refresh" to {},
                tertiary = "Detach" to {}
            )
        }
    }
}

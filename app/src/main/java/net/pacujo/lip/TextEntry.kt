@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TextEntry(
    label: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(UNIVERSAL_PADDING),
    value: String,
    setter: (String) -> Unit,
) {
    TextField(
        label = { Text(text = label) },
        modifier = modifier,
        value = value,
        onValueChange = setter,
    )
}

@Composable
fun TextEntry(
    label: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(UNIVERSAL_PADDING),
    value: TextFieldValue,
    setter: (TextFieldValue) -> Unit,
) {
    TextField(
        label = { Text(label) },
        modifier = modifier,
        value = value,
        onValueChange = setter,
    )
}

@Preview
@Composable
fun TextEntryPreview() {
    var value by remember { mutableStateOf(TextFieldValue()) }
    TextEntry(
        label = "Server Host",
        value = value,
        setter = { value = it },
    )
}

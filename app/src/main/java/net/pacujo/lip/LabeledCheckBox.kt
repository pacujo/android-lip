package net.pacujo.lip

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LabeledCheckBox(label: String, value: Boolean, setter: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.toggleable(
            value = value, onValueChange = { setter(!value) },
            role = Role.Checkbox,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = value, onCheckedChange = null)
        Text(
            text = label, Modifier.padding(start = 2.dp),
        )
    }
}

@Preview
@Composable
fun LabeledCheckBoxPreview() {
    LabeledCheckBox(
        label = "UseTLS",
        value = true,
        setter = {},
    )
}

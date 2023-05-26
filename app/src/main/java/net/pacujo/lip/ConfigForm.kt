@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@file:Suppress("SameParameterValue")

package net.pacujo.lip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

private fun goodTcpPort(portText: String): Boolean {
    val portOrNull = portText.toIntOrNull()
    return portOrNull != null && portOrNull >= 1 && portOrNull <= 65535
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigForm(
    configuration: LiveData<Configuration>,
    onSubmit: (Configuration) -> Unit,
) {
    val obsConfiguration = configuration.observeAsState()
    
    var state by rememberSaveable {
        mutableStateOf(obsConfiguration.value!!)
    }
    var portText by rememberSaveable {
        mutableStateOf(obsConfiguration.value!!.port.toString())
    }

    fun goodToSubmit() =
        state.nick != "" && state.name != "" &&
                state.serverHost != "" && goodTcpPort(portText)

    fun resetConfig() {
        state = obsConfiguration.value!!
        portText = state.port.toString()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { ConfigTopBar() },
        ) { contentPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(contentPadding),
            ) {
                TextEntry(
                    label = "Your Nick", value = state.nick,
                    setter = { state = state.copy(nick = it) },
                )
                TextEntry(
                    label = "Your Name (optional)", value = state.name,
                    setter = { state = state.copy(name = it) },
                )
                TextEntry(
                    label = "Server Host", value = state.serverHost,
                    setter = { state = state.copy(serverHost = it) },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UNIVERSAL_PADDING),
                ) {
                    TextEntry(
                        label = "TCP Port", modifier = Modifier.weight(1.0f),
                        value = portText,
                    ) {
                        portText = it
                    }
                    Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                    LabeledCheckBox(
                        label = "UseTls", value = state.useTls,
                        setter = { state = state.copy(useTls = it) },
                    )
                }
                Column(modifier = Modifier.weight(1.0f)) {
                    Autojoins(
                        chatNames = state.autojoins,
                        setter = { state = state.copy(autojoins = it) },
                    )
                }
                OkOrReset(
                    goodToSubmit = goodToSubmit(),
                    goodToReset = state != obsConfiguration.value!!,
                    onSubmit = { onSubmit(state) },
                    onReset = ::resetConfig,
                )
            }
        }
    }
}

@Composable
fun ConfigTopBar() {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Lip IRC Client",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
fun OkOrReset(
    goodToSubmit: Boolean,
    goodToReset: Boolean,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(UNIVERSAL_PADDING),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(enabled = goodToSubmit, onClick = onSubmit) {
            Text(text = "Ok")
        }
        OutlinedButton(enabled = goodToReset, onClick = onReset) {
            Text(text = "Reset")
        }
    }
}


@Composable
fun Autojoins(
    chatNames: List<String>,
    setter: (List<String>) -> Unit,
) {
    if (chatNames.isEmpty())
        return

    fun onDismiss(name: String) {
        setter(chatNames.filter { it != name })
    }

    val sorted = chatNames.sortedBy { it.toIRCLower() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Autojoin Chats/Channels",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(UNIVERSAL_PADDING),
        )
        FlowRow(
            modifier = Modifier.padding(UNIVERSAL_PADDING / 2),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (chatName in sorted) {
                if (validNick(chatName))
                    DismissChatButton(
                        nick = chatName,
                        onDismiss = ::onDismiss,
                    )
                else
                    DismissChannelButton(
                        name = chatName,
                        onDismiss = ::onDismiss,
                    )
            }
        }
    }
}

@Composable
fun DismissChatButton(
    nick: String,
    onDismiss: (String) -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.padding(UNIVERSAL_PADDING),
        onClick = { onDismiss(nick) },
    ) {
        DismissLabel(
            label = nick,
        )
    }
}

@Composable
fun DismissChannelButton(
    name: String,
    onDismiss: (String) -> Unit,
) {
    Button(
        modifier = Modifier.padding(UNIVERSAL_PADDING),
        onClick = { onDismiss(name) },
    ) {
        DismissLabel(
            label = name,
        )
    }
}

@Composable
fun DismissLabel(
    label: String,
) {
    Text(label)
    Icon(
        imageVector = Icons.Default.Clear,
        contentDescription = "Do not autojoin",
    )
}

private enum class AutojoinTestChoice {
    EMPTY, NORMAL, CROWDED
}

private fun autojoinTestMaterial(choice: AutojoinTestChoice): List<String> {
    return when (choice) {
        AutojoinTestChoice.EMPTY -> listOf()
        AutojoinTestChoice.NORMAL -> listOf(
            "#testudo",
            "pacujo",
        )
        AutojoinTestChoice.CROWDED -> ((1..100).map {
            it.toString()
        })
    }
}

@Preview
@Composable
fun ConfigFormPreview() {
    ConfigForm(
        configuration = MutableLiveData(
            Configuration.default().copy(
                nick = "testudo",
                name = "testanto",
                autojoins = autojoinTestMaterial(AutojoinTestChoice.NORMAL),
            )
        ),
        onSubmit = {}
    )
}


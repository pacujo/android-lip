@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

@Composable
fun Join(
    chatInfo: LiveData<Map<String, ChatInfo>>,
    onConsole: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var chatName by rememberSaveable { mutableStateOf("") }

    fun goodToSubmit() = validNick(chatName) || validChannelName(chatName)

    fun imageVector() =
        if (validNick(chatName))
            Icons.Default.Person
        else
            Icons.Default.Send

    fun setChatName(name: String) {
        if (name.isEmpty() || name.last() != '\n')
            chatName = name
        else if (goodToSubmit())
            onJoin(chatName)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(topBar = { JoinTopBar() }) { contentPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                    )
                    .padding(contentPadding),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UNIVERSAL_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextEntry(
                        modifier = Modifier.weight(1f),
                        label = "Channel/nick",
                        value = chatName,
                        setter = { setChatName(it) },
                    )
                    Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                    Button(
                        enabled = goodToSubmit(),
                        modifier = Modifier.size(size = 40.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = { onJoin(chatName) },
                    ) {
                        Icon(
                            imageVector = imageVector(),
                            contentDescription = "Join",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                if (chatInfo.value!!.isNotEmpty())
                    SingleClickJoin(
                        chatInfo = chatInfo,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        onConsole = onConsole,
                        onJoin = onJoin,
                    )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleClickJoin(
    chatInfo: LiveData<Map<String, ChatInfo>>,
    modifier: Modifier,
    onConsole: () -> Unit,
    onJoin: (String) -> Unit,
) {
    val obsChatInfo = chatInfo.observeAsState().value!!
    Text(
        text = "Chats/Channels",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(UNIVERSAL_PADDING),
    )
    Column(
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier.padding(UNIVERSAL_PADDING / 2),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (key in obsChatInfo.keys.sorted()) {
                if (key.isEmpty())
                    continue
                val info = obsChatInfo[key]!!
                val obsTotalCount = info.totalCount.observeAsState()
                val obsSeenCount = info.seenCount.observeAsState()
                val unseen = obsTotalCount.value!! - obsSeenCount.value!!
                val badgeCount = if (unseen > 0L) unseen else null
                if (validNick(key))
                    ChatButton(
                        nick = info.name,
                        badgeCount = badgeCount,
                        onJoin = onJoin,
                    )
                else
                    ChannelButton(
                        name = info.name,
                        badgeCount = badgeCount,
                        onJoin = onJoin,
                    )
            }
            val consoleInfo = obsChatInfo[""]!!
            val obsConsoleTotal = consoleInfo.totalCount.observeAsState()
            val obsConsoleSeen = consoleInfo.seenCount.observeAsState()
            ConsoleButton(
                badged = obsConsoleTotal.value != obsConsoleSeen.value,
                onConsole = onConsole,
            )
        }
    }
}

@Composable
fun ChatButton(
    nick: String,
    badgeCount: Long?,
    onJoin: (String) -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.padding(UNIVERSAL_PADDING),
        onClick = { onJoin(nick) },
    ) {
        BadgedLabel(
            label = nick,
            badgeObject = badgeCount,
        )
    }
}

@Composable
fun ChannelButton(
    name: String,
    badgeCount: Long?,
    onJoin: (String) -> Unit,
) {
    Button(
        modifier = Modifier.padding(UNIVERSAL_PADDING),
        onClick = { onJoin(name) },
    ) {
        BadgedLabel(
            label = name,
            badgeObject = badgeCount,
        )
    }
}

@Composable
fun ConsoleButton(
    badged: Boolean,
    onConsole: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.padding(UNIVERSAL_PADDING),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        onClick = onConsole,
    ) {
        BadgedLabel(
            label = "Lip Console",
            badgeObject = if (badged) "" else null,
        )
    }
}

@Composable
fun BadgedLabel(
    label: String,
    badgeObject: Any?,
) {
    if (badgeObject == null)
        Text(label)
    else {
        BadgedBox(
            badge = {
                Badge {
                    Text(badgeObject.toString())
                }
            }
        ) {
            Text(label)
        }
    }
}

@Composable
fun JoinTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Join Chat/Channel",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Preview
@Composable
fun JoinPreview() {
    Join(
        chatInfo = MutableLiveData(
            mapOf(
                "" to ChatInfo(
                    name = "",
                    totalCount = MutableLiveData(7L),
                    seenCount = MutableLiveData(0L),
                ),
                "pacujo" to ChatInfo(
                    name = "pacujo",
                    totalCount = MutableLiveData(20L),
                    seenCount = MutableLiveData(9L),
                ),
                "#testudo" to ChatInfo(
                    name = "#testudo",
                    totalCount = MutableLiveData(100L),
                    seenCount = MutableLiveData(0L),
                ),
            )
        ),
        onConsole = {},
        onJoin = {},
    )
}

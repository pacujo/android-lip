package net.pacujo.lip

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    configuration: LiveData<Configuration>,
    chatName: String,
    contents: LiveData<Array<ProcessedLine>>,
    chatStatus: LiveData<List<ChatStatus>>,
    onSend: (String) -> Unit,
    toggleAutojoin: () -> Unit,
    back: () -> Unit,
) {
    val chatKey = chatName.toIRCLower()
    val obsConfiguration = configuration.observed()

    val favorite =
        obsConfiguration.autojoins.map { it.toIRCLower() }.contains(chatKey)

    val otherChats =
        chatStatus.observed().filter { it.name.toIRCLower() != chatKey }

    var message by rememberSaveable { mutableStateOf("") }

    val toastContext = LocalContext.current

    fun send() {
        if (message.startsWith("//"))
            onSend(message.substring(1))
        else
            onSend(message)
        message = ""
    }

    fun goodToSend() = message.isNotBlank() &&
            (message[0] != '/' || message.startsWith("//"))

    fun setMessage(text: String) {
        when {
            text.isEmpty() || text.last() != '\n' -> message = text

            goodToSend() -> send()

            message.isBlank() ->
                Toast.makeText(
                    toastContext,
                    "Blank message not sent",
                    Toast.LENGTH_SHORT,
                ).show()

            else ->
                Toast.makeText(
                    toastContext,
                    "Initial slash must be doubled",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    title = chatName,
                    nick = obsConfiguration.nick,
                    favorite = favorite,
                    toggleFavorite = toggleAutojoin,
                    otherChatStatus = otherChats,
                    back = back,
                )
                     },
        ) { contentPadding ->
            Column(modifier = Modifier.padding(contentPadding)) {
                Log(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contents = contents,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UNIVERSAL_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextEntry(
                        modifier = Modifier.weight(1f),
                        label = "Message",
                        value = message,
                        setter = { setMessage(it) },
                    )
                    Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                    Button(
                        enabled = goodToSend(),
                        modifier = Modifier.size(size = 40.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = ::send,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Join",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ChatViewPreview() {
    val timestamp = Instant.now()
    ChatView(
        configuration = MutableLiveData(Configuration.default()),
        chatName = "#hottub",
        contents = MutableLiveData(
            arrayListOf(
                LogLine(
                    timestamp = timestamp,
                    from = null,
                    line = IRCString(string = "Hello"),
                    mood = Mood.LOG,
                ),
                LogLine(
                    timestamp = timestamp,
                    from = "Cirque",
                    line = IRCString(string = "\u0012world"),
                    mood = Mood.INFO,
                ),
                LogLine(
                    timestamp = timestamp,
                    from = "du",
                    line = IRCString(string = "This is \u0002me\u0002 talking"),
                    mood = Mood.MINE,
                ),
                LogLine(
                    timestamp = timestamp,
                    from = "soleil",
                    line = IRCString(string = "I can hear \u0015you"),
                    mood = Mood.THEIRS,
                ),
                LogLine(
                    timestamp = timestamp,
                    from = null,
                    line = IRCString(
                        string = "\u00037,1Add \u00036,15some\u0003 color",
                    ),
                    mood = Mood.INFO,
                ),
            ).toLogBuffer().getAll(),
        ),
        chatStatus = MutableLiveData(
            listOf(
                ChatStatus(
                    name = "pacujo",
                    totalCount = MutableLiveData(20L),
                    seenCount = MutableLiveData(9L),
                ),
                ChatStatus(
                    name = "#testudo",
                    totalCount = MutableLiveData(100L),
                    seenCount = MutableLiveData(100L),
                ),
            )
        ),
        onSend = {},
        toggleAutojoin = {},
        back = {},
    )
}

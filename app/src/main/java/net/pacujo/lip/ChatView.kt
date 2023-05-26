package net.pacujo.lip

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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    configuration: LiveData<Configuration>,
    chatName: LiveData<String>,
    contents: LiveData<Array<ProcessedLine>>,
    onSend: (String) -> Unit,
    toggleAutojoin: () -> Unit,
    back: () -> Unit,
) {
    val obsChatName = chatName.observeAsState().value!!
    val favorite =
        configuration.observeAsState().value!!.autojoins.map {
            it.toIRCLower()
        }.contains(obsChatName.toIRCLower())
    var message by rememberSaveable { mutableStateOf("") }

    fun send() {
        onSend(message)
        message = ""
    }

    fun setMessage(text: String) {
        if (text.isEmpty() || text.last() != '\n')
            message = text
        else if (text.isNotBlank())
            send()
    }

    fun goodToSend() = message.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    chatName = obsChatName,
                    favorite = favorite,
                    toggleFavorite = toggleAutojoin,
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
        chatName = MutableLiveData("#hottub"),
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
        onSend = {},
        toggleAutojoin = {},
        back = {},
    )
}
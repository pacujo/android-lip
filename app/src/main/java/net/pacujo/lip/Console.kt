@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package net.pacujo.lip

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant

@Composable
fun Console(
    contents: LiveData<Array<ProcessedLine>>,
    back: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    chatName = "Lip Console",
                    back = back,
                )
                     },
        ) {
            Log(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(it),
                contents = contents,
                contentPadding = it,
            )
        }
    }
}

@Preview
@Composable
fun ConsolePreview() {
    val timestamp = Instant.now()
    Console(
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
                    line = IRCString(
                        string = "This is \u0002me\u0002 talking",
                    ),
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
        back = {},
    )
}
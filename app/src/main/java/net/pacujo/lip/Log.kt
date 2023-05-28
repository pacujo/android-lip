package net.pacujo.lip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant

@Composable
fun Log(
    modifier: Modifier = Modifier,
    contents: LiveData<Array<ProcessedLine>>,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val obsContents = contents.observed()
    val listState = rememberLazyListState()
    LaunchedEffect(obsContents) {
        listState.animateScrollToItem(obsContents.size)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.background(
            MaterialTheme.colorScheme.background,
        ),
        contentPadding = contentPadding,
    ) {
        items(obsContents.size) {
            Text(
                text = obsContents[it].second.markParagraphStyle(
                    ParagraphStyle(
                        textIndent = TextIndent(restLine = 10.sp),
                    ),
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

data class IRCTextStyle(
    val bold: Boolean = false,
    val underline: Boolean = false,
    val italic: Boolean = false,
    val fgColor: Int? = null,
    val bgColor: Int? = null,
)

@Preview
@Composable
fun LogPreview() {
    val timestamp = Instant.now()
    val initialLines = arrayListOf(
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
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Log(
            contents = MutableLiveData(initialLines.toLogBuffer().getAll()),
        )
    }
}

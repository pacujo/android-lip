@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
    onPart: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit,
    back: () -> Unit,
) {
    val chatKey = chatName.toIRCLower()
    val obsConfiguration = configuration.observed()

    val otherChats =
        chatStatus.observed().filter { it.name.toIRCLower() != chatKey }

    val (expanded, setExpanded) = remember { mutableStateOf(false) }

    var message by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val toastContext = LocalContext.current

    fun send() {
        if (message.text.startsWith("//"))
            onSend(message.text.substring(1))
        else
            onSend(message.text)
        message = TextFieldValue()
    }

    fun goodToSend() = message.text.isNotBlank() &&
            (message.text[0] != '/' || message.text.startsWith("//"))

    fun setMessage(value: TextFieldValue) {
        when {
            value.text.isEmpty() || value.text.last() != '\n' ->
                message = value

            goodToSend() -> send()

            message.text.isBlank() ->
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

    fun insertToMessage(snippet: String) {
        val head = message.text.substring(0, message.selection.start)
        val tail = message.text.substring(message.selection.end)
        message =
            message.copy(
                text = head + snippet + tail,
                selection = TextRange(head.length + snippet.length),
            )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    chatName = chatName,
                    autojoin = obsConfiguration.amongAutojoins(chatKey),
                    toggleAutojoin = toggleAutojoin,
                    onPart = onPart,
                    onClearChat = onClearChat,
                    onDeleteChat = onDeleteChat,
                    otherChatStatus = otherChats,
                    onBack = back,
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
                    Button(
                        modifier = Modifier.size(size = 40.dp),
                        colors = ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { setExpanded(true) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Join",
                        )
                        EditMenu(expanded, setExpanded, ::insertToMessage)
                    }
                    Spacer(
                        modifier = Modifier.padding(UNIVERSAL_PADDING / 2),
                    )
                    TextEntry(
                        modifier = Modifier.weight(1f),
                        label = "${obsConfiguration.nick} ⮕ $chatName",
                        value = message,
                        setter = ::setMessage,
                    )
                    Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                    Button(
                        enabled = goodToSend(),
                        modifier = Modifier.size(size = 40.dp),
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

@Composable
fun EditMenu(
    expanded: Boolean,
    setExpanded: (Boolean) -> Unit,
    insertMarkup: (String) -> Unit,
) {
    val textIconScale = 1.5f
    var settingColors by remember { mutableStateOf(false) }

    fun chooseColors(fgColor: Int, bgColor: Int) {
        val colorCode =
            when {
                fgColor < 0 -> ""
                bgColor < 0 -> "%02d".format(fgColor)
                else -> "%d,%02d".format(fgColor, bgColor)
            }
        insertMarkup("$ColorMarkup$colorCode")
    }

    @Composable
    fun StyleMenuItem(markup: String, label: String) {
        DropdownMenuItem(
            leadingIcon = {
                Text(
                    modifier = Modifier.scale(textIconScale),
                    text = markup,
                )
            },
            text = { Text(text = label) },
            onClick = {
                setExpanded(false)
                insertMarkup(markup)
            },
        )
    }

    if (settingColors)
        SetColors(
            onDismiss = { settingColors = false },
            chooseColors = ::chooseColors,
        )
    else
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { setExpanded(false) },
        ) {
            StyleMenuItem(BoldMarkup, "Toggle boldface")
            StyleMenuItem(ItalicMarkup, "Toggle italic")
            StyleMenuItem(UnderlineMarkup, "Toggle underline")
            DropdownMenuItem(
                leadingIcon = {
                    Text(
                        modifier = Modifier.scale(textIconScale),
                        text = ColorMarkup,
                    )
                },
                text = { Text(text = "Set colors...") },
                onClick = {
                    setExpanded(false)
                    settingColors = true
                    //insertMarkup(markup)
                },
            )
            Divider()
            StyleMenuItem(OriginalMarkup, "Reset style")
            Divider()
            StyleMenuItem(HideMarkup, "Toggle hiding")
        }
}

private val colors = arrayOf(
    "White", "Black", "Blue", "Green", "Red", "Brown", "Purple", "Orange",
    "Yellow", "Light green", "Cyan", "Light cyan", "Light blue", "Pink",
    "Grey", "Light grey",
)

@Composable
fun SetColors(
    onDismiss: () -> Unit,
    chooseColors: (Int, Int) -> Unit,
) {
    val (fgColor, setFgColor) = remember { mutableStateOf(-1) }
    val (bgColor, setBgColor) = remember { mutableStateOf(-1) }
    val (fgExpanded, setFgExpanded) = remember { mutableStateOf(false) }
    val (bgExpanded, setBgExpanded) = remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface {
            Column(
                modifier = Modifier.padding(UNIVERSAL_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Text Color",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                ForegroundMenu(
                    bgColor,
                    fgExpanded, setFgExpanded, fgColor, setFgColor,
                )
                Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                BackgroundMenu(
                    fgColor,
                    bgExpanded, setBgExpanded, bgColor, setBgColor,
                )
                Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                Row {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.padding(UNIVERSAL_PADDING))
                    Button(
                        onClick = {
                            onDismiss()
                            chooseColors(fgColor, bgColor)
                        },
                    ) {
                        Text("Choose")
                    }
                }
            }
        }
    }
}

@Composable
fun ForegroundMenu(
    bgColor: Int,
    expanded: Boolean, setExpanded: (Boolean) -> Unit,
    color: Int, setColor: (Int) -> Unit,
) {
    fun fgSelection(i: Int) =
        if (i < 0)
            "None"
        else
            "${colors[i]} text ($i)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { setExpanded(!expanded) },
    ) {
        TextField(
            value = fgSelection(color),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                           },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { setExpanded(false) }) {
            for (i in -1 until colors.size)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = fgSelection(i),
                            color = ircColor(i),
                            style = TextStyle(background = ircColor(bgColor)),
                        )
                           },
                    onClick = {
                        setExpanded(false)
                        setColor(i)
                              },
                )
        }
    }
}

@Composable
fun BackgroundMenu(
    fgColor: Int,
    expanded: Boolean, setExpanded: (Boolean) -> Unit,
    color: Int, setColor: (Int) -> Unit,
) {
    fun bgSelection(i: Int) =
        if (i < 0)
            "on default background"
        else
            "on ${colors[i].toIRCLower()} background ($i)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { setExpanded(!expanded) },
    ) {
        TextField(
            value = bgSelection(color),
            onValueChange = {},
            enabled = fgColor >= 0,
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                           },
            modifier = Modifier.menuAnchor(),
        )
        if (fgColor >= 0)
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { setExpanded(false) }) {
                for (i in -1 until colors.size)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = bgSelection(i),
                                color = ircColor(fgColor),
                                style = TextStyle(background = ircColor(i)),
                            )
                               },
                        onClick = {
                            setExpanded(false)
                            setColor(i)
                                  },
                    )
            }
    }
}

@Preview
@Composable
fun ChatViewPreview() {
    val timestamp = Instant.now()
    ChatView(
        configuration = MutableLiveData(
            Configuration.default().copy(nick = "testudo"),
        ),
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
        onPart = {},
        onClearChat = {},
        onDeleteChat = {},
        back = {},
    )
}

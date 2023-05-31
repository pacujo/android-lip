package net.pacujo.lip

import android.util.JsonReader
import android.util.JsonWriter
import androidx.compose.runtime.Composable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val ConfigFileName = "lipConfig.json"
private const val ConfigBackupFileName = "$ConfigFileName.bak"
private const val ConfigNewFileName = "$ConfigFileName.new"

class ChatStatus(
    val name: String,
    private val totalCount: MutableLiveData<Long>,
    private val seenCount: MutableLiveData<Long>,
) {
    constructor(name: String) : this(
        name,
        MutableLiveData(0L),
        MutableLiveData(0L),
    )

    @Composable
    fun observedUnseen() = totalCount.observed() - seenCount.observed()

    fun look() {
        seenCount.value = totalCount.value!!
    }

    fun setTotal(total: Long) {
        totalCount.value = total
    }
}

class LipModel : ViewModel() {
    private lateinit var filesDir: File
    private lateinit var journal: LipJournal

    val state = MutableLiveData(AppState.CONFIGURING)

    val configuration = MutableLiveData(Configuration.default())

    private val consoleLogBuffer = LogBuffer()
    val consoleContents = MutableLiveData(consoleLogBuffer.getAll())
    private val consoleCount = AtomicLong()
    private val consoleStatus = ChatStatus("")

    private val outputBridge = Channel<String>(capacity = 100)

    val chats = sortedMapOf<String, Chat>()
    val chatInfo = MutableLiveData(generateChatInfo())

    val currentChatKey = MutableLiveData<String>()

    fun initialize(filesDir: File) {
        this.filesDir = filesDir
        loadConfiguration()
        journal = LipJournal(File(filesDir, "journal"))
    }

    private fun generateChatInfo(): List<ChatStatus> {
        val result = mutableListOf(consoleStatus)
        for (chat in chats.values)
            result.add(chat.status)
        return result
    }

    private fun loadConfiguration() {
        val configFile = File(filesDir, ConfigFileName)
        configuration.value =
            try {
                JsonReader(configFile.reader()).use { jr ->
                    Configuration.parse(jr).also {
                        jr.expectEnd()
                    }
                }
            } catch (e: IOException) {
                logError("Failed to load configuration", e)
                return
            } catch (e: JsonSchemaException) {
                logError("Invalid configuration file ignored")
                return
            }
    }

    fun configure(newConfiguration: Configuration) {
        check(state.value == AppState.CONFIGURING)
        updateConfiguration(newConfiguration)
        state.value = AppState.JOIN
        communicate()
        for (chatName in newConfiguration.autojoins)
            join(chatName)
    }

    private fun updateConfiguration(newConfiguration: Configuration) {
        configuration.value = newConfiguration
        saveConfiguration()
    }

    private fun saveConfiguration() {
        val configFile = File(filesDir, ConfigFileName)
        val backupFile = File(filesDir, ConfigBackupFileName)
        val newFile = File(filesDir, ConfigNewFileName)
        try {
            JsonWriter(newFile.writer()).use {
                configuration.value!!.emit(it)
            }
        } catch (e: IOException) {
            logError("Cannot write $newFile", e)
            return
        }
        backupFile.delete()
        configFile.renameTo(backupFile)
        newFile.renameTo(configFile)
    }

    fun leaveConsole() {
        check(state.value == AppState.CONSOLE)
        consoleStatus.look()
        state.value = AppState.JOIN
    }

    private fun command(line: String) {
        if (outputBridge.trySend(line).isFailure)
            logWarning("Failed to issue command")
    }

    fun showConsole() {
        check(state.value == AppState.JOIN)
        state.value = AppState.CONSOLE
    }

    fun doJoin(name: String) {
        check(state.value == AppState.JOIN)
        val chat = join(name)
        currentChatKey.value = chat!!.key
        state.value = AppState.CHAT
    }

    private fun join(name: String, limit: Int? = null): Chat? {
        val chatKey = name.toIRCLower()
        return chats[chatKey]
            ?: if (limit != null && chats.size >= limit)
                null
            else
                Chat(
                    name = name,
                    key = chatKey,
                ).also {
                    chats[chatKey] = it
                    chatInfo.value = generateChatInfo()
                }
    }

    private fun communicate() {
        val connectionBridge = Channel<Connection>(0)
        viewModelScope.launchGuarded(Dispatchers.IO) {
            val connection = connect()
            connectionBridge.send(connection)
            transmitter(connection)
        }
        val inputBridge = Channel<String>(capacity = 100)
        viewModelScope.launchGuarded(Dispatchers.IO) {
            connectionBridge.receive().lineReader(inputBridge)
        }
        viewModelScope.launchGuarded {
            receiver(inputBridge)
        }
    }

    private suspend fun connect() =
        with(configuration.value!!) {
            if (useTls)
                TlsConnection.connect(
                    hostname = serverHost,
                    port = port
                )
            else
                TcpConnection.connect(
                    hostname = serverHost,
                    port = port
                )
        }

    private suspend fun transmitter(connection: Connection) {
        with(configuration.value!!) {
            connection.transmitLine("NICK $nick")
            connection.transmitLine("USER $nick 0 * :$name")
        }
        while (true)
            connection.transmitLine(outputBridge.receive())
    }

    private suspend fun receiver(inputBridge: ReceiveChannel<String>) {
        try {
            while (true) {
                var line = inputBridge.receive()
                val now = Instant.now()
                while (true) {
                    actOnLine(now, line)
                    val result = inputBridge.tryReceive()
                    if (result.isFailure)
                        break
                    line = result.getOrThrow()
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            displayOnConsole(
                timestamp = Instant.now(),
                line = "Disconnected",
                mood = Mood.ERROR,
            )
        }
    }

    private fun actOnLine(timestamp: Instant, line: String) {
        val (prefix, tail) = parsePrefix(line)
        val (cmd, rest) = parseCommand(tail)
        if (cmd == null) {
            val repr = line.repr()
            logWarning("Incomprehensible input from server: \"$repr\"")
            return
        }
        val params = parseParams(rest)
        if (params == null) {
            val repr = line.repr()
            logWarning("Illegal command parameters from server: \"$repr\"")
            return
        }
        val message = ParsedMessage(timestamp, prefix, cmd, params)
        actOnCommand(line, message)
    }

    private fun actOnCommand(
        line: String,
        message: ParsedMessage,
    ) {
        val done = when (message.cmd) {
            "001" -> rplWelcome001(message)
            "301" -> rplAway301(message)
            "372" -> rplMotd372(message)
            "366", "376" -> true // RPL_ENDOFNAMES, RPL_ENDOFMOTD
            "401" -> rplNoSuchNick401(message)
            "JOIN" -> joinIndication(message)
            //"MODE" -> tbd("MODE")
            "NICK" -> nick(message)
            //"NOTICE" -> tbd("NOTICE")
            //"PART" -> tbd("PART")
            "PING" -> pingIndication(message)
            "PRIVMSG" -> privMsgIndication(message)
            else -> false
        }
        if (!done)
            displayOnConsole(message.timestamp, line, Mood.LOG)
    }

    private fun tbd(s: String): Boolean {
        logWarning("TBD: $s")
        return false
    }

    private fun displayOnConsole(
        timestamp: Instant,
        line: String,
        mood: Mood,
    ) {
        consoleLogBuffer.add(
            LogLine(
                timestamp = timestamp,
                from = null,
                IRCString(line),
                mood,
            ),
        )
        consoleContents.value = consoleLogBuffer.getAll()
        consoleStatus.setTotal(consoleCount.addAndGet(1L))
    }

    private fun consoleInfo(timestamp: Instant, params: List<String>) {
        displayOnConsole(
            timestamp,
            params.joinToString(" "),
            Mood.INFO,
        )
    }

    private fun rplWelcome001(message: ParsedMessage): Boolean {
        if (message.params.isEmpty())
            return false
        resetNick(message.params[0])
        consoleInfo(message.timestamp, message.subparams(1))
        return true
    }

    private fun resetNick(newNick: String) {
        updateConfiguration(
            configuration.value!!.copy(nick = newNick)
        )
    }

    private fun rplAway301(message: ParsedMessage) =
        simpleChatError(message, "away", Mood.INFO)

    private fun rplMotd372(message: ParsedMessage): Boolean {
        if (message.params.isEmpty())
            return false
        consoleInfo(message.timestamp, message.subparams(1))
        return true
    }

    private fun rplNoSuchNick401(message: ParsedMessage) =
        simpleChatError(message, "not known", Mood.ERROR)

    private fun simpleChatError(
        message: ParsedMessage,
        trouble: String,
        mood: Mood,
    ): Boolean {
        if (message.params.size != 3)
            return false
        val (_, name, explanation) = message.params
        if (!validNick(name))
            return false
        val chat = chats[name.toIRCLower()] ?: return false
        chat.indicateMessage(
            from = null,
            text = "$name $trouble: $explanation",
            mood = mood,
        )
        return true
    }

    private fun nick(message: ParsedMessage): Boolean {
        if (message.params.size != 1 || message.prefix == null)
            return false
        val newNick = message.params[0]
        val parts = parsePrefixParts(message.prefix) ?: return false
        if (parts.nick != newNick)
            return false
        resetNick(newNick)
        return true
    }

    private fun joinIndication(message: ParsedMessage): Boolean {
        if (message.params.size != 1 || message.prefix == null)
            return false
        val parts = parsePrefixParts(message.prefix) ?: return false
        if (parts.nick == null || parts.nick == configuration.value!!.nick)
            return false
        val receivers = message.params[0]
        val address =
            if (parts.server == null) "${parts.nick}"
            else {
                val user = parts.user ?: parts.nick
                "${parts.nick} ($user@${parts.server})"
            }
        distribute(receivers) { chatName ->
            val info = "$address joined $chatName"
            val chatKey = chatName.toIRCLower()
            val chat = chats[chatKey]
            if (chat == null)
                displayOnConsole(message.timestamp, info, Mood.LOG)
            else {
                chat.indicateMessage(
                    from = null,
                    text = info,
                    mood = Mood.LOG,
                )
                chat.nicksPresent += chatKey
            }
        }
        return true
    }

    private fun pingIndication(message: ParsedMessage): Boolean {
        return when (message.params.size) {
            1 -> {
                val s1 = message.params[0]
                command("PONG :$s1")
                true
            }

            2 -> {
                val (s1, s2) = message.params
                command("PONG $s1 :$s2")
                true
            }

            else -> false
        }
    }

    private fun privMsgIndication(message: ParsedMessage): Boolean {
        if (message.params.size != 2 || message.prefix == null)
            return false
        val parts = parsePrefixParts(message.prefix)
        if (parts == null || parts.server != null)
            return false
        val (receivers, text) = message.params
        if (text.isNotEmpty() && text[0] == CtrlA)
            return doCTCP(message, text)
        distribute(receivers) {
            post(parts, it, text)
        }
        return true
    }

    private fun doCTCP(message: ParsedMessage, text: String): Boolean {
        return tbd("CTCP")
    }

    private fun post(parts: PrefixParts, receiver: String, text: String) {
        val limit = 50
        if (receiver.isEmpty()) {
            logWarning("Ignore empty receiver")
            return
        }
        val sender = parts.nick!!
        val chatName =
            if (validNick(receiver)) {
                if (receiver != configuration.value!!.nick)
                    return // not for me
                sender
            } else
                receiver

        val chat = join(chatName, limit)
        if (chat == null) {
            displayOnConsole(Instant.now(), "Too many chats", Mood.ERROR)
            return
        }
        chat.indicateMessage(sender, text, Mood.THEIRS)
    }

    private fun distribute(receivers: String, func: (String) -> Unit) =
        receivers.split(",").forEach(func)

    inner class Chat(
        val name: String,
        val key: String,
    ) {
        val contents = MutableLiveData(arrayOf<ProcessedLine>())
        val status = ChatStatus(name)
        private val messages = Channel<LogLine>(100)
        val nicksPresent = mutableSetOf<String>()

        override fun toString() = "Chat(\"$name\", \"$key\")"

        init {
            viewModelScope.launchGuarded {
                val buffer = journal.replay(name)
                if (!validNick(name))
                    command("JOIN $name\r\n")
                var count = 0L

                fun process(logLine: LogLine) {
                    buffer.add(logLine)
                    journal.logMessage(name, logLine)
                    count++
                }

                while (true) {
                    while (true)
                        process(messages.tryReceive().getOrNull() ?: break)
                    contents.value = buffer.getAll()
                    status.setTotal(count)
                    process(messages.receive())
                }
            }
        }

        fun sendPrivMsg(text: String) {
            check(state.value == AppState.CHAT)
            val archived = text // markupToArchive(text)
            indicateMessage(
                from = configuration.value!!.nick,
                text = archived,
                mood = Mood.MINE,
            )
            command("PRIVMSG $name :$text")
        }

        fun leave() {
            check(state.value == AppState.CHAT)
            status.look()
            state.value = AppState.JOIN
        }

        fun toggleAutojoin() {
            check(state.value == AppState.CHAT)
            if (amongAutojoins())
                removeFromAutojoins()
            else
                addToAutojoins()
        }

        fun amongAutojoins() = configuration.value!!.amongAutojoins(key)

        fun addToAutojoins() {
            if (!amongAutojoins()) {
                val currentConfiguration = configuration.value!!
                val newAutojoins = currentConfiguration.autojoins + name
                updateConfiguration(
                    currentConfiguration.copy(autojoins = newAutojoins),
                )
            }
        }

        fun removeFromAutojoins() {
            val currentConfiguration = configuration.value!!
            val newAutojoins =
                currentConfiguration.autojoins.filter { it.toIRCLower() != key }
            updateConfiguration(
                currentConfiguration.copy(autojoins = newAutojoins),
            )
        }

        fun clear() {
            check(state.value == AppState.CHAT)
            state.value = AppState.CLEARING
            val expungeJob = viewModelScope.launchGuarded(Dispatchers.IO) {
                journal.expunge(key)
            }
            viewModelScope.launchGuarded {
                expungeJob.join()
                chats[key] = Chat(name, key)
                chatInfo.value = generateChatInfo()
                state.value = AppState.CHAT
            }
        }

        fun delete() {
            check(state.value == AppState.CHAT)
            state.value = AppState.DELETING
            if (validChannelName(name))
                command("PART $name")
            val expungeJob = viewModelScope.launchGuarded(Dispatchers.IO) {
                journal.expunge(key)
            }
            viewModelScope.launchGuarded {
                expungeJob.join()
                removeFromAutojoins()
                chats.remove(key)
                chatInfo.value = generateChatInfo()
                state.value = AppState.JOIN
            }
        }

        fun part() {
            check(state.value == AppState.CHAT)
            if (validChannelName(name))
                command("PART $name")
            removeFromAutojoins()
            chats.remove(key)
            chatInfo.value = generateChatInfo()
            state.value = AppState.JOIN
        }

        private fun highlight(s: String) = highlightURLs(highlightNicks(s))

        private fun highlightNicks(s: String): String {
            val points = mutableListOf<Int>()
            val lcase = s.toIRCLower()
            var cursor = 0
            while (cursor < lcase.length) {
                val skipped = skipNick(lcase, cursor)
                if (skipped != null) {
                    points.add(cursor)
                    points.add(skipped)
                    cursor = skipped
                    continue
                }
                cursor++
                while (cursor < lcase.length && lcase[cursor].nickBreak())
                    cursor++
            }
            return wedge(s, IRCBold, points)
        }

        private fun highlightURLs(s: String): String {
            val points = mutableListOf<Int>()
            var cursor = 0
            while (true) {
                val range = findUrl(s, cursor) ?: break
                val (start, end) = range
                points.add(start)
                points.add(end)
                cursor = end
            }
            return wedge(s, IRCUnderline, points)
        }

        private fun skipNick(lcase: String, index: Int): Int? {
            val tailLength = lcase.length - index
            nicksPresent.filter {
                it.length >= tailLength &&
                        lcase.substring(index, index + it.length) == it
            }.forEach {
                val skipped = index + it.length
                if (it.length == tailLength || lcase[skipped].nickBreak())
                    return skipped
            }
            return null
        }

        fun indicateMessage(from: String?, text: String, mood: Mood) {
            val timestamp = Instant.now()
            val highlighted = highlight(text)
            val logLine = LogLine(
                timestamp = timestamp,
                from = from,
                line = IRCString(text),
                mood = mood,
            )
            messages.trySend(logLine)
        }
    }
}

fun wedge(s: String, joiner: String, points: Iterable<Int>): String {
    var prev = 0
    val snippets = mutableListOf<String>()
    for (point in points) {
        snippets.add(s.substring(prev, point))
        prev = point
    }
    snippets.add(s.substring(prev))
    return snippets.joinToString(joiner)
}

private fun Char.nickBreak() =
    when (this.category) {
        CharCategory.LOWERCASE_LETTER,
        CharCategory.MODIFIER_LETTER,
        CharCategory.OTHER_LETTER,
        CharCategory.TITLECASE_LETTER,
        CharCategory.UPPERCASE_LETTER,
        CharCategory.DECIMAL_DIGIT_NUMBER,
        CharCategory.LETTER_NUMBER,
        CharCategory.OTHER_NUMBER -> false

        else -> true
    }

private fun Char.toIRCLower() =
    when (this) {
        '[' -> '{'
        ']' -> '}'
        '\\' -> '|'
        '~' -> '^'
        else -> if (this in 'A'..'Z')
            this + ('a' - 'A')
        else this
    }

fun String.toIRCLower() =
    map { it.toIRCLower() }.toCharArray().concatToString()

private suspend fun Connection.lineReader(
    bridge: SendChannel<String>,
) {
    try {
        stringifyChunks(chunksToLines(readFlow(this::read))).collect {
            bridge.send(it.first)
        }
        close()
    } catch (e: Throwable) {
        logError("Connection broken", e)
    } finally {
        bridge.close()
    }
}
private suspend fun Connection.transmitLine(line: String) =
    transmit("$line\r\n")

private suspend fun Connection.transmit(snippet: String) {
    val success = writeAll(ByteBuffer.wrap(snippet.toByteArray())) {
        write(it)
    }
    if (!success)
        throw IOException("writeAll failure")
}

fun CoroutineScope.launchGuarded(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
) =
    this.launch(context, start) {
        try {
            block()
        } catch (e: Throwable) {
            logError("Uncaught exception", e)
            throw (e)
        }
    }

fun validNick(nick: String): Boolean {
    if (nick.isEmpty())
        return false
    when (nick[0]) {
        '\u0000', '$', ':', '#', '&' -> return false
    }
    for (c in nick)
        when (c) {
            ' ', ',', '*', '?', '!', '@', '.' -> return false
            else -> if (c < ' ')
                return false
        }
    return true
}

fun validChannelName(name: String): Boolean {
    if (name.length < 2 || name.length > 50)
        return false
    when (name[0]) {
        '&', '#', '+', '!' -> {}
        else -> return false
    }
    for (c in name)
        when (c) {
            '\u0000', ' ', '\u0007', ',', '\r', '\n' -> return false
        }
    return true
}

private data class ParsedMessage(
    val timestamp: Instant,
    val prefix: String?,
    val cmd: String,
    val params: List<String>,
) {
    fun subparams(startingFrom: Int) =
        params.subList(startingFrom, params.size)
}

private fun String.repr() =
    map {
        when (it) {
            '"' -> "\\\""
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            '\\' -> "\\\\"
            else ->
                if (it in ' '..'~')
                    it
                else
                    "\\u%04x".format(it)
        }
    }.joinToString("")

private fun parsePrefix(line: String) =
    if (line.isEmpty() || line[0] != ':')
        null to line
    else
        splitOff(line.substring(1))

private fun splitOff(s: String): Pair<String, String> {
    var i = s.indexOf(' ')
    if (i < 0)
        return s to ""
    val prefix = s.substring(0, i)
    while (i < s.length && s[i] == ' ')
        i++
    return prefix to s.substring(i)
}

private fun parseCommand(s: String): Pair<String?, String> {
    var cursor = 0
    when {
        s.isEmpty() -> return null to s

        s[cursor].isAsciiDigit() -> {
            cursor++
            if (s.length < 3 ||
                !s[cursor++].isAsciiDigit() ||
                !s[cursor++].isAsciiDigit())
                return null to s
        }

        s[cursor].isAsciiAlpha() -> {
            cursor++
            while (s[cursor].isAsciiAlpha())
                cursor++
        }

        else -> return null to s
    }
    val cmd = s.substring(0, cursor)
    if (s.length == cursor)
        return cmd to ""
    if (s[cursor++] != ' ')
        return null to s
    while (cursor < s.length && s[cursor] == ' ')
        cursor++
    return cmd to s.substring(cursor)
}

private fun parseParams(s: String): List<String>? {
    var rest = s
    val params = arrayListOf<String>()
    while (rest.isNotEmpty() && rest[0] != ':' && rest[0] != ' ') {
        val (param, tail) = splitOff(rest)
        params.add(param)
        rest = tail
    }
    when {
        rest.isEmpty() -> {}
        rest[0] == ':' -> params.add(rest.substring(1))
        else -> return null
    }
    return params
}

private data class PrefixParts(
    val server: String? = null,
    val nick: String? = null,
    val user: String? = null,
    val host: String? = null,
)

private fun parsePrefixParts(prefix: String): PrefixParts? {
    for ((cursor, c) in prefix.withIndex())
        when (c) {
            '!' -> {
                val nick = prefix.substring(0, cursor)
                val atLoc = prefix.indexOf('@', cursor + 1)
                if (atLoc < 0 || !validNick(nick))
                    return null
                return PrefixParts(
                    nick = nick,
                    user = prefix.substring(cursor + 1, atLoc),
                    host = prefix.substring(atLoc + 1)
                )
            }
            '@' -> {
                val nick = prefix.substring(0, cursor)
                if (!validNick(nick))
                    return null
                return PrefixParts(
                    nick = nick,
                    host = prefix.substring(cursor + 1)
                )
            }
        }
    if (validNick(prefix))
        return PrefixParts(nick = prefix)
    return PrefixParts(server = prefix)
}

enum class AppState {
    CONFIGURING, JOIN, CONSOLE, CHAT, CLEARING, DELETING,
}


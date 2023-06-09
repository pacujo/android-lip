package net.pacujo.lip

import android.util.JsonReader
import android.util.JsonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.io.Writer
import java.time.DateTimeException
import java.time.Instant
import java.util.LinkedList
import java.util.Queue
import java.util.SortedMap
import kotlin.math.min

private typealias FileSize = Long

class Journal(
    private val journalDir: File,
    private val terminator: Char,
) {
    private val rotateThreshold = 100_000L
    private val deleteThreshold = 1_000_000L

    private val mutex = Mutex()
    private val rotatedFiles: SortedMap<String, FileSize>
    private var rotatedLength: FileSize
    private val journalFile: File
    private var outputWriter: Writer
    private var tentativeLength: FileSize

    init {
        if (!journalDir.exists())
            journalDir.mkdir()
        rotatedFiles =
            journalDir.list { _, s -> rotatedPattern.matches(s) }!!
                .associateWith {
                    File(journalDir, it).length()
                }.toSortedMap()
        rotatedLength = rotatedFiles.values.sum()
        journalFile = File(journalDir, journalFileName)
        outputWriter =
            BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(journalFile, true),
                ),
            )
        tentativeLength = journalFile.length()
    }

    /* Append a full string record atomically to the journal using the Writer
     * interface. */
    suspend fun output(block: (Writer) -> Unit) {
        val stringWriter = StringWriter()
        stringWriter.use(block)
        val str = stringWriter.toString()
        require(!str.contains(terminator))
        mutex.withLock {
            outputWriter.write(str)
            outputWriter.write(terminator.code)
            outputWriter.flush()
            tentativeLength += str.length + 1
            if (tentativeLength >= rotateThreshold)
                rotate()
        }
    }

    /* Produce a flow of all string records starting from the oldest one
     * available. If new records are added concurrently, they may not be
     * included in the flow. However, possible journal rotations do not
     * create gaps in the flow. */
    suspend fun input() = flow {
        val queue = openJournalFiles()
        try {
            while (queue.isNotEmpty())
                queue.remove().use { inputStream ->
                    emitRecords(this, inputStream)
                }
        } finally {
            queue.forEach { it.close() }
        }
    }

    suspend fun editRecords(predicate: (String) -> Boolean) {
        suspend fun edit(fileName: String): FileSize {
            val origFile = File(journalDir, fileName)
            val tempFile = File(journalDir, "temp.log")
            dataInputStream(origFile).use { inputStream ->
                BufferedWriter(
                    OutputStreamWriter(
                        FileOutputStream(tempFile),
                    ),
                ).use { tempWriter ->
                    recordFlow(inputStream).collect { (record, terminated) ->
                        if (!terminated)
                            tempWriter.write(record)
                        else if (predicate(record)) {
                            tempWriter.write(record)
                            tempWriter.write(terminator.code)
                        }
                    }
                }
            }
            origFile.delete()
            tempFile.renameTo(origFile)
            return origFile.length()
        }

        rotatedLength = 0L
        mutex.withLock {
            for (fileName in rotatedFiles.keys) {
                val length = edit(fileName)
                rotatedFiles[fileName] = length
                rotatedLength += length
            }
            tentativeLength = edit(journalFileName)
            outputWriter.close()
            outputWriter =
                BufferedWriter(
                    OutputStreamWriter(
                        FileOutputStream(journalFile, true),
                    ),
                )
        }
    }

    private suspend fun openJournalFiles(): Queue<DataInputStream> {
        val queue = LinkedList<DataInputStream>()
        mutex.withLock {
            flow {
                for (fileName in rotatedFiles.keys)
                    emit(dataInputStream(File(journalDir, fileName)))
                emit(dataInputStream(File(journalDir, journalFileName)))
            }
        }.collect {
            queue.add(it)
        }
        return queue
    }

    private fun dataInputStream(file: File) =
        DataInputStream(BufferedInputStream(FileInputStream(file)))

    private suspend fun emitRecords(
        flowCollector: FlowCollector<String>,
        inputStream: DataInputStream,
    ) {
        recordFlow(inputStream).filter { it.second }.collect {
            flowCollector.emit(it.first)
        }
    }

    private fun recordFlow(
        inputStream: DataInputStream,
    ): Flow<Pair<String, Boolean>> {
        val chunkFlow = readFlow { buf ->
            val chunk = ByteArray(size = min(10_000, buf.remaining()))
            inputStream.read(chunk).also { count ->
                if (count > 0)
                    buf.put(chunk, 0, count)
            }
        }
        val lineFlow = chunksToRecords(chunkFlow, terminator.code.toByte())
        return stringifyChunks(lineFlow)
    }

    private fun rotate() {
        val actualLength = journalFile.length()
        rotatedLength += actualLength
        val newFileName = instantFileName(Instant.now())
        rotatedFiles[newFileName] = actualLength
        journalFile.renameTo(File(journalDir, newFileName))
        outputWriter.close()
        outputWriter = journalFile.writer()
        tentativeLength = 0L
        while (true) {
            val oldestKey = rotatedFiles.firstKey()
            val oldestLength = rotatedFiles[oldestKey]!!
            if (rotatedLength - oldestLength < deleteThreshold)
                break
            File(journalDir, oldestKey).delete()
            rotatedLength -= oldestLength
        }
    }

    companion object {
        private const val journalFileName = "journal.log"
        private const val DD = "\\d\\d"
        private val rotatedPattern =
            Regex("j$DD$DD-$DD-${DD}T$DD:$DD:$DD[.]$DD$DD${DD}Z[.]log")

        private fun instantFileName(instant: Instant) =
            "j$instant.log"
    }
}

class LipJournal(journalDir: File) {
    private val journal = Journal(journalDir, '\u0000')
    private val channel = Channel<Pair<String, LogLine>>(100)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val (chatName, logLine) = channel.receive()
                output(chatName, logLine)
            }
        }
    }

    fun logMessage(chatName: String, logLine: LogLine) {
        channel.trySend(chatName to logLine)
    }

    private suspend fun output(chatKey: String, logLine: LogLine) {
        journal.output { writer ->
            JsonWriter(writer).use { jw ->
                jw.emitObject {
                    with(logLine) {
                        jw.name("version").value(1)
                            .name("chat").value(chatKey)
                            .name("timestamp").value(timestamp.toEpochMilli())
                            .name("line").value(line.string)
                            .name("mood").value(mood.toString())
                        if (from != null)
                            jw.name("from").value(from)
                    }
                }
            }
        }
    }

    suspend fun replay(chatKey: String) =
        LogBuffer().apply {
            journal.input()
                .map { parse(JsonReader(it.reader())) }
                .filter { it.first == chatKey }
                .collect { add(it.second) }
        }

    suspend fun expunge(chatKey: String) {
        journal.editRecords { record ->
            chatKey != parse(JsonReader(record.reader())).first
        }
    }

    companion object {
        private val entrySchema = JsonSchema(
            "version" to J.int,
            "chat" to J.string,
            "from" to J.string,
            "timestamp" to J.long,
            "line" to J.string,
            "mood" to J.string,
        )

        private fun parse(jr: JsonReader): Pair<String, LogLine> {
            val obj = jr.parseObject(entrySchema)
            val version = obj["version"] as Int?
            if (version != 1)
                throw JsonSchemaException("Unknown journal entry format")
            val chatKey = obj["chat"] as String?
                ?: throw JsonSchemaException("Chat key missing")
            val from = obj["from"] as String?
            val epochMilli = obj["timestamp"] as Long?
                ?: throw JsonSchemaException("Timestamp missing")
            val timestamp = try {
                Instant.ofEpochMilli(epochMilli)
            } catch (e: DateTimeException) {
                throw JsonSchemaException(cause = e)
            }
            val line = obj["line"] as String?
                ?: throw JsonSchemaException("Line missing")
            val moodName = obj["mood"] as String?
                ?: throw JsonSchemaException("Mood missing")
            val mood = try {
                Mood.valueOf(moodName)
            } catch (e: IllegalArgumentException) {
                throw JsonSchemaException(cause = e)
            }
            return chatKey to LogLine(
                timestamp = timestamp,
                from = from,
                line = IRCString(line),
                mood = mood,
            )
        }
    }
}

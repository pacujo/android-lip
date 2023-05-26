package net.pacujo.lip

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.text.AnnotatedString
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

typealias ProcessedLine = Pair<LogLine?, AnnotatedString>

class LogBuffer(val capacity: Int = 1000) {
    private val dates = sortedMapOf<String, MutableList<ProcessedLine>>()

    val size get() =
        dates.size + dates.values.fold(0) { sum, arr ->
            sum + arr.size
        }

    private var _totalCount = 0L
    val totalCount get() = _totalCount

    operator fun get(index: Int): ProcessedLine {
        require(index >= 0)
        var cursor = 0
        for ((key, value) in dates.entries) {
            if (index < cursor + value.size + 1) {
                if (index > cursor)
                    return value[index - cursor - 1]
                val markedUpDate =
                    IRCString(key).toAnnotatedString().markMood(Mood.LOG)
                return null to markedUpDate
            }
            cursor += value.size + 1
        }
        throw IndexOutOfBoundsException()
    }

    fun add(logLine: LogLine) {
        _totalCount++
        val localStamp = logLine.timestamp.atZone(ZoneId.systemDefault())
        val localDate = localStamp.toLocalDate()
        val localTime = localStamp.toLocalTime()
        val key = "($localDate)"
        val todPrefix =
            AnnotatedString("[${todFormatter.format(localTime)}] ")
                .markMood(Mood.LOG)
        val author =
            AnnotatedString(logLine.from?.plus(">") ?: "")
                .markMood(Mood.INFO)
        val payload =
            logLine.line
                .toAnnotatedString()
                .markMood(logLine.mood)
        val displayLine = todPrefix + author + payload
        val value = logLine to displayLine
        if (!dates.contains(key))
            dates[key] = arrayListOf()
        dates[key]?.add(value)
        while (size >= capacity) {
            val firstKey = dates.firstKey()
            checkNotNull(firstKey)
            val firstValue = dates[dates.firstKey()]
            checkNotNull(firstValue)
            if (firstValue.size > 1) {
                dates[firstKey] =
                    firstValue.subList(1, firstValue.size)
            } else {
                check(firstValue.size == 1)
                dates.remove(firstKey)
            }
        }
    }

    fun getAll(): Array<ProcessedLine> =
        (0 until size).map { get(it) }.toTypedArray()

    companion object {
        private val todFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm")
    }
}

fun Iterable<LogLine>.toLogBuffer(): LogBuffer {
    val buffer = LogBuffer()
    for (line in this)
        buffer.add(line)
    return buffer
}

enum class Mood { LOG, INFO, ERROR, MINE, THEIRS }

data class LogLine(
    val timestamp: Instant,
    val from: String?,
    val line: IRCString,
    val mood: Mood,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val version = 1
        parcel.writeInt(version)
        parcel.writeLong(timestamp.toEpochMilli())
        parcel.writeString(from)
        parcel.writeString(line.string)
        parcel.writeString(mood.name)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<LogLine> {
        override fun createFromParcel(parcel: Parcel): LogLine {
            val version = parcel.readInt()
            check(version == 1)
            return LogLine(
                timestamp = Instant.ofEpochMilli(parcel.readLong()),
                from = parcel.readString(),
                line = IRCString(parcel.readString() ?: ""),
                mood = Mood.valueOf(value = parcel.readString() ?: ""),
            )
        }

        override fun newArray(size: Int): Array<LogLine?> {
            return arrayOfNulls(size)
        }
    }
}

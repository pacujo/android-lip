@file:Suppress("unused")

package net.pacujo.lip

import android.os.Parcel
import android.os.Parcelable
import android.util.JsonReader
import android.util.JsonWriter

data class Configuration(
    val nick: String,
    val name: String,
    val serverHost: String,
    val port: Int,
    val useTls: Boolean,
    val autojoins: List<String>,
) : Parcelable {
    fun amongAutojoins(chatKey: String) =
        autojoins.find { chatName -> chatName.toIRCLower() == chatKey } != null

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val version = 2
        parcel.writeInt(version)
        parcel.writeString(nick)
        parcel.writeString(name)
        parcel.writeString(serverHost)
        parcel.writeInt(port)
        parcel.writeInt(if (useTls) 1 else 0)
        parcel.writeInt(autojoins.size)
        for (channelName in autojoins)
            parcel.writeString(channelName)
    }

    override fun describeContents() = 0

    fun emit(jw: JsonWriter) {
        jw.emitObject {
            jw.name("version").value(1)
                .name("nick").value(nick)
                .name("name").value(name)
                .name("server-host").value(serverHost)
                .name("port").value(port)
                .name("use-tls").value(useTls)
                .name("autojoins").emitArray {
                    for (chatName in autojoins)
                        jw.value(chatName)
                }
        }
    }

    companion object CREATOR : Parcelable.Creator<Configuration> {
        fun default(): Configuration = Configuration(
            nick = "",
            name = "",
            serverHost = "irc.oftc.net",
            port = 6697,
            useTls = true,
            autojoins = listOf(),
        )

        override fun createFromParcel(parcel: Parcel): Configuration {
            val version = parcel.readInt()
            check(version == 1 || version == 2)
            val nick = parcel.readString() ?: ""
            val name = parcel.readString() ?: ""
            val serverHost = parcel.readString() ?: ""
            val port = parcel.readInt()
            val useTls = parcel.readInt() != 0
            val autojoins = mutableListOf<String>()
            repeat(parcel.readInt()) {
                if (version == 1)
                    parcel.readString() // skip
                autojoins.add(parcel.readString() ?: "")
            }
            return Configuration(
                nick = nick,
                name = name,
                serverHost = serverHost,
                port = port,
                useTls = useTls,
                autojoins = autojoins,
            )
        }

        override fun newArray(size: Int): Array<Configuration?> {
            return arrayOfNulls(size)
        }

        private val configurationSchema = JsonSchema(
            "version" to J.int,
            "nick" to J.string,
            "name" to J.string,
            "server-host" to J.string,
            "port" to J.int,
            "use-tls" to J.boolean,
            "autojoins" to J.array(J.string),
        )

        fun parse(jr: JsonReader): Configuration {
            val obj = jr.parseObject(configurationSchema)
            val version = obj["version"] as Int?
            if (version != 1)
                throw JsonSchemaException("Unknown configuration format")
            val nick = obj["nick"] as String?
            if (nick == null || !validNick(nick))
                throw JsonSchemaException("Invalid or missing nick")
            val name = obj["name"] as String?
            if (name.isNullOrEmpty())
                throw JsonSchemaException("Invalid or missing name")
            val serverHost = obj["server-host"] as String?
            if (serverHost.isNullOrEmpty())
                throw JsonSchemaException("Invalid or missing server")
            val port = obj["port"] as Int?
            if (port == null || port !in 1..65535)
                throw JsonSchemaException("Invalid or missing port")
            val useTls = obj["use-tls"] as Boolean?
                ?: throw JsonSchemaException("TLS selection missing")
            val autojoins = obj["autojoins"] as JsonArray?
                ?: throw JsonSchemaException("Autojoins missing")
            return Configuration(
                nick = nick,
                name = name,
                serverHost = serverHost,
                port = port,
                useTls = useTls,
                autojoins = autojoins.map { it as String }.toList(),
            )
        }

        val jsonParser = JsonReader::parseConfiguration
    }
}

fun JsonReader.parseConfiguration(
    @Suppress("UNUSED_PARAMETER")
    depth: Int = 0,
): JsonValue =
    Configuration.parse(this)

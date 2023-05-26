package net.pacujo.lip

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var model: LipModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            model = ViewModelProvider(this)[LipModel::class.java]
            model.initialize(applicationContext.filesDir)
        } catch (e: Exception) {
            logError("Failed to initialize", e)
            throw(e)
        }
        setContent {
            MaterialTheme {
                SetBarColors()
                Application(model)
            }
        }
    }

}

@Composable
private fun SetBarColors() {
    val view = LocalView.current
    val argb = MaterialTheme.colorScheme.primary.toArgb()
    if (!view.isInEditMode)
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = argb
            window.navigationBarColor = argb
        }
}

@Composable
fun Application(model: LipModel) {
    val state = model.state.observeAsState()

    when (state.value) {
        AppState.CONFIGURING ->
            ConfigForm(
                configuration = model.configuration,
                onSubmit = model::configure,
            )

        AppState.JOIN ->
            Join(
                consoleTotal = model.consoleTotal,
                consoleSeen = model.consoleSeen,
                chatInfo = model.chatInfo,
                onConsole = model::showConsole,
                onJoin = { model.doJoin(it) },
            )

        AppState.CONSOLE ->
            Console(
                contents = model.consoleContents,
                back = model::leaveConsole,
            )

        AppState.CHAT -> {
            val chatKey = model.currentChatKey.value!!
            val chat = model.chats[chatKey]!!
            ChatView(
                configuration = model.configuration,
                chatName = model.currentChatKey,
                contents = chat.contents,
                onSend = model::sendPrivMsg,
                toggleAutojoin = chat::toggleAutojoin,
                back = model::leaveChat,
            )
        }

        null -> check(false)
    }
}

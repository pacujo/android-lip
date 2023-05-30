@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

@Composable
fun Deleting(
    configuration: LiveData<Configuration>,
    chatName: String,
    chatStatus: LiveData<List<ChatStatus>>,
) {
    val chatKey = chatName.toIRCLower()
    val obsConfiguration = configuration.observed()

    val otherChats =
        chatStatus.observed().filter { it.name.toIRCLower() != chatKey }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    title = chatName,
                    nick = obsConfiguration.nick,
                    otherChatStatus = otherChats,
                    back = {},
                )
                     },
        ) { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Waiting()
            }
        }
    }
}

@Preview
@Composable
fun DeletingPreview() {
    Deleting(
        configuration = MutableLiveData(
            Configuration.default().copy(nick = "testudo"),
        ),
        chatName = "#hottub",
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
    )
}

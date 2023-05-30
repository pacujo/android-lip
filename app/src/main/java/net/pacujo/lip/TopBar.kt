@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import kotlin.math.sign


@Composable
fun TopBar(
    title: String,
    nick: String,
    favorite: Boolean? = null,
    toggleFavorite: () -> Unit = {},
    onPart: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    otherChatStatus: List<ChatStatus>,
    back: () -> Unit,
) {
    val unseen = otherChatStatus.map { it.observedUnseen().sign }.sum()
    val backBadgeObject = if (unseen > 0) unseen else null

    BackHandler(onBack = back)
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = back) {
                BadgedBackArrow(badgeObject = backBadgeObject)
            }
        },
        actions = {
            FavoriteAction(
                nick = nick,
                chat = title,
                favorite = favorite,
                toggleFavorite = toggleFavorite,
                onPart = onPart,
                onClearChat = onClearChat,
                onDeleteChat = onDeleteChat,
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
fun FavoriteAction(
    nick: String,
    chat: String? = null,
    favorite: Boolean? = null,
    toggleFavorite: () -> Unit = {},
    onPart: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
) {
    val (expanded, setExpanded) =
        remember { mutableStateOf(false) }
    val (confirmClear, setConfirmClear) =
        remember { mutableStateOf(false) }
    val (confirmDelete, setConfirmDelete) =
        remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = nick,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Row {
            IconButton(onClick = toggleFavorite) {
                Icon(
                    imageVector = favoriteIcon(favorite),
                    contentDescription = favoriteDescription(favorite),
                    tint = favoriteTint(favorite),
                )
            }
            if (chat != null && favorite != null) {
                IconButton(onClick = { setExpanded(true) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = favoriteTint(favorite),
                    )
                }
                when {
                    confirmClear -> ConfirmDialog(
                        chat, onClearChat, setConfirmClear,
                        Icons.Default.Clear,
                        "Clear Chat History?", "Clear",
                    )

                    confirmDelete -> ConfirmDialog(
                        chat, onDeleteChat, setConfirmDelete,
                        Icons.Default.Delete,
                        "Delete Chat?", "Delete",
                    )

                    /* Do not compose an alert dialog simultanously with a
                     * dropdown menu! It messes up the UI. */
                    else -> Menu(
                        onPart, expanded, setExpanded,
                        setConfirmClear, setConfirmDelete,
                    )
                }
            }
        }
    }
}


@Composable
fun ConfirmDialog(
    chat: String,
    onConfirmation: () -> Unit,
    setConfirm: (Boolean) -> Unit,
    imageVector: ImageVector,
    title: String,
    affirm: String,
) {
    AlertDialog(
        icon = { Icon(imageVector = imageVector, contentDescription = null) },
        title = { Text(text = title) },
        text = { Text(text = "Chat: $chat") },
        onDismissRequest = { setConfirm(false) },
        confirmButton = {
            Button(
                onClick = {
                    setConfirm(false)
                    onConfirmation()
                          },
            ) {
                Text(text = affirm)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { setConfirm(false) },
            ) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
fun Menu(
    onPart: () -> Unit,
    expanded: Boolean, setExpanded: (Boolean) -> Unit,
    setConfirmClear: (Boolean) -> Unit,
    setConfirmDelete: (Boolean) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { setExpanded(false) },
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Part from chat",
                )
            },
            text = { Text(text = "Part from chat") },
            onClick = {
                setExpanded(false)
                onPart()
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear chat history",
                )
            },
            text = { Text(text = "Clear chat history") },
            onClick = {
                setExpanded(false)
                setConfirmClear(true)
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete chat",
                )
            },
            text = { Text(text = "Delete chat") },
            onClick = {
                setExpanded(false)
                setConfirmDelete(true)
            },
        )
    }
}

@Composable
fun BadgedBackArrow(badgeObject: Any?) {
    if (badgeObject == null)
        BackArrow()
    else
        BadgedBox(
            badge = {
                Badge {
                    Text(badgeObject.toString())
                }
            },
        ) {
            BackArrow()
        }
}


@Composable
fun BackArrow() {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = "Back",
        tint = MaterialTheme.colorScheme.onPrimary,
    )
}

private fun favoriteIcon(favorite: Boolean?) =
    if (favorite == true)
        Icons.Default.Favorite
    else
        Icons.Default.FavoriteBorder

private fun favoriteDescription(favorite: Boolean?) =
    if (favorite == null)
        null
    else
        "Favorite"

@Composable
private fun favoriteTint(favorite: Boolean?) =
    if (favorite == null)
        MaterialTheme.colorScheme.primary // dummy: invisible on primary
    else
        MaterialTheme.colorScheme.onPrimary

@Preview
@Composable
fun TopBarPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    title = "#kapow",
                    nick = "testudo",
                    favorite = true,
                    toggleFavorite = {},
                    otherChatStatus = listOf(
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
                    ),
                    back = {},
                )
            },
        ) {
            Text(
                text = "sample",
                modifier = Modifier.padding(it),
            )
        }
    }
}
@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import kotlin.math.sign


@Composable
fun TopBar(
    title: String,
    nick: String,
    favorite: Boolean? = null,
    toggleFavorite: () -> Unit = {},
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
            FavoriteAction(nick, favorite, toggleFavorite)
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
    favorite: Boolean? = null,
    toggleFavorite: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = nick,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        IconButton(onClick = toggleFavorite) {
            Icon(
                imageVector = favoriteIcon(favorite),
                contentDescription = favoriteDescription(favorite),
                tint = favoriteTint(favorite),
            )
        }
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
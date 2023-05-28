@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    favorite: Boolean? = null,
    toggleFavorite: (() -> Unit)? = null,
    otherChatInfo: Map<String, ChatInfo>,
    back: () -> Unit,
) {
    val unseen =
        otherChatInfo.entries.map { it.value.observedUnseen().sign }.sum()
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
            if (favorite != null && toggleFavorite != null)
                IconButton(onClick = toggleFavorite) {
                    Icon(
                        imageVector = favoriteIcon(favorite),
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
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

private fun favoriteIcon(favorite: Boolean) =
    if (favorite)
        Icons.Default.Favorite
    else
        Icons.Default.FavoriteBorder

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
                    favorite = true,
                    toggleFavorite = {},
                    otherChatInfo = mapOf(
                        "pacujo" to ChatInfo(
                            name = "pacujo",
                            totalCount = MutableLiveData(20L),
                            seenCount = MutableLiveData(9L),
                        ),
                        "#testudo" to ChatInfo(
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
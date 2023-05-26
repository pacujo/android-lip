@file:OptIn(ExperimentalMaterial3Api::class)

package net.pacujo.lip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    chatName: String,
    favorite: Boolean? = null,
    toggleFavorite: (() -> Unit)? = null,
    back: () -> Unit,
) {
    BackHandler(onBack = back)
    TopAppBar(
        title = {
            Text(
                text = chatName,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = back) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
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
                    chatName = "#kapow",
                    favorite = true,
                    toggleFavorite = {},
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
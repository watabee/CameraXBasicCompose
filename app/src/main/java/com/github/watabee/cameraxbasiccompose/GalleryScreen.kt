package com.github.watabee.cameraxbasiccompose

import android.media.MediaScannerConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.watabee.cameraxbasiccompose.utils.filterJpegFiles
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import java.io.File

@OptIn(ExperimentalPagerApi::class)
@Composable
fun GalleryScreen(rootDirectory: File, navigateUp: () -> Unit) {
    val mediaList = remember(rootDirectory) {
        rootDirectory.filterJpegFiles()?.sortedDescending().orEmpty().toMutableStateList()
    }
    // If all photos have been deleted, return to camera
    if (mediaList.isEmpty()) {
        navigateUp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        val pagerState = rememberPagerState()

        var openDeleteDialog by remember { mutableStateOf(false) }
        if (openDeleteDialog) {
            val context = LocalContext.current
            DeleteDialog(
                deleteMediaFile = {
                    val mediaFile = mediaList[pagerState.currentPage]
                    // Delete current photo
                    mediaFile.delete()
                    // Send relevant broadcast to notify other apps of deletion
                    MediaScannerConnection.scanFile(context, arrayOf(mediaFile.absolutePath), null, null)
                    mediaList.removeAt(pagerState.currentPage)
                },
                onDismiss = { openDeleteDialog = false }
            )
        }

        HorizontalPager(
            state = pagerState,
            count = mediaList.size
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    modifier = Modifier.align(Alignment.Center),
                    model = mediaList[page],
                    contentScale = ContentScale.Fit,
                    contentDescription = null
                )
            }
        }

        IconButton(
            modifier = Modifier
                .padding(top = 32.dp, start = 16.dp),
            onClick = navigateUp
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back_button_alt)
            )
        }

        IconButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            onClick = { openDeleteDialog = true }
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(id = R.string.delete_button_alt)
            )
        }
    }
}

@Composable
private fun DeleteDialog(
    deleteMediaFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = null)
                Text(text = stringResource(id = R.string.delete_title))
            }
        },
        text = {
            Text(text = stringResource(id = R.string.delete_dialog))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    deleteMediaFile()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        onDismissRequest = onDismiss
    )
}

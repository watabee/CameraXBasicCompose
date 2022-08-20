package com.github.watabee.cameraxbasiccompose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        val photoFileController = rememberPhotoFileController()
        val cameraViewState = rememberCameraViewState(photoFileController)
        val coroutineScope = rememberCoroutineScope()
        // Flush animation.
        var alpha by remember { mutableStateOf(0f) }

        CameraView(
            modifier = Modifier.fillMaxSize(),
            cameraViewState = cameraViewState
        )

        if (cameraViewState.canSwitchCamera) {
            SwitchCameraButton(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 92.dp),
                onClick = cameraViewState::switchCamera
            )
        }

        if (cameraViewState.canTakePicture) {
            CaptureButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                onClick = {
                    cameraViewState.takePicture()
                    // Flush animation.
                    coroutineScope.launch {
                        delay(100L)
                        alpha = 1f
                        delay(50L)
                    }.invokeOnCompletion {
                        alpha = 0f
                    }
                }
            )
        }

        GalleryButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 92.dp, end = 32.dp),
            photoUri = photoFileController.lastSavedPhotoUri,
            onClick = {}
        )

        // Flush animation.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alpha)
                .background(color = Color.White)
        )
    }
}

@Composable
private fun SwitchCameraButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Image(
        modifier = modifier
            .clip(CircleShape)
            .size(64.dp)
            .clickable(onClick = onClick),
        painter = painterResource(id = R.drawable.ic_switch),
        contentDescription = stringResource(id = R.string.switch_camera_button_alt)
    )
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Image(
        modifier = modifier
            .clip(CircleShape)
            .size(92.dp)
            .clickable(onClick = onClick),
        painter = painterResource(id = R.drawable.ic_shutter_normal),
        contentDescription = stringResource(id = R.string.capture_button_alt)
    )
}

@Composable
private fun GalleryButton(
    modifier: Modifier = Modifier,
    photoUri: Uri?,
    onClick: () -> Unit
) {
    SubcomposeAsyncImage(
        modifier = modifier
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .size(64.dp)
            .clickable(onClick = onClick),
        model = photoUri,
        contentDescription = stringResource(id = R.string.gallery_button_alt)
    ) {
        when (painter.state) {
            AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Error -> {
                Image(
                    modifier = Modifier.padding(16.dp),
                    painter = painterResource(id = R.drawable.ic_photo),
                    contentScale = ContentScale.Fit,
                    contentDescription = null
                )
            }
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

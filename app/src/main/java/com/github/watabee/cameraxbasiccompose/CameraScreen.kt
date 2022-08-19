package com.github.watabee.cameraxbasiccompose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        val cameraViewState = rememberCameraViewState()
        CameraView(
            modifier = Modifier.fillMaxSize(),
            cameraViewState = cameraViewState
        )

        if (cameraViewState.canToggleLensFacing) {
            Image(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 92.dp)
                    .clip(CircleShape)
                    .size(64.dp)
                    .clickable {
                        cameraViewState.toggleLensFacing()
                    },
                painter = painterResource(id = R.drawable.ic_switch),
                contentDescription = stringResource(id = R.string.switch_camera_button_alt)
            )
        }

        if (cameraViewState.canTakePicture) {
            Image(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .clip(CircleShape)
                    .size(92.dp)
                    .clickable {
                        cameraViewState.takePicture()
                    },
                painter = painterResource(id = R.drawable.ic_shutter_normal),
                contentDescription = stringResource(id = R.string.capture_button_alt)
            )
        }
    }
}

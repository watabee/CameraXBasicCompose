package com.github.watabee.cameraxbasiccompose

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen() {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            CameraScreen()
        }
        is PermissionStatus.Denied -> {
            Column {
                val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                    "The camera is important for this app. Please grant the permission."
                } else {
                    "Camera not available"
                }

                Text(textToShow)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                }
            }
        }
    }
}

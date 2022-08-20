package com.github.watabee.cameraxbasiccompose

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import com.github.watabee.cameraxbasiccompose.utils.filterJpegFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun rememberPhotoFileController(): PhotoFileController {
    val context = LocalContext.current
    return remember(context) { PhotoFileController(context) }
}

@Stable
class PhotoFileController(
    private val context: Context
) : RememberObserver {
    private val outputDirectory: File
    private val simpleDateFormat = SimpleDateFormat(FILENAME, Locale.US)

    private var coroutineScope: CoroutineScope? = null

    var lastSavedPhotoUri: Uri? by mutableStateOf(null)
        private set

    init {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
    }

    fun createPhotoFile(): File {
        val filename = simpleDateFormat.format(System.currentTimeMillis())
        return File(outputDirectory, "$filename$PHOTO_EXTENSION")
    }

    fun onPhotoSaved(savedPhotoUri: Uri) {
        lastSavedPhotoUri = savedPhotoUri
        scanFile(savedPhotoUri)
    }

    private fun scanFile(savedPhotoUri: Uri) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(savedPhotoUri.toFile().extension)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(savedPhotoUri.toFile().absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
            Timber.w("Image capture scanned into media store: $uri")
        }
    }

    override fun onRemembered() {
        if (coroutineScope != null) {
            return
        }
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        coroutineScope?.launch {
            lastSavedPhotoUri = outputDirectory.filterJpegFiles()?.maxOrNull()?.let(Uri::fromFile)
        }
    }

    override fun onAbandoned() {
        clear()
    }

    override fun onForgotten() {
        clear()
    }

    private fun clear() {
        coroutineScope?.cancel()
        coroutineScope = null
    }

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
    }
}

package com.github.watabee.cameraxbasiccompose.utils

import java.io.File
import java.util.Locale

fun File.filterJpegFiles(): Array<File>? = listFiles { file ->
    arrayOf("JPG").contains(file.extension.uppercase(Locale.ROOT))
}

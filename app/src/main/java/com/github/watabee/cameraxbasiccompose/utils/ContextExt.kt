package com.github.watabee.cameraxbasiccompose.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.getActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

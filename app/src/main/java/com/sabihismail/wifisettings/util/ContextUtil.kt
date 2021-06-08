package com.sabihismail.wifisettings.util

import android.content.Context
import android.util.TypedValue
import com.sabihismail.wifisettings.R

object ContextUtil {
    fun Context.appName(): String {
        return resources.getString(R.string.app_name)
    }

    fun Context.dpToPixel(num: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, num.toFloat(), resources.displayMetrics).toInt()
    }
}

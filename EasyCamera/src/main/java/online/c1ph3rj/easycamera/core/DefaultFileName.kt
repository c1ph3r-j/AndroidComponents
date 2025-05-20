package online.c1ph3rj.easycamera.core


import java.text.SimpleDateFormat
import java.util.*

internal object DefaultFileName {
    private val sdf = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ENGLISH)
    fun name(isVideo: Boolean): String =
        (if (isVideo) "VID_" else "IMG_") + sdf.format(Date()) + if (isVideo) ".mp4" else ".jpg"
}

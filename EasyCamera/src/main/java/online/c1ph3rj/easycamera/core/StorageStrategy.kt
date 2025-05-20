package online.c1ph3rj.easycamera.core

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Abstraction that decides where the media goes.
 */
interface StorageStrategy {
    /** Called for videos; return pair of ContentValues + RELATIVE_PATH */
    fun createVideoOptions(): Pair<ContentValues, String>

    /** Called for images; returns target File */
    fun createImageFile(): File
}

class MediaStoreStrategy(
    private val subDir: String = "DCIM/CameraKit"
) : StorageStrategy {

    override fun createVideoOptions(): Pair<ContentValues, String> {
        val fileName = DefaultFileName.name(true)
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, subDir)
        } to "$subDir/$fileName"
    }

    override fun createImageFile(): File {
        val root = Environment.getExternalStoragePublicDirectory(subDir)
        if (!root.exists()) root.mkdirs()
        return File(root, DefaultFileName.name(false))
    }
}

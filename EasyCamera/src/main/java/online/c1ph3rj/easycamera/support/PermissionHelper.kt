package online.c1ph3rj.easycamera.support

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

typealias PermissionCallback = (Boolean) -> Unit

internal class PermissionHelper(private val caller: ActivityResultCaller) {

    private var callback: PermissionCallback? = null

    private val launcher =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val granted = it.values.all { v -> v }
            callback?.invoke(granted)
        }

    fun ensurePermissions(cb: PermissionCallback) {
        val needed = REQUIRED.filter {
            ContextCompat.checkSelfPermission(
                (caller as android.content.Context),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) cb(true)
        else {
            callback = cb
            launcher.launch(needed.toTypedArray())
        }
    }

    companion object {
        private val REQUIRED = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ) else arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

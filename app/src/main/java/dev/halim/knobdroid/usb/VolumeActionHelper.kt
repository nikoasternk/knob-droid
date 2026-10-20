package dev.halim.knobdroid.usb

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import java.util.concurrent.Executors

object VolumeActionHelper {
    private const val TAG = "VolumeActionHelper"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun applyVolume(
        context: Context,
        usbManager: UsbManager,
        sharedPreferences: SharedPreferences,
        requestedVolume: Int? = null,
        onComplete: () -> Unit = {}
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Audio permission not granted")
            onComplete()
            return
        }

        val volumePercent = requestedVolume ?: sharedPreferences.getInt(
            AppConstants.PreferenceKeys.VOLUME_PERCENT,
            AppConstants.PreferenceKeys.DEFAULT_VOLUME_PERCENT
        )

        sharedPreferences.edit { 
            putInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, volumePercent) 
        }

        executor.execute {
            val result = VolumeController.execute(usbManager, volumePercent)
            val appContext = context.applicationContext

            mainHandler.post {
                when (result) {
                    is VolumeController.Result.Success -> {
                        Log.d(TAG, "Applied volume: $volumePercent%")
                        showToast(appContext, "Device volume set to $volumePercent%")
                    }
                    is VolumeController.Result.DeviceNotFound -> {
                        showToast(appContext, "Device not found")
                    }
                    is VolumeController.Result.Error -> {
                        showToast(appContext, "Error: ${result.message}")
                    }
                    is VolumeController.Result.VolumeError -> {
                        showToast(appContext, "Error code: ${result.code}")
                    }
                }
                onComplete()
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

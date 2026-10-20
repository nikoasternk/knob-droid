package dev.halim.knobdroid.ui.screen

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.usb.UsbHelper
import kotlin.math.roundToInt

@Composable
fun UsbControlScreen(
    modifier: Modifier = Modifier,
    sharedPreferences: SharedPreferences,
    onApplyVolume: (Int) -> Unit,
) {
    val androidContext = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(androidContext, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasAudioPermission = isGranted
        }

    var volumePercent by remember {
        mutableFloatStateOf(
            sharedPreferences.getInt(
                AppConstants.PreferenceKeys.VOLUME_PERCENT,
                AppConstants.PreferenceKeys.DEFAULT_VOLUME_PERCENT
            ).toFloat()
        )
    }

    var deviceName by remember { mutableStateOf("Waiting for USB device...") }

    DisposableEffect(androidContext) {
        val usbManager = androidContext.getSystemService(Context.USB_SERVICE) as UsbManager

        fun updateDeviceName() {
            val appleDongle = UsbHelper.findAppleDongle(usbManager)
            deviceName = when {
                appleDongle != null -> "Apple USB-C Adapter connected"
                UsbHelper.hasAnyUsbDevice(usbManager) -> "USB device found"
                else -> "USB device not connected"
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateDeviceName()
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        androidContext.registerReceiver(receiver, filter)
        updateDeviceName()

        onDispose { androidContext.unregisterReceiver(receiver) }
    }

    if (!hasAudioPermission) {
        PermissionRequestScreen(
            onGrantClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "USB device hardware volume",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${volumePercent.roundToInt()}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = volumePercent,
                onValueChange = { volumePercent = it },
                valueRange = 0f..100f,
                steps = 100,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", style = MaterialTheme.typography.labelSmall)
                Text("100%", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onApplyVolume(volumePercent.roundToInt()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Apply (${volumePercent.roundToInt()}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

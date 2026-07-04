package com.danilaf.esp32controller.controls

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import androidx.annotation.RequiresApi
import com.danilaf.esp32controller.MainActivity
import com.danilaf.esp32controller.R
import com.danilaf.esp32controller.data.Device

object DeviceControlsRequester {
    fun requestAdd(context: Context, device: Device): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        requestAddInternal(context, device)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestAddInternal(context: Context, device: Device) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, MainActivity::class.java)
            .putExtra("deviceId", device.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            device.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val control = Control.StatelessBuilder(device.id, pendingIntent)
            .setTitle(device.name)
            .setSubtitle(device.room.ifBlank { appContext.getString(R.string.device_controls_subtitle) })
            .setDeviceType(DeviceTypes.TYPE_LIGHT)
            .build()
        ControlsProviderService.requestAddControl(
            appContext,
            ComponentName(appContext, EspControlsProviderService::class.java),
            control
        )
    }
}

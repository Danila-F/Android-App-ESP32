package com.danilaf.esp32controller.controls

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.ModeAction
import android.service.controls.actions.FloatAction
import android.service.controls.actions.CommandAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.StatelessTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import com.danilaf.esp32controller.MainActivity
import com.danilaf.esp32controller.R
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceRepository
import com.danilaf.esp32controller.network.EspApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
import java.util.concurrent.SubmissionPublisher
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.R)
class EspControlsProviderService : ControlsProviderService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        val publisher = SubmissionPublisher<Control>()
        scope.launch {
            runCatching {
                val repository = DeviceRepository(applicationContext)
                repository.getDevices().forEach { device ->
                    publisher.submit(device.toStatelessControl())
                }
            }
            publisher.close()
        }
        return publisher
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        val publisher = SubmissionPublisher<Control>()
        scope.launch {
            val repository = DeviceRepository(applicationContext)
            val api = EspApiClient(applicationContext)
            repository.getDevices()
                .filter { it.id in controlIds }
                .forEach { device ->
                    val control = runCatching {
                        val state = api.getState(device)
                        repository.updatePower(device.id, state.power)
                        device.copy(lastKnownPower = state.power).toStatefulControl(
                            status = Control.STATUS_OK,
                            statusText = if (state.power) getString(R.string.control_on) else getString(R.string.control_off)
                        )
                    }.getOrElse {
                        device.toStatefulControl(
                            status = Control.STATUS_ERROR,
                            statusText = getString(R.string.control_unavailable)
                        )
                    }
                    publisher.submit(control)
                }
            publisher.close()
        }
        return publisher
    }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: Consumer<Int>
    ) {
        scope.launch {
            val repository = DeviceRepository(applicationContext)
            val api = EspApiClient(applicationContext)
            val device = repository.findDevice(controlId)
            if (device == null) {
                consumer.accept(ControlAction.RESPONSE_FAIL)
                return@launch
            }

            val result = runCatching {
                when (action) {
                    is BooleanAction -> api.setPower(device, action.newState)
                    is CommandAction -> api.togglePower(device)
                    is FloatAction -> api.getState(device)
                    is ModeAction -> api.getState(device)
                    else -> api.getState(device)
                }
            }

            result.onSuccess { state ->
                repository.updatePower(device.id, state.power)
                consumer.accept(ControlAction.RESPONSE_OK)
            }.onFailure {
                consumer.accept(ControlAction.RESPONSE_FAIL)
            }
        }
    }

    private fun Device.toStatelessControl(): Control {
        return Control.StatelessBuilder(id, activityIntent(id))
            .setTitle(name)
            .setSubtitle(room.ifBlank { getString(R.string.device_controls_subtitle) })
            .setDeviceType(DeviceTypes.TYPE_LIGHT)
            .build()
    }

    private fun Device.toStatefulControl(status: Int, statusText: String): Control {
        val isOn = lastKnownPower ?: false
        return Control.StatefulBuilder(id, activityIntent(id))
            .setTitle(name)
            .setSubtitle(room.ifBlank { getString(R.string.device_controls_subtitle) })
            .setDeviceType(DeviceTypes.TYPE_LIGHT)
            .setStatus(status)
            .setStatusText(statusText)
            .setControlTemplate(
                ToggleTemplate(
                    "power",
                    ControlButton(isOn, if (isOn) getString(R.string.control_on) else getString(R.string.control_off))
                )
            )
            .build()
    }

    private fun activityIntent(deviceId: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("deviceId", deviceId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            deviceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

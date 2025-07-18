package com.itsmurphy.flutter_audio_output

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterAudioOutputPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            notifyChanged()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            notifyChanged()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "flutter_audio_output").apply {
            setMethodCallHandler(this@FlutterAudioOutputPlugin)
        }
        context = binding.applicationContext

        audioManager.registerAudioDeviceCallback(
            audioDeviceCallback, Handler(Looper.getMainLooper())
        )
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getAvailableOutputs" -> result.success(getAvailableOutputs())
            "changeToReceiver" -> result.success(changeToReceiver())
            "changeToSpeaker" -> result.success(changeToSpeaker())
            "changeToHeadphones" -> result.success(changeToHeadphones())
            "changeToBluetooth" -> result.success(changeToBluetooth())
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        channel.setMethodCallHandler(null)
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun notifyChanged() {
        channel.invokeMethod("inputChanged", 1)
    }

    private fun changeToReceiver(): Boolean {
        audioManager.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            stopBluetoothSco()
            isBluetoothScoOn = false
            isSpeakerphoneOn = false
        }
        notifyChanged()
        return true
    }

    private fun changeToSpeaker(): Boolean {
        audioManager.apply {
            mode = AudioManager.MODE_NORMAL
            stopBluetoothSco()
            isBluetoothScoOn = false
            isSpeakerphoneOn = true
        }
        notifyChanged()
        return true
    }

    private fun changeToHeadphones(): Boolean = changeToReceiver()

    private fun changeToBluetooth(): Boolean {
        audioManager.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            startBluetoothSco()
            isBluetoothScoOn = true
        }
        notifyChanged()
        return true
    }

    private fun getAvailableOutputs(): List<List<String>> {
        return listOf<List<String>>(
            listOf("Receiver", "1"),
            *getSpeakers().map { listOf<String>(it.productName.toString(), "2") }.toTypedArray(),
            *getWiredHeadsets().map { listOf<String>(it.productName.toString(), "3") }
                .toTypedArray(),
            *getBluetoothDevices().map { listOf<String>(it.productName.toString(), "4") }
                .toTypedArray(),
        )
    }

    private fun getSpeakers(): List<AudioDeviceInfo> {
        val devices =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return emptyList()
        return devices.filter {
            it.type in setOf(
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            )
        }
    }

    private fun getWiredHeadsets(): List<AudioDeviceInfo> {
        val devices =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return emptyList()
        return devices.filter {
            it.type in setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET
            )
        }
    }

    private fun getBluetoothDevices(): List<AudioDeviceInfo> {
        val devices =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return emptyList()
        return devices.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }
}

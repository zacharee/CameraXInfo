package dev.zwander.cameraxinfo.model

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.compose.runtime.*
import com.google.ar.core.ArCoreApk
import dev.zwander.cameraxinfo.data.CameraInfoHolder
import dev.zwander.cameraxinfo.data.ExtensionAvailability

val LocalDataModel = compositionLocalOf<DataModel> { error("No DataModel set") }

class DataModel {
    val supportedQualities = mutableStateMapOf<String, List<String>>()
    val physicalSensors = mutableStateMapOf<String, Map<String, CameraCharacteristics>>()
    val extensions = mutableStateMapOf<String, Map<Int, ExtensionAvailability>>()
    val cameraInfos = mutableStateListOf<CameraInfoHolder>()

    var arCoreStatus by mutableStateOf<ArCoreApk.Availability?>(null)
    var depthStatus by mutableStateOf<Boolean?>(null)
}
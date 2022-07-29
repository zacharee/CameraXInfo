package dev.zwander.cameraxinfo.model

import android.hardware.camera2.CameraCharacteristics
import androidx.compose.runtime.*
import com.google.ar.core.ArCoreApk

val LocalDataModel = compositionLocalOf<DataModel> { error("No DataModel set") }

class DataModel {
    val supportedQualities = mutableStateMapOf<String, List<String>>()
    val physicalSensors = mutableStateMapOf<String, Map<String, CameraCharacteristics>>()
    val extensions = mutableStateMapOf<String, Map<Int, Boolean?>>()

    var arCoreStatus by mutableStateOf<ArCoreApk.Availability?>(null)
    var depthStatus by mutableStateOf<Boolean?>(null)
}
package dev.zwander.cameraxinfo.data

import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo

data class CameraInfoHolder(
    val cameraInfo: CameraInfo,
    val camera2Info: Camera2CameraInfo
)

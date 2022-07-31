package dev.zwander.cameraxinfo.data

import android.annotation.SuppressLint
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import java.util.Objects

data class CameraInfoHolder(
    val cameraInfo: CameraInfo,
    val camera2Info: Camera2CameraInfo
) {
    @SuppressLint("UnsafeOptInUsageError")
    override fun equals(other: Any?): Boolean {
        return other is CameraInfoHolder &&
                other.camera2Info.cameraId == camera2Info.cameraId
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun hashCode(): Int {
        return Objects.hash(camera2Info.cameraId)
    }
}

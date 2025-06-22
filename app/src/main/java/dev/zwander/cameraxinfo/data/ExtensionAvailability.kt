package dev.zwander.cameraxinfo.data

data class ExtensionAvailability(
    val extension: Int,
    val camera2Availability: Boolean? = null,
    val cameraXAvailability: Boolean? = null,
    val strengthAvailability: Boolean? = null,
)

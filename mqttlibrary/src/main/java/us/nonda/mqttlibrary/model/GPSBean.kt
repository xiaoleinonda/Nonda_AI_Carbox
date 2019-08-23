package us.nonda.mqttlibrary.model

data class GPSBean constructor(
    var lat: Double,
    var lng: Double,
    var spd: Float,
    var acc: Float,
    var brg: Float,
    var time: Long
)


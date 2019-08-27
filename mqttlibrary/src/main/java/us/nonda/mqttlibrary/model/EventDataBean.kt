package us.nonda.mqttlibrary.model

class EventDataBean constructor(
    var fw: String?,
    var app: String?,
    var lat: Double,
    var lng: Double,
    var acc: Float?,
    var vol: Float?,
    var type: Int?,
    var content: String?
)

package us.nonda.mqttlibrary.model

data class StatusBean constructor(
    var fw: String?,
    var app: String?,
    var lat: Double,
    var lng: Double,
    var acc: Float?,
    var vol: Float?
) {

}

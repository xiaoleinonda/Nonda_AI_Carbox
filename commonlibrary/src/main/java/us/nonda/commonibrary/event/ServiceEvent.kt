package us.nonda.commonibrary.event

class ServiceEvent constructor(var action: Int, var open: Boolean) {

    companion object {
        val ACTION_GPS = 1
        val ACTION_GSENSOR = 2
        val ACTION_GYRO = 3

        val OPEN: Boolean = true
        val CLOSE: Boolean = false
    }


}
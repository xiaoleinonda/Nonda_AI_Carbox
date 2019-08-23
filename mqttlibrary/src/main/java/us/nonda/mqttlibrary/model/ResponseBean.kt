package us.nonda.mqttlibrary.model

class ResponseBean {
    /**
     * cmd : 20002
     * time : xxxxxx
     * data : {"collectFreq":1,"reportFreq":1}
     */
    var cmd: Int = 0
    var time: Long? = 0
    var data: DataBean? = null

    class DataBean {
        /**
         * collectFreq : 1
         * reportFreq : 1
         */
        var collectFreq: Int = 0
        var reportFreq: Int = 0
    }
}

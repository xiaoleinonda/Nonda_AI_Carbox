package us.nonda.commonibrary.model

class UploadPartResponseModel {

    /**
     * code : 200
     * msg : ok
     * data : {"result":true}
     */

    var code: Int = 0
    var msg: String? = null
    var data: DataBean? = null

    class DataBean {
        /**
         * result : true
         */

        var result: Boolean = false
    }
}

package us.nonda.ai.app.crash

import us.nonda.commonibrary.MyLog

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    companion object {
        val instance: CrashHandler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CrashHandler()
        }
    }

    override fun uncaughtException(p0: Thread, p1: Throwable) {
        MyLog.d("异常", "原因：${p1.message}")
    }


}
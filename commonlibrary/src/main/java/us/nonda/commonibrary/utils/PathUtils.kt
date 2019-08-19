package us.nonda.commonibrary.utils

import android.content.Context
import android.os.storage.StorageManager
import java.lang.reflect.InvocationTargetException

object PathUtils {

    /**
     * 获取Android系统所有内置SD卡以及外置SD卡路径
     *
     *
     * 返回的String[]数组中，string[0]为内置sd卡路径；string[1]为外置sd卡路径。
     */
    private fun getAllExtSDCardPaths(context: Context): Array<String>? {
        val storageManager = context.getSystemService(
            Context
                .STORAGE_SERVICE
        ) as StorageManager
        try {
            val paramClasses = arrayOf<Class<*>>()
            val getVolumePathsMethod = StorageManager::class.java.getMethod("getVolumePaths", *paramClasses)
            getVolumePathsMethod.isAccessible = true
            val params = arrayOf<Any>()
            val invoke = getVolumePathsMethod.invoke(storageManager, *params)
            return invoke as Array<String>
        } catch (e1: NoSuchMethodException) {
            e1.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        return null
    }


    /**
     * 获取sdcard1 路径
     */
    fun getSDCardPath(context: Context): String {
        val allExtSDCardPaths = getAllExtSDCardPaths(context)
        if (allExtSDCardPaths == null || allExtSDCardPaths.size < 1) {
            return ""
        }

        if (allExtSDCardPaths.size == 1) {
            return allExtSDCardPaths[0]
        }

        return allExtSDCardPaths[1]
    }
}
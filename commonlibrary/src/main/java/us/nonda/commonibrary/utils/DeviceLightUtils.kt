package us.nonda.commonibrary.utils

import android.text.TextUtils

class DeviceLightUtils {

    companion object {
        private val PILOT_LIGHT_R_PATH = "/sys/devices/leds_ctl/R_leds_control"
        private val PILOT_LIGHT_G_PATH = "/sys/devices/leds_ctl/G_leds_control"
        private val PILOT_LIGHT_B_PATH = "/sys/devices/leds_ctl/B_leds_control"

        fun normallyRed() {
            normally(PILOT_LIGHT_R_PATH)
            closeGreen()
            closeBlue()
        }


        fun normallyGreen() {
            normally(PILOT_LIGHT_G_PATH)
            closeRed()
            closeBlue()
        }

        fun normallyBlue() {
            normally(PILOT_LIGHT_B_PATH)
            closeRed()
            closeGreen()
        }

        fun normallPink() {
            normally(PILOT_LIGHT_R_PATH)
            normally(PILOT_LIGHT_B_PATH)
            close(PILOT_LIGHT_G_PATH)
        }

        fun normallYellow() {
            normally(PILOT_LIGHT_G_PATH)
            normally(PILOT_LIGHT_R_PATH)
            close(PILOT_LIGHT_B_PATH)
        }

        /**
         * 浅蓝色
         */
        fun normallWathet() {
            normally(PILOT_LIGHT_B_PATH)
            normally(PILOT_LIGHT_G_PATH)
            close(PILOT_LIGHT_R_PATH)
        }

        fun normallyWhite() {
            normally(PILOT_LIGHT_R_PATH)
            normally(PILOT_LIGHT_G_PATH)
            normally(PILOT_LIGHT_B_PATH)
        }


        fun flashRed() {
            flash(PILOT_LIGHT_R_PATH)
            closeGreen()
            closeBlue()
        }

        fun flashGreen() {
            flash(PILOT_LIGHT_G_PATH)
            closeRed()
            closeBlue()
        }

        fun flashBlue() {
            flash(PILOT_LIGHT_B_PATH)
            closeRed()
            closeGreen()
        }

        fun flashPink() {
            flash(PILOT_LIGHT_R_PATH)
            flash(PILOT_LIGHT_B_PATH)
            close(PILOT_LIGHT_G_PATH)
        }

        fun flashYellow() {
            flash(PILOT_LIGHT_G_PATH)
            flash(PILOT_LIGHT_R_PATH)
            close(PILOT_LIGHT_B_PATH)
        }

        /**
         * 浅蓝色
         */
        fun flashWathet() {
            flash(PILOT_LIGHT_B_PATH)
            flash(PILOT_LIGHT_G_PATH)
            close(PILOT_LIGHT_R_PATH)
        }

        fun flashWhite() {
            flash(PILOT_LIGHT_R_PATH)
            flash(PILOT_LIGHT_G_PATH)
            flash(PILOT_LIGHT_B_PATH)
        }


        fun closeRed() {
            close(PILOT_LIGHT_R_PATH)
        }


        fun closeGreen() {
            close(PILOT_LIGHT_G_PATH)
        }


        fun closeBlue() {
            close(PILOT_LIGHT_B_PATH)
        }

        fun closeWhite() {
            closeRed()
            closeGreen()
            closeBlue()
        }

        fun putLightStatus() {
            val strR = StringUtils.getString(PILOT_LIGHT_R_PATH)
            val strG = StringUtils.getString(PILOT_LIGHT_G_PATH)
            val strB = StringUtils.getString(PILOT_LIGHT_B_PATH)
            val r = if (strR.isNullOrBlank()) "1" else strR
            val g = if (strG.isNullOrBlank()) "0" else strG
            val b = if (strB.isNullOrBlank()) "0" else strB
            val toString = StringBuffer().append(r.replace("f", ""))
                .append(g.replace("f", ""))
                .append(b.replace("f", ""))
                .toString()
            SPUtils.put(AppUtils.context, "light", toString)
        }

        fun restoreLastLightStatus() {
            val str = SPUtils.get(AppUtils.context, "light", "100") as String
            if (str.isNullOrBlank() || str.length >= 3) {
                val r = str[0].toString()
                val g = str[1].toString()
                val b = str[2].toString()
                StringUtils.writeString(PILOT_LIGHT_R_PATH, r)
                StringUtils.writeString(PILOT_LIGHT_G_PATH, g)
                StringUtils.writeString(PILOT_LIGHT_B_PATH, b)
            }

        }

        /**
         * 灯光常亮
         */
        private fun normally(lightPath: String) {
            StringUtils.writeString(lightPath, "1")
        }

        /**
         * 灯光闪烁
         */
        private fun flash(lightPath: String) {
            StringUtils.writeString(lightPath, "2")
        }

        /**
         * 灯光关闭
         */
        private fun close(lightPath: String) {
            StringUtils.writeString(lightPath, "0")
        }


    }


}
package dev.dotworld.telemetrydata.utils

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootUitls {


    companion object {
        public fun checkRootedOrNot(): Boolean {
            return (checkTags() || checkRootFiles() || checkSuperUser())
        }

        private fun checkTags(): Boolean {
            var buildTag = Build.TAGS
            return buildTag.contains("test-keys")
        }

        private fun checkRootFiles(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            return false
        }

        private fun checkSuperUser(): Boolean {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val output = BufferedReader(InputStreamReader(process.inputStream))
                return output.readLine() != null

            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                process?.destroy()
            }

        }
    }}


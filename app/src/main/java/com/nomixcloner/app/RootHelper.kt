package com.nomixcloner.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.scottyab.rootbeer.RootBeer
import com.scottyab.rootbeer.util.QLog
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays
import java.util.Scanner

class RootHelper(val context: Context) {

    val rootBeer = RootBeer(context)

    fun isDeviceRooted(): Boolean {
        return  checkSuExists()
    }


    fun isRooted(): Boolean {
        return checkForBinary("su")
//        rootBeer.detectRootManagementApps()
//              ||   rootBeer.detectPotentiallyDangerousApps()
//              ||   checkForBinary("su")
//              ||   checkForDangerousProps()
              ||   rootBeer.checkForRWPaths()
              ||   rootBeer.detectTestKeys()
//              ||   rootBeer.checkSuExists()
//              ||   rootBeer.checkForRootNative()
//              ||   rootBeer.checkForMagiskBinary()
    }

    fun checkForBinary(filename: String): Boolean {
        val pathsArray = Const.paths

        var result = false

        for (path in pathsArray) {
            val completePath = path + filename
            val f = File(path, filename)
            val fileExists = f.exists()
            if (fileExists) {
                QLog.v(completePath + " binary detected!")
                result = true
            }
        }

        return result
    }


    fun checkForDangerousProps(): Boolean {
        val dangerousProps: MutableMap<String, String?> = HashMap<String, String?>()
        dangerousProps.put("ro.debuggable", "1")
        dangerousProps.put("ro.secure", "0")

        var result = false

        val lines: Array<String>? = propsReader()

        if (lines == null) {
            // Could not read, assume false;
            return false
        }

        for (line in lines) {
            for (key in dangerousProps.keys) {
                if (line.contains(key)) {
                    var badValue = dangerousProps.get(key)
                    badValue = "[" + badValue + "]"
                    if (line.contains(badValue)) {
                        QLog.v(key + " = " + badValue + " detected!")
                        result = true
                    }
                }
            }
        }
        return result
    }

    fun checkSuExists(): Boolean {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf<String?>("which", "su"))
            val `in` = BufferedReader(InputStreamReader(process.getInputStream()))
            return `in`.readLine() != null
        } catch (t: Throwable) {
            return false
        } finally {
            if (process != null) process.destroy()
        }
    }

    fun check(): Boolean {
        //work
       return File("/system/xbin/su").exists()
    }

    fun checkRootAccess(): Boolean {
        //not work
        return try {
            // Пытаемся выполнить простую команду от root
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))
            val output = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            output == "test"
        } catch (e: Exception) {
            false
        }
    }

    fun isRootedByBuildProps(): Boolean {
        //work
        return try {
            val process = ProcessBuilder("getprop", "ro.debuggable")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()

            output == "1"  // 1 = debug сборка, возможен root
        } catch (e: Exception) {
            false
        }
    }



    fun hasMagisk(): Boolean {
        val magiskPaths = arrayOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/ksu",
            "/data/adb/ap"
        )

        return magiskPaths.any { File(it).exists() }
    }
    private fun checkRootMethod1(): Boolean {


        //not work
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/abb",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootMethod2(): Boolean {
        //not work
        val commands = arrayOf("su", "/system/xbin/which su")
        return try {
            commands.any { cmd ->
                Runtime.getRuntime().exec(cmd).waitFor() == 0
            }
        } catch (e: Exception) {
            false
        }
    }
    fun checkViaBuildTags(): Boolean {
        //not works
        return Build.TAGS?.contains("test-keys") == true
    }

    fun checkRootApps(): Boolean {
        //not works
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine"
        )

        val packageManager = context.packageManager
        return rootApps.any { app ->
            try {
                packageManager.getPackageInfo(app, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun propsReader(): Array<String>? {
        try {
            val inputstream = Runtime.getRuntime().exec("getprop").getInputStream()
            if (inputstream == null) return null
            val propVal = Scanner(inputstream).useDelimiter("\\A").next()
            return propVal.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } catch (e: IOException) {
            QLog.e(e)
            return null
        } catch (e: NoSuchElementException) {
            QLog.e(e)
            return null
        }
    }

    fun checkSuCommand(): Boolean {
        //not work
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val input = BufferedReader(InputStreamReader(process.inputStream))
            input.readLine() != null
        } catch (e: Exception) {
            false
        }
    }
}

internal class Const private constructor() {
    init {
        throw InstantiationException("This class is not for instantiation")
    }

    companion object {
        const val BINARY_SU: String = "su"
        const val BINARY_BUSYBOX: String = "busybox"

        val knownRootAppsPackages: Array<String?> = arrayOf<String?>(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )

        val knownDangerousAppsPackages: Array<String?> = arrayOf<String?>(
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.chelpus.luckypatcher",
            "com.blackmartalpha",
            "org.blackmart.market",
            "com.allinone.free",
            "com.repodroid.app",
            "org.creeplays.hack",
            "com.baseappfull.fwd",
            "com.zmapp",
            "com.dv.marketmod.installer",
            "org.mobilism.android",
            "com.android.wp.net.log",
            "com.android.camera.update",
            "cc.madkite.freedom",
            "com.solohsu.android.edxp.manager",
            "org.meowcat.edxposed.manager",
            "com.xmodgame",
            "com.cih.game_cih",
            "com.charles.lpoqasert",
            "catch_.me_.if_.you_.can_"
        )

        val knownRootCloakingPackages: Array<String?> = arrayOf<String?>(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
        )

        // These must end with a /
        private val suPaths = arrayOf<String?>(
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/cache/",
            "/data/",
            "/dev/"
        )


        val pathsThatShouldNotBeWritable: Array<String?> = arrayOf<String?>(
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc",  //"/sys",
            //"/proc",
            //"/dev"
        )

        val paths: Array<String?>
            /**
             * Get a list of paths to check for binaries
             *
             * @return List of paths to check, using a combination of a static list and those paths
             * listed in the PATH environment variable.
             */
            get() {
                val paths = ArrayList<String?>(Arrays.asList<String?>(*suPaths))

                val sysPaths = System.getenv("PATH")

                // If we can't get the path variable just return the static paths
                if (sysPaths == null || "" == sysPaths) {
                    return paths.toTypedArray<String?>()
                }

                for (path in sysPaths.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    var path = path
                    if (!path.endsWith("/")) {
                        path = path + '/'
                    }

                    if (!paths.contains(path)) {
                        paths.add(path)
                    }
                }

                return paths.toTypedArray<String?>()
            }
    }
}

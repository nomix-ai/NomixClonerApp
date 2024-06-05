package com.nomixcloner.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionsHelper(
    private val activity: MainActivity
) {

    fun isReadPhoneStatePermissionGranted(): Boolean = isGranted(Manifest.permission.READ_PHONE_STATE)

    fun requestReadPhoneStatePermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            READ_PHONE_STATE_PERMISSION_REQUEST_CODE
        )
    }

    fun isLocationPermissionGranted(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION) && isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun requestMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && (!isGranted(Manifest.permission.READ_MEDIA_IMAGES)
                    || !isGranted(Manifest.permission.READ_MEDIA_VIDEO)
                    || !isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ),
                MEDIA_PERMISSION_REQUEST_CODE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && (!isGranted(Manifest.permission.READ_MEDIA_IMAGES) || !isGranted(Manifest.permission.READ_MEDIA_VIDEO))
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ),
                MEDIA_PERMISSION_REQUEST_CODE
            )
        } else if (!isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MEDIA_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun isGranted(grantResults: IntArray): Boolean = !grantResults.contains(PackageManager.PERMISSION_DENIED)

    private fun isGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 1
        const val LOCATION_PERMISSION_REQUEST_CODE = 2
        const val MEDIA_PERMISSION_REQUEST_CODE = 3
    }
}
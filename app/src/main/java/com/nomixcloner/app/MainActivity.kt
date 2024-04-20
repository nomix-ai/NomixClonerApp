package com.nomixcloner.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.nomixcloner.app.ui.theme.NomixClonerAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var googleAdId: String by mutableStateOf("")
    private var simInfo: String by mutableStateOf("")
    private var locationInfo: String by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getGoogleAdvertisingId(this)
        setContent {
            NomixClonerAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DeviceInfoScreen(this, googleAdId, ::requestSimInfo, simInfo, ::requestLocationInfo, locationInfo)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestSimInfo()
        }

        if (requestCode == 2 && grantResults.size >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            requestLocationInfo()
        }
    }

    private fun getGoogleAdvertisingId(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            googleAdId = try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                adInfo.id ?: "Unknown"
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                "Unknown"
            }
        }
    }

    private fun requestSimInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
        } else {
            simInfo = try {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                "${telephonyManager.simOperatorName}, ${telephonyManager.simCountryIso}"
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                "Unknown"
            }
        }
    }

    private fun requestLocationInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 2)
        } else {
            try {
                val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                val locationResult: Task<Location> = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    locationInfo = if (task.isSuccessful && task.result != null) {
                        val location: Location = task.result!!
                        "lat ${location.latitude}, long ${location.longitude}"
                    } else {
                        "Unknown"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                locationInfo = "Unknown"
            }
        }
    }
}

@SuppressLint("HardwareIds")
@Composable
fun DeviceInfoScreen(
    context: Context,
    googleAdId: String,
    requestSimInfo: () -> Unit,
    simInfo: String,
    requestLocationInfo: () -> Unit,
    locationInfo: String,
) {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val dnsServers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getDnsServers(context)
    } else {
        "Unknown"
    }
    val buildProps = """
        ${Build.MODEL}
        ${Build.MANUFACTURER}
        ${Build.BRAND}
        ${Build.PRODUCT}
        ${Build.DEVICE}
        ${Build.BOARD}
        ${Build.HARDWARE}
        ${Build.BOOTLOADER}
        ${Build.FINGERPRINT}
        ${Build.DISPLAY}
        ${Build.ID}
        ${Build.VERSION.SECURITY_PATCH}
        ${Build.VERSION.RELEASE}
        ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Build.VERSION.RELEASE_OR_CODENAME else "Unknown"}
        ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY else "Unknown"}
        ${Build.VERSION.INCREMENTAL}
        ${Build.VERSION.SDK_INT}
    """
    val osVersion = System.getProperty("os.version")

    val scrollState = rememberScrollState()

    Box {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(fontWeight = FontWeight.Bold, text = "/* --- DEVICE IDENTITY --- */\n")
            Text("Android ID: $androidId\n")
            Text("DNS servers: $dnsServers\n")
            Text("Build props: $buildProps")
            Text("OS version: $osVersion\n")
            Text("Google Advertising ID: $googleAdId\n")
            Text(fontWeight = FontWeight.Bold, text = "/* --- PERMISSIONS --- */\n")
            Button(onClick = requestSimInfo) {
                Text("READ_PHONE_STATE")
            }
            Text("SIM info: $simInfo\n")
            Button(onClick = requestLocationInfo) {
                Text("LOCATION")
            }
            Text("Current location: $locationInfo")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getDnsServers(context: Context): String {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
    return linkProperties
        ?.dnsServers
        ?.mapNotNull { it.hostAddress }
        ?.joinToString(", ")
        ?: "Unknown"
}

@Preview(showBackground = true)
@Composable
fun DeviceInfoPreview() {
    NomixClonerAppTheme {
        DeviceInfoScreen(LocalContext.current, "", {}, "",  {}, "")
    }
}

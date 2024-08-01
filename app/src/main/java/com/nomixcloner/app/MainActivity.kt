package com.nomixcloner.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.nomixcloner.app.ui.WebViewScreen
import com.nomixcloner.app.ui.theme.NomixClonerAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean


private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var googleAdId: String by mutableStateOf("")
    private var simInfo: String by mutableStateOf("")
    private var locationInfo: String by mutableStateOf("")
    private var ipInfo: String by mutableStateOf("")
    private var ipInfoOkHttp: String by mutableStateOf("")
    private var proxyType: Proxy.Type by mutableStateOf(Proxy.Type.DIRECT)
    private lateinit var permissionsHelper: PermissionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsHelper = PermissionsHelper(this)
        getGoogleAdvertisingId(this)
        getProxyType()

        setContent {
            NomixClonerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (BuildConfig.webViewMode) {
                        WebViewScreen(url = "https://nomixcloner.com")
                    } else {
                        DeviceInfoScreen(
                            this,
                            googleAdId,
                            ::requestSimInfo,
                            simInfo,
                            ::requestLocationInfo,
                            locationInfo,
                            { permissionsHelper.requestMediaPermissions() },
                            ::requestIpInfo,
                            ipInfo,
                            ::requestIpInfoOkHttp,
                            ipInfoOkHttp,
                            proxyType
                        )
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionsHelper.READ_PHONE_STATE_PERMISSION_REQUEST_CODE && permissionsHelper.isGranted(
                grantResults
            )
        ) {
            requestSimInfo()
        }

        if (requestCode == PermissionsHelper.LOCATION_PERMISSION_REQUEST_CODE && permissionsHelper.isGranted(
                grantResults
            )
        ) {
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
        if (!permissionsHelper.isReadPhoneStatePermissionGranted()) {
            permissionsHelper.requestReadPhoneStatePermission()
        } else {
            simInfo = try {
                val telephonyManager =
                    getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                "${telephonyManager.simOperatorName}, ${telephonyManager.simCountryIso}"
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                "Unknown"
            }
        }
    }

    private fun requestLocationInfo() {
        if (!permissionsHelper.isLocationPermissionGranted()) {
            permissionsHelper.requestLocationPermission()
        } else {
            try {
                val fusedLocationClient: FusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(this)
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

                val result = StringBuilder()
                val locationHelper = LocationHelper()

                val isLoading = AtomicBoolean(true)
                GlobalScope.launch(Dispatchers.Unconfined) {
                    val indicator = "◢◣◤◥"
                    var index = 0
                    while (isLoading.get()) {
                        locationInfo =
                            "Loading... " + indicator[index++ % indicator.length].toString()
                        delay(200)
                    }
                }

                GlobalScope.launch(Dispatchers.IO) {
                    result.append(
                        locationHelper.requestNativeCurrentLocation(
                            locationManager,
                            this@MainActivity
                        )
                    )
                    result.append(locationHelper.requestNativeLastLocation(locationManager))

                    result.append(locationHelper.requestLastLocation(fusedLocationClient))
                    result.append(locationHelper.requestLastLocationBound(fusedLocationClient))
                    result.append(locationHelper.requestCurrentLocation(fusedLocationClient))
                    result.append(locationHelper.requestCurrentLocationBound(fusedLocationClient))
                    isLoading.set(false)
                    locationInfo = result.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                locationInfo = "Unknown"
            }
        }
    }

    private fun requestIpInfo() {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL("https://ipinfo.io/json")
            val connection = url.openConnection() as HttpURLConnection

            ipInfo = try {
                connection.requestMethod = "GET"
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun requestIpInfoOkHttp() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url("https://ipinfo.io/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    ipInfoOkHttp = if (response.isSuccessful) {
                        response.body?.string().orEmpty()
                    } else {
                        ""
                    }
                }
            } finally {

            }
        }
    }

    private fun getProxyType() {
        val proxyList = ProxySelector.getDefault().select(URI.create("https://google.com"))
        proxyType = if (proxyList.isNotEmpty() && proxyList[0].type() != Proxy.Type.DIRECT) {
            proxyList[0].type()
        } else {
            Proxy.Type.DIRECT
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
    requestMediaPermissions: () -> Unit,
    requestIp: () -> Unit,
    ipInfo: String,
    requestIpOkHttp: () -> Unit,
    ipInfoOkHttp: String,
    proxyType: Proxy.Type
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
    val webView = remember { WebView(context) }
    val userAgent = remember { mutableStateOf(webView.settings.userAgentString) }
    val defaultUserAgent = remember { mutableStateOf(WebSettings.getDefaultUserAgent(context)) }
    var showWebView by remember { mutableStateOf(false) }

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
            Text(locationInfo)
            Button(onClick = requestMediaPermissions) {
                Text("MEDIA")
            }
            Text(fontWeight = FontWeight.Bold, text = "\n/* --- WEB VIEW --- */\n")
            Text("User Agent: ${userAgent.value}\n")
            Text("Default User Agent: ${defaultUserAgent.value}\n")
            Button(onClick = { showWebView = true }) {
                Text("OPEN WEB VIEW")
            }
            Text("\nProxy detected? : ${if (proxyType == Proxy.Type.DIRECT) "No" else proxyType}\n")
            Button(onClick = requestIp) {
                Text("REQUEST IP")
            }
            Text("Ip info: ${ipInfo}\n")
            Button(onClick = requestIpOkHttp) {
                Text("REQUEST IP (OKHTTP)")
            }
            Text("Ip info OkHttp: ${ipInfoOkHttp}\n")
        }

        if (showWebView) {
            WebViewScreen(
                "https://www.whatismybrowser.com/detect/what-is-my-user-agent/",
                showTopBar = true,
                onClose = {
                    showWebView = false
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getDnsServers(context: Context): String {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        DeviceInfoScreen(LocalContext.current, "", {}, "", {}, "", {}, {}, "", {}, "", Proxy.Type.DIRECT)
    }
}

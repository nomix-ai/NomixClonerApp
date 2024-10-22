package com.nomixcloner.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.SSLCertificateSocketFactory
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var googleAdId: String by mutableStateOf("")
    private var simInfo: String by mutableStateOf("")
    private var appPackageName: String by mutableStateOf("")
    private var appSignature: String by mutableStateOf("")
    private var locationInfo: String by mutableStateOf("")
    private var ipInfo: String by mutableStateOf("")
    private var ipInfoOkHttp: String by mutableStateOf("")
    private var ipInfoSSL: String by mutableStateOf("")
    private var proxyType: Proxy.Type by mutableStateOf(Proxy.Type.DIRECT)
    private lateinit var permissionsHelper: PermissionsHelper
    private var hardwareData: String by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsHelper = PermissionsHelper(this)
        getGoogleAdvertisingId(this)
        getProxyType()

        appPackageName = applicationContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appPackageName += ", " + applicationContext.opPackageName
        }
        appSignature = getSignature()

        simInfo = obtainSimInfo()
        hardwareData = getAllBuildProperties()

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
                            simInfo,
                            appPackageName,
                            appSignature,
                            ::requestLocationInfo,
                            locationInfo,
                            { permissionsHelper.requestMediaPermissions() },
                            ::requestIpInfo,
                            ipInfo,
                            ::requestIpInfoOkHttp,
                            ipInfoOkHttp,
                            ::requestIpInfoSSLSocket,
                            ipInfoSSL,
                            proxyType,
                            hardwareData
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

    private fun obtainSimInfo(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var simInfo = telephonyManager.run {
            "$simOperator, $simOperatorName, $simCountryIso, $networkOperator, $networkOperatorName, $networkCountryIso, $phoneType, ${hasCarrierPrivileges()}"
        }
        if (permissionsHelper.isLocationPermissionGranted()) {
            @SuppressLint("MissingPermission")
            simInfo += ", ${telephonyManager.cellLocation}, ${telephonyManager.allCellInfo}"
        }
        return simInfo
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

                simInfo = obtainSimInfo()
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

    private fun requestIpInfoSSLSocket() {
        val authenticator: Authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("admin", "admin1".toCharArray())
            }
        }
        Authenticator.setDefault(authenticator)

        GlobalScope.launch(Dispatchers.IO) {
            val responseFactory1 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                SSLSocketFactory.getDefault()
            )
            val responseFactory2 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                HttpsURLConnection.getDefaultSSLSocketFactory()
            )
            val responseFactory3 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                SSLCertificateSocketFactory.getDefault()
            )
            val responseFactory4 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                SSLCertificateSocketFactory.getDefault(0)
            )
            val responseFactory5 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                SSLCertificateSocketFactory.getDefault(0, null)
            )
            val responseFactory6 = SSLSocketGetRequest.makeGetRequest(
                "ipinfo.io",
                443,
                "/json",
                SSLContext.getDefault().socketFactory
            )
            ipInfoSSL = """
                |
                |SSLSocketFactory: ${getCountry(responseFactory1)}
                |HttpsURLConnection: ${getCountry(responseFactory2)}
                |SSLCertificateSocketFactory1: ${getCountry(responseFactory3)}
                |SSLCertificateSocketFactory2: ${getCountry(responseFactory4)}
                |SSLCertificateSocketFactory3: ${getCountry(responseFactory5)}
                |SSLContext: ${getCountry(responseFactory6)}
                |""".trimMargin()
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

    private fun getCountry(input: String): String {
        // Regular expression to match the country value
        val pattern: Pattern = Pattern.compile("\"country\":\\s*\"(.*?)\"")
        val matcher: Matcher = pattern.matcher(input)

        // Check if the pattern matches
        return "Country: " + if (matcher.find()) {
            // Extract the value inside the quotes
            val country = matcher.group(1)
            country
        } else {
            "Unknown"
        }
    }

    private fun getSignature(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    val signatures = signingInfo.apkContentsSigners
                    signatures.joinToString(" : ") { signature ->
                        signature.toByteArray().toShortHash()
                    }
                } else {
                    "No signatures found"
                }
            } else {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                val signatures = packageInfo.signatures
                signatures.joinToString(" : ") { signature ->
                    signature.toByteArray().toShortHash()
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Error getting signature"
        }
    }

    private fun ByteArray.toShortHash(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this)
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    @SuppressLint("HardwareIds")
    @Composable
    fun DeviceInfoScreen(
        context: Context,
        googleAdId: String,
        simInfo: String,
        packageName: String,
        signature: String,
        requestLocationInfo: () -> Unit,
        locationInfo: String,
        requestMediaPermissions: () -> Unit,
        requestIp: () -> Unit,
        ipInfo: String,
        requestIpOkHttp: () -> Unit,
        ipInfoOkHttp: String,
        requestIpSSL: () -> Unit,
        ipInfoSSL: String,
        proxyType: Proxy.Type,
        hardwareData: String
    ) {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val dnsServers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getDnsServers(context)
        } else {
            "Unknown"
        }

        val scrollState = rememberScrollState()
        val webView = remember { WebView(context) }
        val userAgent = remember { mutableStateOf(webView.settings.userAgentString) }
        val defaultUserAgent = remember { mutableStateOf(WebSettings.getDefaultUserAgent(context)) }
        var showWebView by remember { mutableStateOf(false) }

        // Calculate boot times
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedRealtimeMillis = SystemClock.elapsedRealtime()
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        val bootTimeElapsed = Date(currentTimeMillis - elapsedRealtimeMillis)
        val bootTimeElapsedNanos = Date(currentTimeMillis - (elapsedRealtimeNanos / 1_000_000))

        Box {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(fontWeight = FontWeight.Bold, text = "/* --- DEVICE IDENTITY --- */\n")
                Text("Android ID: $androidId\n")
                Text("DNS servers: $dnsServers\n")
                Button(onClick = { copyToClipboard(context, hardwareData) }) {
                    Text("COPY JSON TO CLIPBOARD")
                }
                Text("\nHardware data: $hardwareData\n")
                Text("Boot time (elapsedRealtime): ${formatDate(bootTimeElapsed)}")
                Text("Boot time (elapsedRealtimeNanos): ${formatDate(bootTimeElapsedNanos)}\n")
                Text("Package name: $packageName\n")
                Text("Signature: $signature\n")
                Text("Google Advertising ID: $googleAdId\n")
                Text("SIM info: $simInfo\n")
                Text(fontWeight = FontWeight.Bold, text = "/* --- PERMISSIONS --- */\n")
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
                Button(onClick = requestIpSSL) {
                    Text("REQUEST IP (SSL)")
                }
                Text("Ip info SSL: ${ipInfoSSL}\n")
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
        val linkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        return linkProperties
            ?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.joinToString(", ")
            ?: "Unknown"
    }

    private fun getAllBuildProperties(): String {
        val json = JSONObject()

        json.put("board", Build.BOARD)
        json.put("bootloader", Build.BOOTLOADER)
        json.put("brand", Build.BRAND)
        json.put("cpu_abi", Build.CPU_ABI)
        json.put("cpu_abi2", Build.CPU_ABI2)
        json.put("device", Build.DEVICE)
        json.put("display", Build.DISPLAY)
        json.put("fingerprint", Build.FINGERPRINT)
        json.put("hardware", Build.HARDWARE)
        json.put("host", Build.HOST)
        json.put("id", Build.ID)
        json.put("manufacturer", Build.MANUFACTURER)
        json.put("model", Build.MODEL)
        json.put("odm_sku", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.ODM_SKU else Build.UNKNOWN)
        json.put("product", Build.PRODUCT)
        json.put("radio", Build.getRadioVersion())
        json.put("serial", Build.SERIAL)
        json.put("sku", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SKU else Build.UNKNOWN)
        json.put("soc_manufacturer", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else Build.UNKNOWN)
        json.put("soc_model", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else Build.UNKNOWN)
        json.put("supported_32_bit_abis", JSONArray(Build.SUPPORTED_32_BIT_ABIS))
        json.put("supported_64_bit_abis", JSONArray(Build.SUPPORTED_64_BIT_ABIS))
        json.put("supported_abis", JSONArray(Build.SUPPORTED_ABIS))
        json.put("tags", Build.TAGS)
        json.put("time", Build.TIME)
        json.put("type", Build.TYPE)
        json.put("user", Build.USER)
        json.put("version_base_os", Build.VERSION.BASE_OS)
        json.put("version_codename", Build.VERSION.CODENAME)
        json.put("version_incremental", Build.VERSION.INCREMENTAL)
        json.put("version_media_performance_class", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.VERSION.MEDIA_PERFORMANCE_CLASS else Build.UNKNOWN)
        json.put("version_preview_sdk_int", Build.VERSION.PREVIEW_SDK_INT)
        json.put("version_release", Build.VERSION.RELEASE)
        json.put("version_release_or_codename", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Build.VERSION.RELEASE_OR_CODENAME else Build.UNKNOWN)
        json.put("version_release_or_preview_display", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY else Build.UNKNOWN)
        json.put("version_sdk", Build.VERSION.SDK)
        json.put("version_sdk_int", Build.VERSION.SDK_INT)
        json.put("version_security_patch", Build.VERSION.SECURITY_PATCH)

        listOf(
            "os.version",
            "os.arch",
            "os.name",
            "java.vm.version",
            "java.vm.name",
            "java.vm.vendor",
            "java.specification.version",
            "java.specification.vendor",
            "java.version",
            "java.vendor",
            "user.name",
            "user.home",
            "user.dir",
            "file.separator",
            "path.separator",
            "line.separator",
            "java.class.path",
            "java.class.version",
            "java.library.path",
            "java.io.tmpdir",
            "java.compiler",
            "java.ext.dirs",
            "sun.boot.library.path"
        ).forEach { prop ->
            json.put(prop.replace(".", "_").toLowerCase(), System.getProperty(prop) ?: "Unknown")
        }

        return json.toString(2)  // 2 spaces for indentation
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hardware Data", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()

        // Write compact JSON to log
        try {
            val jsonObject = JSONObject(text)
            val compactJson = jsonObject.toString()
            Log.d(TAG, compactJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(date)
    }

    @Preview(showBackground = true)
    @Composable
    fun DeviceInfoPreview() {
        NomixClonerAppTheme {
            DeviceInfoScreen(
                LocalContext.current,
                "",
                "",
                "",
                "",
                {},
                "",
                {},
                {},
                "",
                {},
                "",
                {},
                "",
                Proxy.Type.DIRECT,
                ""
            )
        }
    }
}

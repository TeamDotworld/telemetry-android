package dev.dotworld.telemetrydata

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.net.wifi.WifiManager
import android.opengl.GLES10
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.gson.Gson
import dev.dotworld.telemetrydata.utils.RootUitls
import okhttp3.*
import java.io.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity.javaClass.simpleName

        private fun lensOrientationString(value: Int) = when (value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
            else -> "Unknown"
        }

        @SuppressLint("InlinedApi")
        fun enumerateCameras(cameraManager: CameraManager): List<FormatItem> {
            val availableCameras: MutableList<FormatItem> = mutableListOf()

            val cameraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )
                capabilities?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                ) ?: false
            }


            cameraIds.forEach { id ->

                Log.d("cameraId", "enumerateCameras: Cameraid $id")
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation =
                    lensOrientationString(characteristics.get(CameraCharacteristics.LENS_FACING)!!)

                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val outputFormats = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )!!.outputFormats

                availableCameras.add(FormatItem("$orientation JPEG ($id)", id, ImageFormat.JPEG))

                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                    outputFormats.contains(ImageFormat.RAW_SENSOR)
                ) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation RAW ($id)",
                            id,
                            ImageFormat.RAW_SENSOR
                        )
                    )
                }

                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                    ) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)
                ) {
                    availableCameras.add(
                        FormatItem(
                            "$orientation DEPTH ($id)",
                            id,
                            ImageFormat.DEPTH_JPEG
                        )
                    )
                }
            }

            return availableCameras
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun getListOFCamaras(context: Context): List<FormatItem> {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            return enumerateCameras(cameraManager) //google2
        }

    }

    private var permissionsRequired = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PRECISE_PHONE_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN


    )
    private val PERMISSION_REQUEST_CODE = 1
    lateinit var batteryText: TextView
    var locationTrackService = LocationTrack(this)
    lateinit var sensorManager: SensorManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var wifiManager: WifiManager
    lateinit var gsm: TelephonyManager

    protected var serverUrl: String = "ws://192.168.1.17:3001"
    protected var webSocket: WebSocket? = null
    private var isWebSocketOn = false
    private var availableBluetooth:ArrayList<AvailableBluetooth> = ArrayList()

    @ExperimentalStdlibApi
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initWS()
        Log.d(TAG, "onCreate: hasUsbHostFeature: ${hasUsbHostFeature(this)}")
        Log.d(TAG, "onCreate: getListOFCamaras" + getListOFCamaras(this))
        Log.d(
            TAG,
            "onCreate: checkGooglePlayServicesAvailable:" + checkGooglePlayServicesAvailable()
        )


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() as BluetoothAdapter
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        locationTrackService = LocationTrack(this)
        var sensore = findViewById<TextView>(R.id.SensorList)
        var bluetoothText = findViewById<TextView>(R.id.bluethoothList)
        var wifiText = findViewById<TextView>(R.id.wifiList)
        var gsmText = findViewById<TextView>(R.id.gsmList)
        batteryText = findViewById<TextView>(R.id.batteryList)
        gsm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bReciever, filter)
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startDiscovery()
        }
        /*  getSensor()
          getWifi()
          getBluetooth()
          getGsm()
          getLocation()
          getDeviceDetails()
          getMemoryDetails()
          getStorageDetails()
          batteryStatusAndDetails(this@MainActivity)
          getAllInstallApps()
          getCameraDetails()
          getSoundCardInfo()*/
        getDeviceDetails()

        Log.d(TAG, "onCreate: getGlVersion: " + getGlVersion(this))
        Log.d(TAG, "onCreate: getGlVersion: GL_VENDOR :" + GLES10.glGetString(GLES10.GL_VENDOR))
        Log.d(
            TAG,
            "onCreate: getGlVersion: GL_EXTENSIONS :" + GLES10.glGetString(GLES10.GL_EXTENSIONS)
        )

        findViewById<Button>(R.id.gps).setOnClickListener {
            Log.d(TAG, "onCreate: ")
            getLocation()
            findViewById<TextView>(R.id.locationid).text = " longitude ${getLocation().toString()}"
            sensore.visibility = View.GONE
            gsmText.visibility = View.GONE
            wifiText.visibility = View.GONE
            bluetoothText.visibility = View.GONE
        }

        findViewById<Button>(R.id.SensorBtn).setOnClickListener {
            sensore.text = getSensor().toString()
        }

        findViewById<Button>(R.id.bluethooth).setOnClickListener {
            bluetoothText.visibility = View.VISIBLE
            bluetoothText.text = getBluetooth().toString()
        }
        findViewById<Button>(R.id.wifibtn).setOnClickListener {
            wifiText.visibility = View.VISIBLE
            wifiText.setText(getWifi().toString())
        }
        findViewById<Button>(R.id.gsm).setOnClickListener {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "READ_PHONE_STATE ", Toast.LENGTH_SHORT).show()
            }
            gsmText.visibility = View.VISIBLE
            gsmText.text = getGsm().toString()

        }

        val call: PhoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(TAG, "onCallStateChanged: $incomingNumber")
                    if (isWebSocketOn) {
                        webSocket?.send(Gson().toJson(CallDetails("incomingNumber")))
                    }
                }
            }
        }
        gsm.listen(call, PhoneStateListener.LISTEN_CALL_STATE);

    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("HardwareIds")
    private fun getWifi(): WifiDetails {
        val info = wifiManager.connectionInfo
        var text =
            "wifi  \n ipAddres:${info.ipAddress} \n networkId: ${info.networkId} \n speed: ${info.linkSpeed} \n ssid name: ${info.ssid}\n bssid:${info.bssid} \n mac address${info.macAddress}"
        var scanresult = wifiManager.scanResults
        var ListOFavailableWifiList = ArrayList<AvailableWifiList>()
        scanresult.iterator().forEach { res ->
            Log.d(TAG, "getWifi:${res.SSID} ")
            Log.d(TAG, "getWifi frequency:${res.frequency} ")
            Log.d(TAG, "getWifi channelWidth:${res.channelWidth} ")
            Log.d(TAG, "getWifi: level${res.level} ")
            Log.d(TAG, "getWifi:venueName ${res.venueName} ")
            Log.d(TAG, "getWifi: BSSID ${res.BSSID} ")
            Log.d(TAG, "getWifi: capabilities ${res.capabilities} ")
            Log.d(TAG, "getWifi: isPasspointNetwork ${res.isPasspointNetwork} ")
            Log.d(TAG, "getWifi: timestamp ${res.timestamp} ")
            ListOFavailableWifiList.add(
                AvailableWifiList(
                    res.SSID,
                    res.frequency.toString(),
                    res.channelWidth.toString(),
                    res.level.toString(),
                    res.venueName.toString(),
                    res.BSSID,
                    res.capabilities,
                    res.isPasspointNetwork,
                    res.timestamp.toString()
                )
            )

        }



        return WifiDetails(
            Formatter.formatIpAddress(info.ipAddress),
            info.networkId.toString(),
            info.linkSpeed.toString(),
            info.ssid,
            info.bssid,
            info.macAddress,
            ListOFavailableWifiList
        )


    }

    fun batteryStatusAndDetails(context: Context): BatteryDetails {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, 0)
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        val rawlevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        Log.d(TAG, "batteryHealthStatus: plugged" + voltage?.let { getPluggedType(it) })
        Log.d(TAG, "batteryHealthStatus: technology: " + technology)
        Log.d(TAG, "batteryHealthStatus: helth " + health?.let { getHealth(it) })
        Log.d(TAG, "batteryHealthStatus: status :" + status?.let { getBatteryStatusString(it) })
        if (rawlevel != null) {
            Log.d(TAG, "batteryHealthStatus: leaavel  ${(rawlevel * 100) / scale!!}")
        }
        Log.d(TAG, "batteryHealthStatus: voltage" + voltage)
        Log.d(TAG, "batteryHealthStatus: temperature " + temperature)

        if (rawlevel != null) {
            return BatteryDetails(
                plugged?.let { getPluggedType(it) },
                technology,
                health?.let { getHealth(it) },
                status?.let { getBatteryStatusString(it) },
                ((rawlevel * 100) / scale!!).toString(),
                voltage.toString(),
                temperature.toString()
            )
        }
        return BatteryDetails()
    }

    private fun getGsm(): GsmDetails {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val networkCountryISO: String = gsm.networkCountryIso
            val SIMCountryISO: String = gsm.simCountryIso
            val voiceMailNumber: String? = gsm.voiceMailNumber
            val softwareVersion: String? = gsm.deviceSoftwareVersion
            val simType: String = getSiMType(gsm.phoneType)
            Log.d(
                TAG,
                "getGsm:GSM  \n IMEINumber: \n SIMSerialNumber:  \n networkCountryISO: ${networkCountryISO} \n " +
                        "voiceMailNumber: ${voiceMailNumber}\n SIMCountryISO:${SIMCountryISO} \n softwareVersion :${softwareVersion} \n simType:$simType"
            )
            return GsmDetails(
                networkCountryISO,
                voiceMailNumber,
                SIMCountryISO,
                softwareVersion,
                simType
            )
        }
        return GsmDetails("PERMISSION NOT AVAILABLE")
    }

    private fun getSensor(): ArrayList<SensoreDetails> {
        var sensoreDetailsList = ArrayList<SensoreDetails>()
        val mList: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        var sensoreList: String = ""
        var sb = StringBuilder()
        mList.forEach { sensor ->
            sensoreList.plus("name :${sensor.name} ,vendor:${sensor.vendor} ")
            Log.d(TAG, "onCreate: name :${sensor.name} ,vendor:${sensor.type}  ")
            sensoreDetailsList.add(
                SensoreDetails(
                    sensor.name,
                    sensor.vendor,
                    sensor.version.toString(),
                    sensor.type.toString()
                )
            )
        }

        Log.d(TAG, " getSensor : sensores" + sensoreList.toString())
        return sensoreDetailsList

    }

    private fun getLocation(): LocationDetails {
        if (locationTrackService.canGetLocation()) {
            val longitude: Double = locationTrackService.getLongitude()
            val latitude: Double = locationTrackService.getLatitude()
            Log.d(TAG, "onCreate: longitude $longitude,latitude: $latitude")
            return LocationDetails(longitude, latitude)
        } else {
            locationTrackService = LocationTrack(this)
            if (locationTrackService.canGetLocation()) {
                val longitude: Double = locationTrackService.getLongitude()
                val latitude: Double = locationTrackService.getLatitude()
                Log.d(TAG, "onCreate: longitude $longitude,latitude: $latitude")
                return LocationDetails(longitude, latitude)
            }

        }
        return LocationDetails(0.0, 0.0)
    }


    fun hasUsbHostFeature(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    }

    private fun checkGooglePlayServicesAvailable(): Boolean {
        val status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(applicationContext)
        if (status == ConnectionResult.SUCCESS) {
            return true
        }
        Log.e(
            TAG,
            "Google Play Services not available: " + GooglePlayServicesUtil.getErrorString(status)
        )
        if (GooglePlayServicesUtil.isUserRecoverableError(status)) {

        }
        return false
    }

    private fun getSiMType(type: Int): String {
        when (type) {
            TelephonyManager.PHONE_TYPE_CDMA -> return "CDMA"
            TelephonyManager.PHONE_TYPE_GSM -> return "GSM"
            TelephonyManager.PHONE_TYPE_SIP -> return "SIP"

            else -> return ""
        }
    }

    private fun getBluetooth(): ArrayList<PairedBluetoothDetails> {
        var bluetoothDetailsList = ArrayList<PairedBluetoothDetails>()
        if (bluetoothAdapter == null) {
            var bluetoothDetailsList = ArrayList<PairedBluetoothDetails>()

        } else {
            if (bluetoothAdapter.isEnabled) {
                var text = ""
                val devices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                Log.d(TAG, "getBluetooth: " + devices.size)
                for (device in devices) {
                    Log.d(
                        TAG,
                        "onCreate: getBluetooth: ${device.name},address : ${device.address},device type: ${device.type},bluetooth clas:  ${device.bluetoothClass},bondState: ${device.bondState} ,device alias: ${device.alias} "
                    )
                    val bluetoothDetails = PairedBluetoothDetails(
                        device.name,
                        device.address,
                        device.type.toString(),
                        device.bluetoothClass.toString(),
                        device.bondState.toString()
                    )
                    bluetoothDetailsList.add(bluetoothDetails)
                }

                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                val intent = this.registerReceiver(null, filter)
                bluetoothAdapter.startDiscovery()
                val action = intent?.action
                val mDeviceList = ArrayList<String>()
                Log.d(TAG, "getBluetooth:  after reciver")
                if (BluetoothDevice.ACTION_FOUND == action) {
                    Log.d(TAG, "getBluetooth:  after reciver")
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    Log.d(TAG, "getBluetooth:  discover ${device}")
                    Log.d(
                        TAG,
                        "getBluetooth:  address" + device?.address + "device name: ${device?.address} ,bondState:${device?.bondState} ,uuids:${device?.uuids},name: ${device?.name},type:${device?.type}"
                    )

                }
                Log.d(TAG, "getBluetooth: ${devices.size}")
                return bluetoothDetailsList

            } else {
                bluetoothDetailsList.add(
                    PairedBluetoothDetails(
                        "Bluetooth offline"
                    )
                )
                return bluetoothDetailsList
            }
        }
        bluetoothDetailsList.add(PairedBluetoothDetails("Bluetooth service unavailable"))
        return bluetoothDetailsList
    }

    private fun requestPermission() {
        Log.d(TAG, "requestPermissionForCameraAndMicrophone: check for ")
        if (ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[0]
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[1]
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[2]
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[3]
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[4]
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[5]
            ) != PackageManager.PERMISSION_GRANTED

        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[3])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[4])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[5])
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsRequired,
                    PERMISSION_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsRequired,
                    PERMISSION_REQUEST_CODE
                )
                Log.d(TAG, "requestPermissionForCameraAndMicrophone: just request the permission")
            }
        }
    }

    private fun sendDataToWS() {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {
                //logic
                if (isWebSocketOn) {
                    webSocket?.send(Gson().toJson(getWifi()))
                    webSocket?.send(Gson().toJson(getSensor()))
                    webSocket?.send(Gson().toJson(BluetoothDetails(getBluetooth(),availableBluetooth)))
                    availableBluetooth.clear()
//                    webSocket?.send(Gson().toJson(getBluetooth()))
                    webSocket?.send(Gson().toJson(getGsm()))
                    webSocket?.send(Gson().toJson(getLocation()))
                    webSocket?.send(Gson().toJson(getDeviceDetails()))
                    webSocket?.send(Gson().toJson(getMemoryDetails()))
                    webSocket?.send(Gson().toJson(batteryStatusAndDetails(this@MainActivity)))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        webSocket?.send(Gson().toJson(getStorageDetails()))
                    }
                    webSocket?.send(Gson().toJson(getAllInstallApps()))
                    webSocket?.send(Gson().toJson(getCameraDetails()))
                    webSocket?.send(Gson().toJson(getSoundCardInfo()))

                }


                mainHandler.postDelayed(this, 60000)
            }

        })

    }

    fun getGlVersion(ctx: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val am = ctx.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val configurationInfo = am.deviceConfigurationInfo
            configurationInfo.glEsVersion
        } else {
            GLES10.glGetString(GLES10.GL_VERSION)
        }
    }

    @SuppressLint("NewApi")
    private fun getDeviceDetails(): DeviceDetails {
        /*Log.d(TAG, "getDeviceDetails: serial" + Build.SERIAL)
        Log.d(TAG, "getDeviceDetails: model " + Build.MODEL)
        Log.d(TAG, "getDeviceDetails:  ID " + Build.ID)
        Log.d(TAG, "getDeviceDetails: " + Build.MANUFACTURER)
        Log.d(TAG, "getDeviceDetails: " + Build.BRAND)
        Log.d(TAG, "getDeviceDetails: " + Build.BOARD)
        Log.d(TAG, "getDeviceDetails: " + Build.TYPE)
        Log.d(TAG, "getDeviceDetails: user " + Build.USER)
        Log.d(TAG, "getDeviceDetails: base os version " + Build.VERSION.BASE_OS)
        Log.d(TAG, "getDeviceDetails: sdk version " + Build.VERSION.SDK_INT)
        Log.d(TAG, "getDeviceDetails: HOST " + Build.HOST)
        Log.d(TAG, "getDeviceDetails:  FINGERPRINT " + Build.FINGERPRINT)*/
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels
        /* Log.d(TAG, "getDeviceDetails: os  version " + Build.VERSION.RELEASE)
         Log.d(TAG, "getDeviceDetails: hardware " + Build.HARDWARE)
         Log.d(TAG, "getDeviceDetails: " + Build.DEVICE)
         Log.d(TAG, "getDeviceDetails: dispaly " + Build.DISPLAY)
         Log.d(TAG, "getDeviceDetails: producat " + Build.PRODUCT)
         Log.d(TAG, "getDeviceDetails: type " + Build.TIME)
         Log.d(TAG, "getDeviceDetails: unkown " + Build.UNKNOWN)
         Log.d(TAG, "getDeviceDetails: " + Build.Partition.PARTITION_NAME_SYSTEM)
         Log.d(TAG, "getDeviceDetails: os  SECURITY_PATCH " + Build.VERSION.SECURITY_PATCH)
         Log.d(TAG, "getDeviceDetails: " + Build.getRadioVersion())
         Log.d(TAG, "getDeviceDetails:  rooted?" + RootUitls.checkRootedOrNot())
         Log.d(TAG, "getDeviceDetails:  CPU?" + Build.SUPPORTED_ABIS[0])
         Log.d(TAG, "getDeviceDetails:  RadioVersion?=" + Build.getRadioVersion())
         Log.d(TAG, "getDeviceDetails:  language?" + Locale.getDefault().language)*/
        Log.d(
            TAG, "getDeviceDetails:  time zone?" + Integer.valueOf(
                TimeZone.getDefault().getOffset(
                    System.currentTimeMillis()
                ) / 60 / 1000
            )
        )
        val timeZone = TimeZone.getDefault()
        val timeZoneInGMTFormat = timeZone.getDisplayName(false, TimeZone.SHORT)
        Log.d(TAG, "getDeviceDetails: timeZoneInGMTFormat:$timeZoneInGMTFormat")

        Build.SUPPORTED_ABIS.iterator().forEach { i ->
            Log.d(TAG, "getDeviceDetails  Build.SUPPORTED_ABIS : ${i.toString()}")
        }
        var cpuModeName = ""
        var vendor_id = "no vendor id available"
        var cpu_family = "no cpu family available"
        var cpu_mhz = "no cpu_mhz available"
        var siblings = "no siblings available"
        var cache_alignment = "no Cache available"
        var processCount = 0
        val cpudata = LinkedHashMap<String, String>()
        try {
            val DATA = arrayOf("/system/bin/cat", "/proc/cpuinfo")
            val processBuilder = ProcessBuilder(*DATA)
            var process = processBuilder.start()
            val inputStream = process.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line = ""
            while (reader.readLine().also { line = it } != null) {
                val parts = line.split(":", ignoreCase = true, limit = 2).toTypedArray()
                if (parts.size >= 2) {
                    var key = parts[0].trim { it <= ' ' }
                    key = key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1)
                    val value = parts[1].trim { it <= ' ' }
                    Log.d(TAG, "getDeviceDetails cpu: key=${key}, value=${value}")
                    cpudata.put(key, value)
                    when (key) {
                        "Model name" -> {
                            Log.d(TAG, "getDeviceDetails: if")
                            cpuModeName = value
                        }
                        "Vendor_id" -> {
                            Log.d(TAG, "getDeviceDetails: if")
                            vendor_id = value
                        }
                        "Cpu family" -> {
                            Log.d(TAG, "getDeviceDetails: if")
                            cpu_family = value

                        }
                        "Cpu MHz" -> {
                            Log.d(TAG, "getDeviceDetails: if")
                            cpu_mhz = value
                        }
                        "Siblings" -> {
                            Log.d(TAG, "getDeviceDetails: if")
                            cpu_family = value
                        }
                        "Cache_alignment" -> {
                            Log.d(TAG, "Cache_alignment: $value")
                            cache_alignment = value
                        }
                        "Processor" -> {
                            Log.d(TAG, "Cache_alignment: $value")
                            processCount += 1

                        }
                    }
                }
            }
            reader.close()
            inputStream.close()
            Log.d("CPU_INFO length", "${cpuModeName.length}")
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        Log.d(TAG, "getDeviceDetails: cpu" + cpuModeName.toString())
        Log.d(TAG, "getDeviceDetails: cpu" + vendor_id.toString())
        return DeviceDetails(
            Build.SERIAL,
            Build.MODEL,
            Build.ID,
            Build.MANUFACTURER,
            Build.BRAND,
            Build.BOARD,
            Build.TYPE,
            Build.USER,
            Build.VERSION.BASE_OS,
            Build.VERSION.SDK_INT.toString(),
            Build.HOST,
            Build.FINGERPRINT,
            "$width x $height",
            Build.VERSION.RELEASE,
            Build.HARDWARE,
            Build.DISPLAY,
            Build.VERSION.SECURITY_PATCH,
            RootUitls.checkRootedOrNot(),
            cpuModeName,
            vendor_id,
            cpu_family,
            cpu_mhz,
            siblings,
            cache_alignment,
            processCount,
            hasUsbHostFeature(this),
            Locale.getDefault().language,
            timeZoneInGMTFormat,
            Build.getRadioVersion(),
            getGlVersion(this),
            checkGooglePlayServicesAvailable(),
            getOrientation()
        )
    }

    private  fun getOrientation():String{

        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "orientation: Portrait")
            // code for portrait mode
        } else {
            Log.d(TAG, "orientation: landscape")
            // code for landscape mode
        }
        return if (orientation== Configuration.ORIENTATION_PORTRAIT){ "Portrait"} else {"landscape"}
    }


    @SuppressLint("QueryPermissionsNeeded", "NewApi")
    private fun getAllInstallApps(): ArrayList<InstalledApps> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        var listOfpackageName = ArrayList<InstalledApps>()
        apps.iterator().forEach { app ->
            Log.d(
                TAG, "getAllInstallApps: appname: " +
                        "${app.name},className " + app.className + "" +
                        " packageName ${app.packageName}, " +
                        "dataDir :${app.dataDir}"
            )

            listOfpackageName.add(
                InstalledApps(
                    app.name,
                    app.packageName,
                    app.className,
                    app.dataDir
                )
            )


        }
        return listOfpackageName
    }

    private fun getMemoryDetails(): MemoryDetails {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        val mb = 1024 * 1024
        Log.d(TAG, "getMemoryDetails: " + memoryInfo.availMem / mb)
        Log.d(TAG, "getMemoryDetails: " + memoryInfo.lowMemory)
        Log.d(TAG, "getMemoryDetails: " + memoryInfo.threshold / mb)
        Log.d(TAG, "getMemoryDetails: " + memoryInfo.totalMem / mb)
        val runtime = Runtime.getRuntime()
        Log.d(TAG, "getMemoryDetails: ${runtime.maxMemory() / mb}")
        Log.d(TAG, "getMemoryDetails: ${runtime.totalMemory() / mb}")
        Log.d(TAG, "getMemoryDetails: ${runtime.freeMemory() / mb}")


        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        (totalBlocks * blockSize)
        Log.d(
            TAG, "getMemoryDetails: total " + Formatter.formatFileSize(
                this,
                totalBlocks * blockSize
            )
        )




        return MemoryDetails(
            memoryInfo.availMem / mb,
            memoryInfo.lowMemory,
            memoryInfo.threshold / mb,
            memoryInfo.totalMem / mb,
            runtime.maxMemory() / mb,
            runtime.totalMemory() / mb,
            runtime.freeMemory() / mb
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getStorageDetails(): StorageDetails {
        Log.d(TAG, "getStorageDetails: dedails" + CHECK_EXTERNAL_STORAGE())
        var internalTotalStorage = getInternalTotalStorage()
        var internalAvailableStorage = getInternalAvailableStorage()
        var availabeSDcardSize = "no SD card"
        var totalSDcardSize = "no SD card"
        CHECK_EXTERNAL_STORAGE()
        if (CHECK_EXTERNAL_STORAGE().isNotEmpty()) {
            var file = File(CHECK_EXTERNAL_STORAGE())
            val stat = StatFs(file.getPath())
            availabeSDcardSize =
                Formatter.formatFileSize(this, stat.blockSizeLong * stat.availableBlocksLong)
            totalSDcardSize =
                Formatter.formatFileSize(this, stat.blockSizeLong * stat.blockCountLong)

            Log.d(
                TAG, "getStorageDetails: availableSizeInBytes" + Formatter.formatFileSize(
                    this,
                    stat.blockSizeLong * stat.availableBlocksLong
                )
            )
            Log.d(
                TAG, "getStorageDetails: availableSizeInBytes total " + Formatter.formatFileSize(
                    this,
                    stat.blockSizeLong * stat.blockCountLong
                )
            )

        }
        return StorageDetails(
            internalTotalStorage,
            internalAvailableStorage,
            totalSDcardSize,
            availabeSDcardSize
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun CHECK_EXTERNAL_STORAGE(): String {
        val storage_directory = File("/storage")
        var storage: File
        for (i in storage_directory.listFiles().indices) {
            storage = storage_directory.listFiles()[i]
            if (storage.absolutePath.contains("emulated")) {
                continue
            }
            if (storage.absolutePath == "/storage/self") {
                continue
            }

            if (Environment.getExternalStorageState(storage).equals(Environment.MEDIA_MOUNTED)) {
                if (Environment.isExternalStorageEmulated(storage) === false) {
                    val finalStorage = storage
                    runOnUiThread {
                        Log.d(
                            TAG,
                            "Storage External SD Card exists. Path: " + finalStorage.absolutePath
                        )

                    }
                    return finalStorage.absolutePath
                }
            } else {
                Log.d(TAG, "No external Storage detected.")
            }
        }
        return ""
    }

    private fun getInternalAvailableStorage(): String? {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.getPath())
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        Log.d(
            TAG, "Storage: avilable " + Formatter.formatFileSize(
                this,
                availableBlocks * blockSize
            )
        )
        return Formatter.formatFileSize(this, availableBlocks * blockSize)
    }

    private fun getInternalTotalStorage(): String? {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.getPath())
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        Log.d(
            TAG, "Storage: total " + Formatter.formatFileSize(
                this,
                totalBlocks * blockSize
            )
        )
        return Formatter.formatFileSize(this, totalBlocks * blockSize)
    }

    private fun getHealth(health: Int): String {
        when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> return "COLD"
            BatteryManager.BATTERY_HEALTH_DEAD -> return "DEAD"
            BatteryManager.BATTERY_HEALTH_GOOD -> return "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> return "OVERHEAT"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> return "OVER VOLTAGE"
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> return "HEALTH_UNKNOWN"
            else -> return "HEALTH_UNKNOWN"
        }

    }

    private fun getPluggedType(type: Int): String {
        when (type) {
            BatteryManager.BATTERY_PLUGGED_AC -> return "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> return "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> return "WIRELESS"
            else -> return ""
        }
    }

    private fun getSoundCardNumber(): Int {
        class AudioFilter : FileFilter {
            override fun accept(pathname: File): Boolean =
                Pattern.matches("card[0-9]+", pathname.name)
        }

        return try {
            val dir = File("/proc/asound/")
            val files = dir.listFiles(AudioFilter())
            Log.d(TAG, "getSoundCardNumber: " + files.size)
            files.size
        } catch (e: Exception) {
            Log.d(TAG, "getSoundCardNumber: " + 1)
            1
        }
    }

    private fun getSoundCardId(cardPosition: Int): String {
        var id = "Unknown"
        val filePath = "/proc/asound/card$cardPosition/id"

        var reader: RandomAccessFile? = null
        try {
            reader = RandomAccessFile(filePath, "r")
            id = reader.readLine()
            Log.d(TAG, "tryToGetSoundCardId: getSoundCardInfo" + id)
        } catch (ignored: Exception) {
        } finally {
            reader?.close()
        }
        return id
    }

    private fun getAlsa(): String? {
        var alsa: String? = null
        val filePath = "/proc/asound/version"

        var reader: RandomAccessFile? = null
        try {
            reader = RandomAccessFile(filePath, "r")
            val version = reader.readLine()
            alsa = version
        } catch (ignored: Exception) {
        } finally {
            reader?.close()
        }
        Log.d(TAG, "tryToGetAlsa: getSoundCardInfo " + alsa)
        return alsa
    }

    private fun getSoundCardInfo(): SoundCardDetails {
        val soundCardNumber = getSoundCardNumber()
        var soundCardId = ""
        for (i in 0 until soundCardNumber) {
            soundCardId = getSoundCardId(i)
            Log.d(TAG, "getSoundCardInfo: ${"card"} $i, ${getSoundCardId(i)}")
        }

        val alsa = getAlsa()
        return SoundCardDetails(soundCardNumber, soundCardId, alsa)

    }

    private fun getBatteryStatusString(health: Int): String {
        when (health) {
            BatteryManager.BATTERY_STATUS_CHARGING -> return "CHARGING"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> return "DISCHARGING"
            BatteryManager.BATTERY_STATUS_FULL -> return "FUll"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> return "NOT_CHARGING"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> return "UNKNOWN"

            else -> return "UNKNOWN"
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getCameraDetails(): cameraDetails {
        val camDetails: List<FormatItem> = getListOFCamaras(this)
        return cameraDetails(camDetails.size, camDetails)
    }

    private fun initWS() {
        Log.d(TAG, "initWS: ")
        this@MainActivity.runOnUiThread(java.lang.Runnable {
            val okHttpClient = OkHttpClient()
            var request = Request.Builder().url(serverUrl).build()
            okHttpClient.readTimeoutMillis

            var webSocketListener: WebSocketListener = object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    isWebSocketOn = true
                    Log.d(TAG, "onMessage: $text")
                }

                @SuppressLint("SetTextI18n")
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "onOpen: ")
                    isWebSocketOn = true
                    sendDataToWS()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "onClosed: ")
                    isWebSocketOn = false
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                    Log.d(TAG, "onClosing: ")
                }

                @SuppressLint("SetTextI18n")
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    isWebSocketOn = false
                }


            }

            webSocket = okHttpClient.newWebSocket(request, webSocketListener)
        })
//        webSocket?.send("1st")

    }

    private val bReciever: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val curDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)



                Log.d(TAG, "onReceive: bReciever " + curDevice?.name)
                Log.d(TAG, "onReceive: bReciever " + curDevice?.address)
                if (availableBluetooth.size>0){
                availableBluetooth.iterator().forEach {a->
                    Log.d(TAG, "onReceive: "+ curDevice?.address?.let { a.toString().contains(it) })
                    if (curDevice?.address?.let { a.toString().contains(it) } == false){
                        availableBluetooth.add(AvailableBluetooth(curDevice?.name,curDevice?.address,curDevice?.type.toString(),curDevice?.bluetoothClass.toString(),curDevice?.bondState.toString()))
                    }

                }
                }else{
                    availableBluetooth.add(AvailableBluetooth(curDevice?.name,curDevice?.address,curDevice?.type.toString(),curDevice?.bluetoothClass.toString(),curDevice?.bondState.toString()))
                }
            }

        }
    }

    override fun onDestroy() {
        unregisterReceiver(bReciever)
        super.onDestroy()
    }

}



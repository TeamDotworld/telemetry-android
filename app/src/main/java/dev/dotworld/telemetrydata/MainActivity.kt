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
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.*
import android.os.storage.StorageManager
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
import com.google.gson.Gson
import dev.dotworld.telemetrydata.utils.RootUitls
import okhttp3.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity.javaClass.simpleName

    }

    private var permissionsRequired = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,

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

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initWS()
        var batteryIntent = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryBroadcastReceiver, batteryIntent)
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
//        getMemoryDetails()
        getStorageDetails()

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

    private fun getSiMType(type: Int): String {
        when (type) {
            TelephonyManager.PHONE_TYPE_CDMA -> return "CDMA"
            TelephonyManager.PHONE_TYPE_GSM -> return "GSM"
            TelephonyManager.PHONE_TYPE_SIP -> return "SIP"

            else -> return ""
        }
    }

    private fun getLocation(): LocationDetails {
        if (locationTrackService.canGetLocation()) {
            val longitude: Double = locationTrackService.getLongitude()
            val latitude: Double = locationTrackService.getLatitude()
            Log.d(TAG, "onCreate: longitude $longitude,latitude: $latitude")
            return LocationDetails(longitude, latitude)
        }
        return LocationDetails(0.0, 0.0)
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

        Log.d(TAG, "onCreate: sensores" + sensoreList.toString())
        return sensoreDetailsList
    }

    private fun getBluetooth(): ArrayList<BluetoothDetails> {
        var bluetoothDetailsList = ArrayList<BluetoothDetails>()
        if (bluetoothAdapter == null) {
            var bluetoothDetailsList = ArrayList<BluetoothDetails>()

        } else {
            if (bluetoothAdapter.isEnabled) {
                var text = ""
                val devices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

                for (device in devices) {
//                    bluetoothText.append("""Device: ${device.name}, $device""")
                    Log.d(
                        TAG,
                        "onCreate: Device: ${device.name},address : ${device.address},device typr: ${device.type},bluetooth clas:  ${device.bluetoothClass},bondState: ${device.bondState} ,device alias: ${device.alias} "
                    )
                    val bluetoothDetails = BluetoothDetails(
                        device.name,
                        device.address,
                        device.type.toString(),
                        device.bluetoothClass.toString(),
                        device.bondState.toString()
                    )
                    bluetoothDetailsList.add(bluetoothDetails)
                }
                Log.d(TAG, "getBluetooth: $devices")

                return bluetoothDetailsList

            } else {
                return bluetoothDetailsList
            }
        }
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
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[3])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[4])
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

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("HardwareIds")
    private fun getWifi(): WifiDetails {
        val info = wifiManager.connectionInfo
        var text =
            "wifi  \n ipAddres:${info.ipAddress} \n networkId: ${info.networkId} \n speed: ${info.linkSpeed} \n ssid name: ${info.ssid}\n bssid:${info.bssid} \n mac address${info.macAddress}"
        var scanresult = wifiManager.scanResults
        var ListOFavailableWifiList = ArrayList<availableWifiList>()
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
                availableWifiList(
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

    private fun getGsm(): GsmDetails {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
//        val IMEINumber: String = gsm.getDeviceId()
//        val SIMSerialNumber: String = gsm.simSerialNumber
        val networkCountryISO: String = gsm.networkCountryIso
        val SIMCountryISO: String = gsm.simCountryIso
        val voiceMailNumber: String? = gsm.voiceMailNumber
        val softwareVersion: String? = gsm.deviceSoftwareVersion
        val simType: String = getSiMType(gsm.phoneType)

        var text =
            "GSM  \n IMEINumber: \n SIMSerialNumber:  \n networkCountryISO: ${networkCountryISO} \n " +
                    "voiceMailNumber: ${voiceMailNumber}\n SIMCountryISO:${SIMCountryISO} \n softwareVersion :${softwareVersion} \n simType:$simType"

        return GsmDetails(

//            SIMSerialNumber,
            networkCountryISO,
            voiceMailNumber,
            SIMCountryISO,
            softwareVersion,
            simType
        )
    }

    private fun sendDataToWS() {
        val mainHandler = Handler(Looper.getMainLooper())

        var loop = mainHandler.post(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {
                //logic
                if (isWebSocketOn) {
                    webSocket?.send(Gson().toJson(getWifi()))
                    webSocket?.send(Gson().toJson(getSensor()))
                    webSocket?.send(Gson().toJson(getBluetooth()))
                    webSocket?.send(Gson().toJson(getGsm()))
                    webSocket?.send(Gson().toJson(getLocation()))
                    webSocket?.send(Gson().toJson(getDeviceDetails()))
                    webSocket?.send(Gson().toJson(getMemoryDetails()))
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        webSocket?.send(Gson().toJson(getStorageDetails()))
                    }

                }


                mainHandler.postDelayed(this, 60000)
            }

        })

    }

    private fun getDeviceDetails(): DeviceDetails {
        Log.d(TAG, "getDeviceDetails: serial" + Build.SERIAL)
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
        Log.d(TAG, "getDeviceDetails:  FINGERPRINT " + Build.FINGERPRINT)
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels
        Log.d(TAG, "getDeviceDetails: os  version " + Build.VERSION.RELEASE)
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
        Log.d(TAG, "getDeviceDetails:  CPU?" + Build.CPU_ABI)
        var cpu = ""
        try {
            val DATA = arrayOf("/system/bin/cat", "/proc/cpuinfo")
            val processBuilder = ProcessBuilder(*DATA)
            val process = processBuilder.start()
            val inputStream = process.inputStream
            val byteArry = ByteArray(1024)
            while (inputStream.read(byteArry) != -1) {
                cpu += String(byteArry)
            }
            inputStream.close()

            Log.d("CPU_INFO length", "${cpu.length}")
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }

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
            cpu
        )


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
    private fun getStorageDetails() {
        getInternalavailableStorage()
        getInternalTotalStorage()
        getExternalTotalStorage()
        getExternalAvailabletorage()
        val storageManager: StorageManager = this.getSystemService<StorageManager>(
            StorageManager::class.java
        )
        val volumes = storageManager.storageVolumes
        volumes.iterator().forEach { sd ->

            Log.d(TAG, "getStorageDetails: " + sd.toString())
////            Log.d(TAG, "getStorageDetails: "+sd.isEmulated)
//            Log.d(TAG, "getStorageDetails: "+sd.isPrimary)
//            Log.d(TAG, "getStorageDetails: "+sd.isRemovable)
//            Log.d(TAG, "getStorageDetails: "+sd.mediaStoreVolumeName)
//            Log.d(TAG, "getStorageDetails: "+sd.state)
//            Log.d(TAG, "getStorageDetails: "+sd.uuid)
        }
        storageDetails(
            getInternalavailableStorage(), getInternalTotalStorage(),
            getExternalAvailabletorage(), getExternalTotalStorage()
        )
    }

    private fun getInternalavailableStorage(): String? {
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

    private fun getExternalTotalStorage(): String? {
        val path: File = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.getPath())
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        Log.d(
            TAG, "Storage: External  total " + Formatter.formatFileSize(
                this,
                totalBlocks * blockSize
            )
        )
        return Formatter.formatFileSize(this, totalBlocks * blockSize)
    }

    private fun getExternalAvailabletorage(): String? {
        val path: File = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.getPath())
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.availableBlocksLong
        Log.d(
            TAG, "Storage: External avilable " + Formatter.formatFileSize(
                this,
                totalBlocks * blockSize
            )
        )
        return Formatter.formatFileSize(this, totalBlocks * blockSize)
    }

    fun cpuTemperature(): Float {
        val process: java.lang.Process
        return try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp")
            process.waitFor()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line: String = reader.readLine()
            if (line != null) {
                val temp = line.toFloat()
                temp / 1000.0f
            } else {
                51.0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0f
        }
    }

    val batteryBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val plugged = intent.getIntExtra("plugged", -1)
                val scale = intent.getIntExtra("scale", -1)
                val health = intent.getIntExtra("health", 0)
                val status = intent.getIntExtra("status", 0)
                val rawlevel = intent.getIntExtra("level", -1)
                val voltage = intent.getIntExtra("voltage", 0)
                val temperature = intent.getIntExtra("temperature", 0)
                val technology = intent.getStringExtra("technology")
                Log.d(TAG, "onReceive:rawlevel $rawlevel ")
                Log.d(TAG, "onReceive:scale $scale ")
                var text =
                    "Battery  \n plugged:${getPluggedType(plugged)} \n technology: ${technology} \n health: ${
                        getHealth(
                            health
                        )
                    } \n status: ${getBatteryStatus(status)}\n" +
                            "  battery :${(rawlevel * 100) / scale} % \n mac voltage:${voltage} \n temperature:$temperature "
                this@MainActivity.runOnUiThread(java.lang.Runnable { batteryText.text = text })

                if (isWebSocketOn) {
                    webSocket?.send(
                        Gson().toJson(
                            BatteryDetails(
                                getPluggedType(plugged),
                                technology,
                                getHealth(health),
                                getBatteryStatus(status),
                                ((rawlevel * 100) / scale).toString(),
                                voltage.toString(),
                                temperature.toString()
                            )
                        )
                    )

                }
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

        private fun getBatteryStatus(health: Int): String {
            when (health) {
                BatteryManager.BATTERY_STATUS_CHARGING -> return "CHARGING"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> return "DISCHARGING"
                BatteryManager.BATTERY_STATUS_FULL -> return "FUll"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> return "NOT_CHARGING"
                BatteryManager.BATTERY_STATUS_UNKNOWN -> return "UNKNOWN"

                else -> return "UNKNOWN"
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(batteryBroadcastReceiver)
        super.onDestroy()
    }

    fun initWS() {
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

}
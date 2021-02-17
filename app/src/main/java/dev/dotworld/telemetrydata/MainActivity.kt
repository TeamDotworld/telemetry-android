package dev.dotworld.telemetrydata

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.*


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity.javaClass.simpleName

    }

    private var permissionsRequired = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
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
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                permissionsRequired[2]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])
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

    @SuppressLint("HardwareIds")
    private fun getWifi(): WifiDetails {
        val info = wifiManager.connectionInfo
        var text =
            "wifi  \n ipAddres:${info.ipAddress} \n networkId: ${info.networkId} \n speed: ${info.linkSpeed} \n ssid name: ${info.ssid}\n bssid:${info.bssid} \n mac address${info.macAddress}"


        return WifiDetails(
            Formatter.formatIpAddress(info.ipAddress),
            info.networkId.toString(),
            info.linkSpeed.toString(),
            info.ssid,
            info.bssid,
            info.macAddress
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
            override fun run() {
                //logic
                if (isWebSocketOn) {
                    webSocket?.send(Gson().toJson(getWifi()))
                    webSocket?.send(Gson().toJson(getSensor()))
                    webSocket?.send(Gson().toJson(getBluetooth()))
                    webSocket?.send(Gson().toJson(getGsm()))
                    webSocket?.send(Gson().toJson(getLocation()))

                }


                mainHandler.postDelayed(this, 60000)
            }

        })

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
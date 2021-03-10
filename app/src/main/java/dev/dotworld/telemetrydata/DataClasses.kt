package dev.dotworld.telemetrydata

import android.util.Log

data class WifiDetails(
    var ipAddress: String? = null,
    var networkId: String? = null,
    var linkSpeed: String? = null,
    var connectedSSID: String? = null,
    var bssid: String? = null,
    var macAddress: String? = null,
    var AvailableWifiLists: ArrayList<AvailableWifiList>? = null
)

data class AvailableWifiList(
    private var ssid: String? = null,
    private var frequency: String? = null,
    private var channelWidth: String? = null,
    private var level: String? = null,
    private var venueName: String? = null,
    private var BSSID: String? = null,
    private var capabilities: String? = null,
    private var isPasspointNetwork: Boolean? = null,
    private var timestamp: String? = null,

    )

data class GsmDetails(
//    private  var IMEINumber:String?=null, // not work in android 10
//    private  var SIMSerialNumber:String?=null, // not work in android 10
    private var networkCountryISO: String? = null,
    private var voiceMailNumber: String? = null,
    private var SIMCountryISO: String? = null,
    private var softwareVersion: String? = null,
    private var simType: String? = null,
)

data class SensoreDetails(
    var sensorName: String? = null,
    var sensorVendor: String? = null,
    var sensorVersion: String? = null,
    var sensorType: String? = null
)

data class PairedBluetoothDetails(
    private var name: String? = null,
    private var address: String? = null,
    private var type: String? = null,
    private var bluetoothClass: String? = null,
    private var bondState: String? = null,
)

data class AvailableBluetooth(
    private var name: String? = null,
    private var address: String? = null,
    private var type: String? = null,
    private var bluetoothClass: String? = null,
    private var bondState: String? = null
)

data class BluetoothDetails(
    var pairedBluetoothList: ArrayList<PairedBluetoothDetails>? = null,
    var availableBluetoothList: ArrayList<AvailableBluetooth>? = null
)

data class LocationDetails(
    private var longitude: Double = 0.0,
    private var latitude: Double = 0.0
)

data class CallDetails(
    private var incomingCallNo: String? = null
)

data class BatteryDetails(
    private var plugged: String? = null,
    private var technology: String? = null,
    private var health: String? = null,
    private var status: String? = null,
    private var level: String? = null,
    private var voltage: String? = null,
    private var temperature: String? = null,
)

data class DeviceDetails(
    var serial: String? = null,
    var model: String? = null,
    var id: String? = null,
    var manufacture: String? = null,
    var brand: String? = null,
    var board: String? = null,
    var type: String? = null,
    var user: String? = null,
    var baseVersion: String? = null,
    var sdkversion: String? = null,
    var host: String? = null,
    var fingerprint: String? = null,
    var screenResolution: String? = null,
    var osVersion: String? = null,
    var hardware: String? = null,
    var display: String? = null,
    var lastSecurityPatchDate: String? = null,
    var rooted: Boolean,
    var cpuModeName: String? = null,
    var vendor_id: String? = null,
    var cpuFamily: String? = null,
    var cpu_mhz: String? = null,
    var siblings: String? = null,
    var cache_alignment: String? = null,
    var processor: Int? = null,
    var OTGsupport: Boolean,
    var language: String? = null,
    var localTimeZone: String? = null,
    var radioVersion: String? = null,
    var glVersion: String? = null,
    var isPlayServicesAvailable: Boolean,
    var orientation: String
)

data class cameraDetails(
    private var cameraCount: Int,
    private var cameraDetails: List<FormatItem>
)

// get in mb
data class MemoryDetails(
    private var availMem: Long? = null,
    private var lowMemory: Boolean,
    private var threshold: Long? = null,
    private var totalMem: Long? = null,
    private var runTimeMaxMemory: Long? = null,
    private var runTimeTotalMemory: Long? = null,
    private var runTimeFreeMemory: Long? = null
)

data class StorageDetails(
    private var totalInternalStorage: String? = null,
    private var availableInternalStorage: String? = null,
    private var totalExternalStorage: String? = null,
    private var availableExternalStorage: String? = null,

    )

data class InstalledApps(
    private var name: String? = null,
    private var packageName: String? = null,
    private var className: String? = null,
    private var dataDir: String? = null,
)

data class FormatItem(
    val title: String,
    val cameraId: String,
    val format: Int
)

data class SoundCardDetails(
    var soundCardCount: Int,
    var name: String? = null,
    var alsa: String? = null
)

package dev.dotworld.telemetrydata

data class WifiDetails(
    var ipAddress:String?=null,
    var networkId:String?=null,
    var linkSpeed:String?=null,
    var ssid:String?=null,
    var bssid:String?=null,
    var macAddress:String?=null,

    )
data class GsmDetails(
//    private  var IMEINumber:String?=null, // not work in android 10
//    private  var SIMSerialNumber:String?=null, // not work in android 10
    private  var networkCountryISO:String?=null,
    private  var voiceMailNumber:String?=null,
    private  var SIMCountryISO:String?=null,
    private  var softwareVersion:String?=null,
    private  var simType:String?=null,
)

data class SensoreDetails(
    var sensorName:String?=null,
    var sensorVendor:String?=null,
    var sensorVersion:String?=null,
    var sensorType:String?=null)

data class BluetoothDetails(
    private var name:String?=null,
    private var address:String?=null,
    private var type:String?=null,
    private var bluetoothClass:String?=null,
    private var bondState:String?=null,
)
data class LocationDetails(
    private var getLongitude: Double = 0.0,
    private var latitude: Double = 0.0
)
data class CallDetails (
    private var incomingCallNo:String?=null
        )
data class BatteryDetails(
    private var plugged:String?=null,
    private var technology:String?=null,
    private var health:String?=null,
    private var status:String?=null,
    private var level:String?=null,
    private var voltage:String?=null,
    private var temperature:String?=null,
)
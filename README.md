# telemetry-android
An Android App written in Kotlin to get telemetry data.
 - Wifi details
    - ipAddress
    - networkId
    - linkSpeed
    - connectedSSID
    - bssid
    - macAddress
    - availableWifiLists
      - ssid
      - frequency
      - channelWidth
      - level
      - venueName
      - BSSID
      - capabilities
      - isPasspointNetwork
      - timestamp

* Battery details
    * plugged
    * technology
    * health
    * status
    * level
    * voltage
    * temperature

 * GSM details
    * networkCountryISO
    * voiceMailNumber
    * SIMCountryISO
    * softwareVersion
    * simType

 * Sensore details
    * sensorName
    * sensorVendor
    * sensorVersion
    * sensorType

 * GPS location
    * longitude
    * latitude


  * Device details
    * serial
    * model
    * id
    * manufacture
    * brand
    * board
    * type
    * user
    * baseVersion
    * sdkversion
    * host
    * fingerprint
    * screenResolution
    * osVersion
    * hardware
    * display
    * lastSecurityPatchDate
    * rooted
    * cpuModeName
    * vendor_id
    * cpu_family
    * cpu_mhz
    * siblings
    * cache_alignment
    * process
    * OTGsupport
    * language
    * localTimeZone
    * glVersion
    * isPlayServicesAvailable

* Memory details
    * availMem
    * lowMemory
    * threshold
    * totalMem
    * runTimeMaxMemory
    * runTimeTotalMemory
    * runTimeFreeMemory

* Storage details
    * totalInternalStorage
    * availableInternalStorage
    * totalExternalStorage
    * availableExternalStorage

* Installed apps
    * name
    * packageName
    * className
    * dataDir
* Camera details
    * cameraCount
    * cameraDetails
        - title
        - cameraId
        - format

* SoundCard details
    * soundCardCount
    * name
    * alsa
* Bluetooth details
    * pairedBluetoothList
        - name
        - address
        - type
        - bluetoothClass
        - bondState
    * availableBluetoothList
         - name
         - address
         - type
         - bluetoothClass
         - bondState
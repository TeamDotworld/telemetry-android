# telemetry-android
An Android App written in Kotlin to get telemetry data.

 here  given sample json formats
 
 *get wifi details*  ``` {"bssid":"f4:1d:6b:41:f6:9c","ipAddress":"192.168.1.11","linkSpeed":"433","macAddress":"02:00:00:00:00:00","networkId":"13","ssid":"Dotworld-Airtel501" } ```

 *battery details* ```  {"health":"GOOD","level":"17","plugged":"USB","status":"CHARGING","technology":"Li-ion","temperature":"317","voltage":"3730"} ```

 *get GSM details* ```  {"SIMCountryISO":"","networkCountryISO":"in","simType":"GSM","softwareVersion":"01","voiceMailNumber":""} ```

 *get sensore details*   (json array of objects formate) ``` [{"sensorName":"LIS2DS Accelerometer","sensorType":"1","sensorVendor":"STM","sensorVersion":"1"}] ```

 *get GPS location* ``` {"getLongitude":77.0161953,"latitude":11.0364109} ```

  *get incoming call number* ``` {"incomingCallNo":"********"} ```

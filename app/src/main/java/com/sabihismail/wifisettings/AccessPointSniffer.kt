package com.sabihismail.wifisettings

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.sabihismail.wifisettings.util.ContextUtil.appName
import kotlinx.coroutines.*

class AccessPointSniffer(private val context: Context) {
    val scanResults = ArrayList<ScanResult>()
    private var isBound = false

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver: BroadcastReceiver

    init {
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

                Log.i(context.appName(), "Got Wifi Scan Results, count: ${wifiManager.scanResults.size}")

                wifiManager.scanResults.forEach {
                    val ssid = it.SSID

                    if (ssid.isBlank() || scanResults.any { res -> res.SSID == ssid }) return@forEach
                    scanResults.add(it)
                }
            }
        }
    }

    fun set(checked: Boolean) {
        isBound = checked

        scanResults.clear()

        if (checked) {
            Log.i(context.appName(), "Registered Wifi Receiver")
            context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

            runBlocking {
                CoroutineScope(Dispatchers.Default).launch {
                    while (isBound) {
                        @Suppress("DEPRECATION") // technically still works as expected, although deprecated
                        wifiManager.startScan()
                        Log.i(context.appName(), "Starting Wifi Scan")

                        delay(1000 * 10)
                    }
                }
            }
        } else {
            Log.i(context.appName(), "Unregistered Wifi Receiver")

            try {
                context.unregisterReceiver(wifiScanReceiver) // no api to check if receiver already registered so try/catch
            } catch (_: IllegalArgumentException) {

            }
        }
    }
}
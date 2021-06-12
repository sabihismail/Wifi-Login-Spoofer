package com.sabihismail.wifisettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import com.sabihismail.wifisettings.util.ContextUtil.appName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccessPointSniffer(private val context: Context) {
    val scanResults = ArrayList<ScanResult>()
    private var isBound = false

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver: BroadcastReceiver

    init {
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(context.appName(), "Got Wifi Scan Results, count: ${wifiManager.scanResults.size}")

                wifiManager.scanResults.forEach {
                    if (it.SSID.isBlank() || scanResults.any { res -> res.SSID == it.SSID }) return@forEach

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

            val job = GlobalScope.launch {
                while (isBound) {
                    @Suppress("DEPRECATION") // technically still works as expected, although deprecated
                    wifiManager.startScan()
                    Log.i(context.appName(), "Starting Wifi Scan")

                    delay(1000 * 10)
                }
            }

            job.start()
        } else {
            Log.i(context.appName(), "Unregistered Wifi Receiver")

            try {
                context.unregisterReceiver(wifiScanReceiver) // no api to check if receiver already registered so try/catch
            } catch (e: IllegalArgumentException) {

            }
        }
    }
}
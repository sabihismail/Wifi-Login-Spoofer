package com.sabihismail.wifisettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

class AccessPointSniffer(private val context: Context) {
    val scanResults = ArrayList<ScanResult>()

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver: BroadcastReceiver

    init {
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    scanResults.addAll(wifiManager.scanResults)
                }
            }
        }

        wifiManager.startScan()
    }

    fun set(checked: Boolean) {
        scanResults.clear()

        if (checked) {
            context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        } else {
            context.unregisterReceiver(wifiScanReceiver)
        }
    }
}
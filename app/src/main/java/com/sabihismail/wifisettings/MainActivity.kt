package com.sabihismail.wifisettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sabihismail.wifisettings.util.ContextUtil.appName
import com.sabihismail.wifisettings.util.ContextUtil.dpToPixel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var accessPointSniffer: AccessPointSniffer
    private val lst = ArrayList<String>()

    private var clickCount = 0
    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }

        accessPointSniffer = AccessPointSniffer(this)

        val lytNetworks = findViewById<LinearLayout>(R.id.lytNetworks)
        val swtWifiEnabled = findViewById<SwitchCompat>(R.id.swtWifiEnabled)
        swtWifiEnabled.setOnClickListener {
            accessPointSniffer.set(swtWifiEnabled.isChecked)

            if (swtWifiEnabled.isChecked) {
                lytNetworks.visibility = View.VISIBLE

                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                val job = CoroutineScope(Dispatchers.Main).launch {
                    while (swtWifiEnabled.isChecked) {
                        val ssids = accessPointSniffer.scanResults.sortedBy { it.SSID }

                        if (ssids.isNotEmpty()) {
                            lytNetworks.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                .apply { setMargins(0, applicationContext.dpToPixel(10), 0, 0) }
                        }

                        ssids.forEach { scanResult ->
                            val ssid = scanResult.SSID
                            if (lst.contains(ssid)) return@forEach

                            val view = inflater.inflate(R.layout.access_point_info, lytNetworks, false)
                                .apply { findViewById<TextView>(R.id.txtSSID).text = ssid }
                                .apply {
                                    setOnClickListener {
                                        onClickAccessPoint(ssid)
                                    }
                                }

                            val imgLock = view.findViewById<ImageView>(R.id.imgLock)
                            if (getScanResultSecurity(scanResult) == OPEN) {
                                imgLock.visibility = View.INVISIBLE
                            }

                            val space = Space(applicationContext).apply {
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                    .apply { setMargins(0, 0, 0, applicationContext.dpToPixel(10)) }
                            }

                            lytNetworks.addView(view)
                            lytNetworks.addView(space)

                            lst.add(ssid)
                            Log.i(applicationContext.appName(), "Adding SSID to Layout: $ssid")
                        }

                        delay(1000)
                    }
                }

                job.start()
            } else {
                lytNetworks.visibility = View.INVISIBLE
                lytNetworks.removeAllViews()

                lst.clear()
                Log.i(applicationContext.appName(), "Removed all SSIDs")
            }
        }

        val btnActionAdditionalSettings = findViewById<RelativeLayout>(R.id.btnActionAdditionalSettings)
        btnActionAdditionalSettings.setOnClickListener {
            val time = System.currentTimeMillis()

            if (startTime == 0L || time - startTime > MAX_TIME) {
                startTime = time
                clickCount = 1
            } else {
                clickCount++
            }

            if (clickCount != MAX_CLICKS) return@setOnClickListener

            Toast.makeText(applicationContext, "Developer mode initialized.", Toast.LENGTH_SHORT).show()
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            exitProcess(0)
        }
    }

    private fun onClickAccessPoint(ssid: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
            .apply { setContentView(R.layout.password_prompt_dialog) }
            .apply { findViewById<TextView>(R.id.passwordPromptCancel)?.setOnClickListener { this.dismiss() } }
            .apply {
                val textInputView = findViewById<TextView>(R.id.txtPasswordPromptSSID)
                textInputView?.text = ssid
            }
            .apply {

                val editTextView = findViewById<TextView>(R.id.passwordPromptInput)
                setOnShowListener {
                    if (editTextView?.requestFocus() == true) {
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.showSoftInput(editTextView, InputMethodManager.SHOW_IMPLICIT)
                    }
                }

                findViewById<TextView>(R.id.passwordPromptConnect)?.setOnClickListener {
                    val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(editTextView?.text.toString())
                        .build()

                    val networkRequest = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .setNetworkSpecifier(wifiNetworkSpecifier)
                        .build()

                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkCallback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)

                            // To make sure that requests don't go over mobile data
                            connectivityManager.bindProcessToNetwork(network)
                        }

                        override fun onLost(network: Network) {
                            super.onLost(network)
                            // This is to stop the looping request for OnePlus & Xiaomi models
                            connectivityManager.bindProcessToNetwork(null)
                            connectivityManager.unregisterNetworkCallback(this)
                            // Here you can have a fallback option to show a 'Please connect manually' page with an Intent to the Wifi settings
                        }
                    }

                    connectivityManager.requestNetwork(networkRequest, networkCallback)
                }
            }

        bottomSheetDialog.show()
    }

    private fun getScanResultSecurity(scanResult: ScanResult): String {
        val cap: String = scanResult.capabilities
        val securityModes = arrayOf(WEP, WPA, WPA2, WPA_EAP, IEEE8021X)
        for (i in securityModes.indices.reversed()) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i]
            }
        }

        return OPEN
    }

    companion object {
        const val WPA2 = "WPA2"
        const val WPA = "WPA"
        const val WEP = "WEP"
        const val OPEN = "Open"
        const val WPA_EAP = "WPA-EAP"
        const val IEEE8021X = "IEEE8021X"

        const val MAX_CLICKS = 10
        const val MAX_TIME = 5 * 1000L
    }
}
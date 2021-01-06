package com.sabihismail.wifisettings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.widget.SwitchCompat
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var accessPointSniffer: AccessPointSniffer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 87)
            }
        }

        accessPointSniffer = AccessPointSniffer(this)

        val lytNetworks = findViewById<LinearLayout>(R.id.lytNetworks)

        val swtWifiEnabled = findViewById<SwitchCompat>(R.id.swtWifiEnabled)
        swtWifiEnabled.setOnClickListener {
            accessPointSniffer.set(swtWifiEnabled.isChecked)

            if (swtWifiEnabled.isChecked) {
                lytNetworks.visibility = View.VISIBLE

                while (accessPointSniffer.scanResults.isEmpty()) {
                    Thread.sleep(300)
                }


            } else {
                lytNetworks.visibility = View.INVISIBLE
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            exitProcess(0)
        }
    }
}
package com.example.mentors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mentors.BeaconsApplication.Companion.CHANNEL_ID
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val PERMISSION_REQUEST_COARSE_LOCATION = 1
    }

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val devicesAdress = HashMap<String, BluetoothDevice>()

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            when (devicesAdress.containsKey(device.address).not()) {
                true -> {
                    devicesAdress[device.address] = device
                }
                else -> {
                    setPosition(device.address)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetoothConect()

    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    @RequiresApi(Build.VERSION_CODES.M)
    private fun bluetoothConect() {

        packageManager.takeIf {
            it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }?.also {
            Toast.makeText(this, "Seu Dispositivo não suporta o BLE", Toast.LENGTH_SHORT).show()
            finish()
        }

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        when (bluetoothAdapter.isEnabled) {
            true -> {
                initBLE()
            }
            else -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun initBLE() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_COARSE_LOCATION
            )
        }
        startScanning()

    }

    fun setPosition(address: String) {
        when (address) {
            "C4:AA:52:6D:EC:C7" -> {
                beacon_entrada.visibility = GONE
                beacon_cozinha.visibility = VISIBLE
                sendNotification("Você está na Cozinha!")

            }
            "CE:F9:33:55:E1:45" -> {
                beacon_cozinha.visibility = GONE
                beacon_entrada.visibility = VISIBLE
                sendNotification("Bem Vindo!")

            }
        }
    }

    private fun startScanning() {
        AsyncTask.execute {
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                when (PackageManager.PERMISSION_GRANTED) {
                    grantResults[0] -> {
                        // Location permission granted
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun sendNotification(mensagem: String) {
        val maneger = NotificationManagerCompat.from(this)
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
        notification.setContentTitle("Mentors").setContentText(mensagem)
        notification.setTicker("Nova notificação")
            .setAutoCancel(true)
            .setColor(resources.getColor(R.color.colorBlack))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        maneger.notify(1, notification.build())

    }
}

package com.example.mentors

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ListActivity
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val SCAN_PERIOD: Long = 3000
    private var mScanning: Boolean = true
    private val handler: Handler = Handler()



    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            /* leDeviceListAdapter.addDevice(device)
             leDeviceListAdapter.notifyDataSetChanged()*/
            Log.v("", device.address)

            var bluetoothGatt: BluetoothGatt? = null

            //bluetoothGatt = device.connectGatt(this, false, gattCallback)
        }

    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetoothConect()
        scanLeDevice(mScanning)

    }

    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)


    private fun bluetoothConect() {

        packageManager.takeIf {
            it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }?.also {
            Toast.makeText(this, "Seu cll é lixo, nao funfa o ble", Toast.LENGTH_SHORT).show()
            finish()
        }

        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
    }


    fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    mScanning = false
                    bluetoothAdapter?.stopLeScan(leScanCallback)
                }, SCAN_PERIOD)
                mScanning = true
                bluetoothAdapter?.startLeScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothAdapter?.stopLeScan(leScanCallback)
            }
        }
    }

    @SuppressLint("Registered")
    class BluetoothLeService(private var bluetoothGatt: BluetoothGatt?) : Service() {

        private val TAG = BluetoothLeService::class.java.simpleName
        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2
        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        val UUID_HEART_RATE_MEASUREMENT = UUID.fromString("") //SampleGattAttributes.HEART_RATE_MEASUREMENT
        override fun onBind(p0: Intent?): IBinder? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private var connectionState = STATE_DISCONNECTED

        // Various callback methods defined by the BLE API.
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                val intentAction: String
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        intentAction = ACTION_GATT_CONNECTED
                        connectionState = STATE_CONNECTED
                        broadcastUpdate(intentAction)
                        Log.i(TAG, "Connected to GATT server.")
                        Log.i(TAG, "Attempting to start service discovery: " +
                                bluetoothGatt?.discoverServices())
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        intentAction = ACTION_GATT_DISCONNECTED
                        connectionState = STATE_DISCONNECTED
                        Log.i(TAG, "Disconnected from GATT server.")
                        broadcastUpdate(intentAction)
                    }
                }
            }

            // New services discovered
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    else -> Log.w(TAG, "onServicesDiscovered received: $status")
                }
            }

            // Result of a characteristic read operation
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                    }
                }
            }
        }

        private fun broadcastUpdate(action: String) {
            val intent = Intent(action)
            sendBroadcast(intent)
        }

        private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
            val intent = Intent(action)

            // This is special handling for the Heart Rate Measurement profile. Data
            // parsing is carried out as per profile specifications.
            when (characteristic.uuid) {
                UUID_HEART_RATE_MEASUREMENT -> {
                    val flag = characteristic.properties
                    val format = when (flag and 0x01) {
                        0x01 -> {
                            Log.d(TAG, "Heart rate format UINT16.")
                            BluetoothGattCharacteristic.FORMAT_UINT16
                        }
                        else -> {
                            Log.d(TAG, "Heart rate format UINT8.")
                            BluetoothGattCharacteristic.FORMAT_UINT8
                        }
                    }
                    val heartRate = characteristic.getIntValue(format, 1)
                    Log.d(TAG, String.format("Received heart rate: %d", heartRate))
                    intent.putExtra(EXTRA_DATA, (heartRate).toString())
                }
                else -> {
                    // For all other profiles, writes the data formatted in HEX.
                    val data: ByteArray? = characteristic.value
                    if (data?.isNotEmpty() == true) {
                        val hexString: String = data.joinToString(separator = " ") {
                            String.format("%02X", it)
                        }
                        intent.putExtra(EXTRA_DATA, "$data\n$hexString")
                    }
                }

            }
            sendBroadcast(intent)
        }

        private val gattUpdateReceiver = object : BroadcastReceiver() {

            private lateinit var bluetoothLeService: BluetoothLeService

            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action){
                    ACTION_GATT_CONNECTED -> {
//                        connected = true
//                        updateConnectionState(R.string.connected)
//                        (context as? Activity)?.invalidateOptionsMenu()
                    }
                    ACTION_GATT_DISCONNECTED -> {
//                        connected = false
//                        updateConnectionState(R.string.disconnected)
//                        (context as? Activity)?.invalidateOptionsMenu()
//                        clearUI()
                    }
                    ACTION_GATT_SERVICES_DISCOVERED -> {
                        // Show all the supported services and characteristics on the
                        // user interface.
//                        displayGattServices(bluetoothLeService.getSupportedGattServices())
                    }
                    ACTION_DATA_AVAILABLE -> {
//                        displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                    }
                }
            }
        }
    }
}

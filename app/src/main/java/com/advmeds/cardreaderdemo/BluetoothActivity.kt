package com.advmeds.cardreaderdemo

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.ble.AcsBaseCallback
import com.advmeds.cardreadermodule.acs.ble.AcsBaseDevice
import com.advmeds.cardreadermodule.acs.ble.AcsBaseDevice.AcsBleDeviceStatus
import com.advmeds.cardreadermodule.acs.ble.decoder.AcsBleTWDecoder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vise.baseble.ViseBle
import com.vise.baseble.callback.scan.IScanCallback
import com.vise.baseble.callback.scan.ScanCallback
import com.vise.baseble.model.BluetoothLeDevice
import com.vise.baseble.model.BluetoothLeDeviceStore
import pub.devrel.easypermissions.EasyPermissions

class BluetoothActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks  {

    private val scanCallback: ScanCallback by lazy { ScanCallback(object : IScanCallback {

        override fun onDeviceFound(bluetoothLeDevice: BluetoothLeDevice?) {

            bluetoothLeDevice?.name ?: return

            Log.d("IScanCallback", "onDeviceFound: $bluetoothLeDevice")

            if (bluetoothLeDevice.name.startsWith("ACR3901U-S1")) {
                ViseBle.getInstance().stopScan(scanCallback)

                acsBaseDevice.connect(this@BluetoothActivity, bluetoothLeDevice.address)
            }
        }

        override fun onScanFinish(bluetoothLeDeviceStore: BluetoothLeDeviceStore?) {}

        override fun onScanTimeout() {}
    }) }

    private val acsBaseDevice = AcsBaseDevice(arrayOf(AcsBleTWDecoder()))

    private val acsBaseCallback: AcsBaseCallback? = object : AcsBaseCallback {
        override fun onDeviceStatusChanged(status: AcsBleDeviceStatus) {
            Log.d("AcsBaseCallback", "onDeviceStatusChanged: $status")
        }

        override fun onCardPresent() {
            Log.d("AcsBaseCallback", "onCardPresent")
        }

        override fun onReceiveResult(result: Result<AcsResponseModel>) {
            Log.d("AcsBaseCallback", "onReceiveResult: $result")

            result.onSuccess {
                MaterialAlertDialogBuilder(this@BluetoothActivity)
                    .setTitle("onReceiveResult")
                    .setMessage("$result")
                    .setPositiveButton("OK", null)
                    .show()
            }.onFailure {
                MaterialAlertDialogBuilder(this@BluetoothActivity)
                    .setTitle("onReceiveResult")
                    .setMessage(it.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        override fun onCardAbsent() {
            Log.d("AcsBaseCallback", "onCardAbsent")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        acsBaseDevice.callback = acsBaseCallback
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        requestBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()

        if (scanCallback.isScanning) ViseBle.getInstance().stopScan(scanCallback)
    }

    override fun onDestroy() {
        acsBaseDevice.disconnect()

        super.onDestroy()
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (EasyPermissions.hasPermissions(this, *permissions)) {
            startScan()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "${getString(R.string.app_name)}希望能夠啟用權限來連接藍牙設備",
                0,
                *permissions
            )
        }
    }

    // 開始掃描藍牙設備
    private fun startScan() {
        if (scanCallback.isScanning) return
        ViseBle.getInstance().startScan(scanCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        startScan()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }
}
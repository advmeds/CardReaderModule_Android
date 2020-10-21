package com.advmeds.cardreaderdemo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.usb.AcsUsbCallback
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import com.advmeds.cardreadermodule.acs.usb.decoder.AcsUsbNfcTWDecoder
import com.advmeds.cardreadermodule.acs.usb.decoder.AcsUsbTWDecoder
import java.lang.Exception

class USBActivity : AppCompatActivity() {
    val USB_PERMISSION = "com.android.example.USB_PERMISSION"

    private val usbManager by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }

    private val acsUsbDevice by lazy {
        AcsUsbDevice(
            usbManager,
            arrayOf(AcsUsbTWDecoder()),
            arrayOf(AcsUsbNfcTWDecoder())
        )
    }

    private val acsUsbCallback = object : AcsUsbCallback {
        override fun onConnectDevice() {
            Log.d("AcsUsbCallback", "onConnectDevice")
        }

        override fun onFailToConnectDevice() {
            Log.d("AcsUsbCallback", "onFailToConnectDevice")
        }

        override fun onCardPresent() {
            Log.d("AcsUsbCallback", "onCardPresent")
        }

        override fun onReceiveResult(result: Result<AcsResponseModel>) {
            Log.d("AcsUsbCallback", "onReceiveResult: $result")

            AlertDialog.Builder(this@USBActivity)
                .setTitle("onReceiveResult")
                .setMessage("$result")
                .setPositiveButton("OK", null)
                .show()
        }

        override fun onCardAbsent() {
            Log.d("AcsUsbCallback", "onCardAbsent")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        acsUsbDevice.callback = acsUsbCallback

        acsUsbDevice.supportedDevice?.also { connectUSBDevice(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        acsUsbDevice.disconnect()

        try {
            unregisterReceiver(mUsbPermissionActionReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mUsbPermissionActionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                USB_PERMISSION -> {
                    val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice? ?: return

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // user choose YES for your previously popup window asking for grant perssion for this usb device
                        connectUSBDevice(usbDevice)
                    } else {
                        // user choose NO for your previously popup window asking for grant perssion for this usb device
                        Toast.makeText(context, "Please allow permission for device", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    /**
     * 向該USB裝置取得權限
     * @param device USB裝置
     */
    private fun requestUSBPermission(device: UsbDevice) {
        val filter = IntentFilter(USB_PERMISSION)

        registerReceiver(
            mUsbPermissionActionReceiver,
            filter
        )

        val mPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(USB_PERMISSION),
            0
        )

        usbManager.requestPermission(device, mPermissionIntent)
    }

    private fun connectUSBDevice(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            acsUsbDevice.connectDevice(device)
        } else {
            requestUSBPermission(device)
        }
    }
}
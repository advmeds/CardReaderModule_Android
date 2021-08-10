package com.advmeds.cardreaderdemo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.usb.AcsUsbCallback
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import com.advmeds.cardreadermodule.acs.usb.decoder.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.*

class USBActivity : AppCompatActivity() {
    companion object {
        private const val USB_PERMISSION = "${BuildConfig.APPLICATION_ID}.USB_PERMISSION"
    }

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

            MaterialAlertDialogBuilder(this@USBActivity)
                .setTitle("onReceiveResult")
                .setMessage("$result")
                .setPositiveButton("OK", null)
                .show()
        }

        override fun onCardAbsent() {
            Log.d("AcsUsbCallback", "onCardAbsent")
        }
    }

    private val detectUsbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return

            when (intent.action) {
                USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // user choose YES for your previously popup window asking for grant perssion for this usb device
                        acsUsbDevice.connectDevice(usbDevice)
                    } else {
                        // user choose NO for your previously popup window asking for grant perssion for this usb device
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Please allow permission for ${usbDevice.productName}.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    connectUSBDevice(usbDevice)

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "${usbDevice.productName}, ACTION_USB_DEVICE_ATTACHED",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (usbDevice.productId == acsUsbDevice.connectedDevice?.productId) {
                        acsUsbDevice.disconnect()
                    }

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "${usbDevice.productName}, ACTION_USB_DEVICE_DETACHED",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val filter = IntentFilter(USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerReceiver(
            detectUsbDeviceReceiver,
            filter
        )

        acsUsbDevice.callback = acsUsbCallback

        acsUsbDevice.supportedDevice?.also { connectUSBDevice(it) }
    }

    private fun connectUSBDevice(device: UsbDevice) {
        val mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(USB_PERMISSION), 0
        )

        usbManager.requestPermission(device, mPermissionIntent)
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
            unregisterReceiver(detectUsbDeviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
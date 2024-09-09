package com.advmeds.cardreaderdemo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.UsbDeviceCallback
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import com.advmeds.cardreadermodule.acs.usb.decoder.*
import com.advmeds.cardreadermodule.castles.CastlesUsbDevice
import com.advmeds.cardreadermodule.rfpro.RFProDevice
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class USBActivity : AppCompatActivity() {
    companion object {
        private const val USB_PERMISSION = "${BuildConfig.APPLICATION_ID}.USB_PERMISSION"
    }

    private val mainScroll: ScrollView by lazy { findViewById(R.id.main_scroll) }
    private val logTextView: TextView by lazy { findViewById(R.id.log_text_view) }

    private val usbManager by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }

    private val acsUsbDevice by lazy {
        AcsUsbDevice(
            usbManager,
            arrayOf(AcsUsbTWDecoder(), AcsUsbJPNDecoder(), AcsUsbThaiDecoder()),
            arrayOf(AcsUsbNfcTWDecoder(), AcsUsbNfcTWMNDDecoder())
        )
    }

    private val ezUsbDevice by lazy {
        CastlesUsbDevice(this)
    }

    private val acsUsbCallback = object : UsbDeviceCallback {
        override fun onConnectDevice() {
            appendLog("${acsUsbDevice.connectedDevice?.productName} connecting")
        }

        override fun onFailToConnectDevice() {
            appendLog("Failed to connect ${acsUsbDevice.connectedDevice?.productName}")
        }

        override fun onCardPresent() {
            appendLog("The card is present")
        }

        override fun onReceiveResult(result: Result<AcsResponseModel>) {
            result.onSuccess {
                appendLog(it.toString())
            }.onFailure {
                appendLog(it.stackTraceToString())
            }
        }

        override fun onCardAbsent() {
            appendLog("The card is absent")
        }
    }

    private val ezUsbCallCallback = object : UsbDeviceCallback {
        override fun onConnectDevice() {
            appendLog("${ezUsbDevice.connectedDevice?.productName} connecting")
        }

        override fun onFailToConnectDevice() {
            appendLog("Failed to connect ${ezUsbDevice.connectedDevice?.productName}")
        }

        override fun onCardPresent() {
            appendLog("The card is present")
        }

        override fun onReceiveResult(result: Result<AcsResponseModel>) {
            result.onSuccess {
                appendLog(it.toString())
            }.onFailure {
                appendLog(it.stackTraceToString())
            }
        }

        override fun onCardAbsent() {
            appendLog("The card is absent")
        }
    }

    private val detectUsbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return

            when (intent.action) {
                USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // user choose YES for your previously popup window asking for grant perssion for this usb device
                        when (usbDevice.productId) {
                            acsUsbDevice.supportedDevice?.productId -> {
                                acsUsbDevice.connectDevice(usbDevice)
                            }
                            ezUsbDevice.supportedDevice?.productId -> {
                                ezUsbDevice.connectDevice(usbDevice)
                            }
                        }
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
                    appendLog("${usbDevice.productName} is attached")

                    connectUSBDevice(usbDevice)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    appendLog("${usbDevice.productName} is detached")

                    when (usbDevice.productId) {
                        acsUsbDevice.connectedDevice?.productId -> {
                            acsUsbDevice.disconnect()
                        }
                        ezUsbDevice.connectedDevice?.productId -> {
                            ezUsbDevice.disconnect()
                        }
                    }
                }
            }
        }
    }

    var device: RFProDevice? = null
    private val rfUsbCallCallback = object : UsbDeviceCallback {
        override fun onConnectDevice() {
            appendLog("${device?.moduleVersion} connecting")
        }

        override fun onFailToConnectDevice() {
            appendLog("Failed to connect")
        }

        override fun onCardPresent() {
            appendLog("The card is present")
        }

        override fun onReceiveResult(result: Result<AcsResponseModel>) {
            result.onSuccess {
                appendLog(it.toString())
            }.onFailure {
                appendLog(it.stackTraceToString())
            }
        }

        override fun onCardAbsent() {
            appendLog("The card is absent")
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
        acsUsbDevice.supportedDevice?.also {
            appendLog("${it.productName} is attached")

            connectUSBDevice(it)
        }

        ezUsbDevice.callback = ezUsbCallCallback
        ezUsbDevice.supportedDevice?.also {
            appendLog("${it.productName} is attached")

            connectUSBDevice(it)
        }

        RFProDevice(applicationContext).apply {
            device = this
            callback = rfUsbCallCallback
            connect()
        }
    }

    private fun connectUSBDevice(device: UsbDevice) {
        val mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(USB_PERMISSION), 0
        )

        usbManager.requestPermission(device, mPermissionIntent)
    }

    private fun appendLog(log: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.TAIWAN)
        logTextView.append(sdf.format(Date()).plus(": ").plus(log).plus("\n"))
        mainScroll.post {
            mainScroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
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
        ezUsbDevice.disconnect()
        device?.disconnect()

        try {
            unregisterReceiver(detectUsbDeviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
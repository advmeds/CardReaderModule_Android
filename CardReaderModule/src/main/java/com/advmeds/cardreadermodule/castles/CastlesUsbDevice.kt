package com.advmeds.cardreadermodule.castles

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.util.Log
import com.advmeds.cardreadermodule.InvalidCardException
import com.advmeds.cardreadermodule.UsbDeviceCallback
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit

class CastlesUsbDevice(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val reader by lazy { CastlesUsbCardReader(usbManager) }
    private var cardIsPresent: Boolean = false
    private var timer: Timer? = null

    public var callback: UsbDeviceCallback? = null
        set(value) {
            field = WeakReference<UsbDeviceCallback>(value).get()
        }

    /** 取得可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = usbManager.deviceList.values.find { reader.isSupported(it) }

    /** 取得已連線的USB裝置 */
    public val connectedDevice: UsbDevice?
        get() = reader.connectedDevice

    /** 連線USB裝置 */
    public fun connectDevice(device: UsbDevice) {
        // 檢查當前是否已連線
        if (reader.isOpened) {
            // 若已連線則在檢查已連線的裝置是否與準備要連線的裝置相同
            if (reader.connectedDevice === device) {
                return
            }
            // 若不相同，則與已連線的裝置斷線
            disconnect()
        }

        try {
            reader.open(device)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (reader.isOpened) {
            callback?.onConnectDevice()

            val timer = Timer()
            timer.schedule(
                object : TimerTask() {
                    override fun run() {
                        if (this@CastlesUsbDevice.cardIsPresent != reader.cardIsPresented) {
                            this@CastlesUsbDevice.cardIsPresent = reader.cardIsPresented

                            if (reader.cardIsPresented) {
                                runOnMainThread {
                                    callback?.onCardPresent()
                                }

                                val response = try {
                                    val cardIsAvailable = reader.powerOn()

                                    if (cardIsAvailable) {
                                        Result.success(reader.getIDCardProfile())
                                    } else {
                                        throw InvalidCardException()
                                    }
                                } catch (e: Exception) {
                                    Log.e(this@CastlesUsbDevice::class.java.simpleName, "Failed to decode", e)
                                    Result.failure(e)
                                }

                                runOnMainThread {
                                    callback?.onReceiveResult(response)
                                }
                            } else {
                                runOnMainThread {
                                    callback?.onCardAbsent()
                                }
                            }
                        }
                    }
                },
                0,
                TimeUnit.SECONDS.toMillis(1)
            )
            this.timer = timer
        } else {
            callback?.onFailToConnectDevice()
        }
    }

    /** 斷線 */
    public fun disconnect() {
        timer?.cancel()
        timer = null
        reader.close()
        cardIsPresent = false
    }

    /** 在主執行緒執行點什麼 */
    private fun runOnMainThread(r: Runnable) = Handler(context.mainLooper).post(r)
}
package com.advmeds.cardreadermodule.acs.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.InvalidCardException
import com.advmeds.cardreadermodule.NullResponseException
import com.advmeds.cardreadermodule.UsbDeviceCallback
import com.advmeds.cardreadermodule.acs.usb.decoder.AcsUsbBaseDecoder
import java.lang.ref.WeakReference

public class AcsUsbDevice(
    private val mUsbManager: UsbManager,
    private val usbDecoders: Array<AcsUsbBaseDecoder> = emptyArray(),
    private val nfcDecoders: Array<AcsUsbBaseDecoder> = emptyArray()
) {
    companion object {
        private val stateStrings = arrayOf(
            "Unknown", "Absent", "Present", "Swallowed", "Powered", "Negotiable", "Specific"
        )
    }

    private val mReader = Reader(mUsbManager)

    public var callback: UsbDeviceCallback? = null
        set(value) {
            field = WeakReference<UsbDeviceCallback>(value).get()
        }

    /** 取得已接上USB裝置列表中第一個可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = mUsbManager.deviceList.values.find { mReader.isSupported(it) }

    /** 取得已連線的USB裝置 */
    public val connectedDevice: UsbDevice?
        get() = mReader.device

    /**
     * 是否已連線
     *
     * NOTE：目前發現若拔出已經連線成功的設備但是未呼叫 disconnect()，則該變數仍然會回傳true。
     */
    public val isConnected: Boolean
        get() = mReader.isOpened &&
                connectedDevice != null

    init {
        setupReader()
    }

    /** 設置Reader的Listener */
    private fun setupReader() {
        mReader.setOnStateChangeListener { slotNum, prevState, currState ->
            Log.d(
                mReader.readerName,
                "Slot $slotNum: ${stateStrings[prevState]} -> ${stateStrings[currState]}"
            )

            when (currState) {
                Reader.CARD_PRESENT -> { // 卡片插入
                    runOnMainThread {
                        callback?.onCardPresent()
                    }

                    val decoders = try {
                        when {
                            mReader.readerName.startsWith("ACS ACR39U") -> {
                                require(usbDecoders.isNotEmpty()) { "The USB decoders must not be empty" }
                                usbDecoders
                            }
                            mReader.readerName.startsWith("ACS ACR1281U") -> {
                                when (slotNum) {
                                    0 -> {
                                        // Smart card
                                        require(usbDecoders.isNotEmpty()) { "The USB decoders must not be empty" }
                                        usbDecoders
                                    }
                                    1 -> {
                                        // NFC card
                                        require(nfcDecoders.isNotEmpty()) { "The NFC decoders must not be empty" }
                                        nfcDecoders
                                    }
                                    else -> {
                                        return@setOnStateChangeListener
                                    }
                                }
                            }
                            mReader.readerName.startsWith("ACS ACR122U") ||
                                    mReader.readerName.startsWith("ACS ACR1251T") -> {
                                require(nfcDecoders.isNotEmpty()) { "The NFC decoders must not be empty" }
                                nfcDecoders
                            }
                            else -> {
                                val decoders = usbDecoders + nfcDecoders
                                require(decoders.isNotEmpty()) { "The USB and NFC decoders must not be empty" }
                                decoders
                            }
                        }
                    } catch (e: Exception) {
                        runOnMainThread {
                            callback?.onReceiveResult(Result.failure(e))
                        }
                        return@setOnStateChangeListener
                    }

                    val failureResults = mutableListOf<Result<AcsResponseModel>>()

                    for (decoder in decoders) {
                        val result = try {
                            val cardIsAvailable = powerOnCard(slotNum)

                            if (cardIsAvailable) {
                                Result.success(decoder.decode(mReader, slotNum))
                            } else {
                                throw InvalidCardException()
                            }
                        } catch (e: Exception) {
                            Log.e(decoder::class.java.simpleName, "Failed to decode", e)
                            Result.failure(e)
                        } finally {
                            try {
                                powerOffCard(slotNum)
                            } catch (ignored: Exception) {

                            }
                        }

                        if (result.isSuccess) {
                            runOnMainThread {
                                callback?.onReceiveResult(result)
                            }

                            return@setOnStateChangeListener
                        } else {
                            failureResults.add(result)
                        }
                    }

                    runOnMainThread {
                        callback?.onReceiveResult(
                            Result.failure(
                                NullResponseException(
                                    cause = failureResults.firstOrNull()?.exceptionOrNull()
                                )
                            )
                        )
                    }
                }
                Reader.CARD_ABSENT -> { // 卡片抽離
                    runOnMainThread {
                        callback?.onCardAbsent()
                    }
                }
            }
        }
    }

    /** 連線USB裝置 */
    public fun connectDevice(device: UsbDevice) {
        // 檢查當前是否已連線
        if (mReader.isOpened) {
            // 若已連線則在檢查已連線的裝置是否與準備要連線的裝置相同
            if (connectedDevice === device) {
                return
            }
            // 若不相同，則與已連線的裝置斷線
            mReader.close()
        }

        try {
            mReader.open(device)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (mReader.isOpened) {
            callback?.onConnectDevice()
        } else {
            callback?.onFailToConnectDevice()
        }
    }

    /** 斷線 */
    public fun disconnect() {
        mReader.close()
    }

    private fun powerOnCard(slotNum: Int): Boolean =
        mReader.power(slotNum, Reader.CARD_WARM_RESET) != null

    private fun powerOffCard(slotNum: Int): Boolean =
        mReader.power(slotNum, Reader.CARD_POWER_DOWN) != null

    /** 在主執行緒執行點什麼 */
    private fun runOnMainThread(r: Runnable) = Handler(Looper.getMainLooper()).post(r)
}
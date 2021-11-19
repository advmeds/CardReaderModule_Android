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
        public const val SMART_CARD_SLOT = 0
        public const val NFC_CARD_SLOT = 1
    }

    private val mReader = Reader(mUsbManager)

    public var callback: UsbDeviceCallback? = null
        set(value) {
            field = WeakReference<UsbDeviceCallback>(value).get()
        }

    /** 取得可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = mUsbManager.deviceList.values.find { mReader.isSupported(it) }

    /** 取得已連線的USB裝置 */
    public val connectedDevice: UsbDevice?
        get() = mReader.device

    /**
     * 是否已連線
     *
     * 目前發現連線成功後將之拔除，仍然會回傳已連線的問題。
     */
    public val isConnected: Boolean
        get() = mReader.isOpened &&
                connectedDevice != null

    init {
        setupReader()
    }

    /** 設置Reader的Listener */
    private fun setupReader() {
        mReader.setOnStateChangeListener { cardSlot, _, cardState ->
            // 部分讀卡機在連線成功後，就算沒有插入卡片仍然會觸發插入的callback，並且cardSlot為2，
            // 故過濾未知的cardSlot。
            if (cardSlot != SMART_CARD_SLOT && cardSlot != NFC_CARD_SLOT) {
                return@setOnStateChangeListener
            }

            when (cardState) {
                Reader.CARD_PRESENT -> { // 卡片插入
                    runOnMainThread {
                        callback?.onCardPresent()
                    }

                    val decoders = try {
                        when (cardSlot) {
                            SMART_CARD_SLOT -> {
                                require(usbDecoders.isNotEmpty()) { "The USB decoders must not be empty" }
                                usbDecoders
                            }
                            NFC_CARD_SLOT -> {
                                require(nfcDecoders.isNotEmpty()) { "The NFC decoders must not be empty" }
                                nfcDecoders
                            }
                            else -> emptyArray()
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
                            val cardIsAvailable = powerOnCard(cardSlot)

                            if (cardIsAvailable) {
                                Result.success(decoder.decode(mReader))
                            } else {
                                throw InvalidCardException()
                            }
                        } catch (e: Exception) {
                            Log.e(decoder::class.java.simpleName, "Failed to decode", e)
                            Result.failure(e)
                        } finally {
                            try {
                                powerOffCard(cardSlot)
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
package com.advmeds.cardreadermodule.acs.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import com.acs.smartcard.Reader
import com.acs.smartcard.ReaderException
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.EmptyResponseException
import com.advmeds.cardreadermodule.acs.NullResponseException
import com.advmeds.cardreadermodule.acs.usb.decoder.AcsUsbBaseDecoder
import java.lang.ref.WeakReference

public class AcsUsbDevice(
    _usbManager: UsbManager,
    _usbDecoders: Array<AcsUsbBaseDecoder> = arrayOf(),
    _nfcDecoders: Array<AcsUsbBaseDecoder> = arrayOf()
) {
    private val mUsbManager = _usbManager

    private val mReader = Reader(_usbManager)

    public var callback: AcsUsbCallback? = null
        set(value) { field = WeakReference<AcsUsbCallback>(value).get() }

    companion object {
        public const val SMART_CARD_SLOT = 0
        public const val NFC_CARD_SLOT = 1
    }

    private val usbDecoders: Array<AcsUsbBaseDecoder> = _usbDecoders
    private val nfcDecoders: Array<AcsUsbBaseDecoder> = _nfcDecoders

    /** 取得可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = mUsbManager.deviceList.values.firstOrNull { mReader.isSupported(it) }

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
        mReader.setOnStateChangeListener { cardType, _, cardAction ->
            var mutableCardAction = cardAction

            if (cardAction < Reader.CARD_UNKNOWN || cardAction > Reader.CARD_SPECIFIC) {
                mutableCardAction = Reader.CARD_UNKNOWN
            }

            when (mutableCardAction) {
                Reader.CARD_PRESENT -> { // 卡片插入
                    when (cardType) {
                        SMART_CARD_SLOT,
                        NFC_CARD_SLOT -> runOnMainThread { callback?.onCardPresent() }
                    }

                    try {
                        val result = when (cardType) {
                            SMART_CARD_SLOT -> powerOnSmartCard()
                            NFC_CARD_SLOT -> powerOnNFCCard()
                            else -> false
                        }

                        if (result) {
                            var response: AcsResponseModel? = null

                            val decoders = when (cardType) {
                                SMART_CARD_SLOT -> usbDecoders
                                NFC_CARD_SLOT -> nfcDecoders
                                else -> arrayOf()
                            }

                            for (acsUsbBaseDecoder in decoders) {
                                response = acsUsbBaseDecoder.decode(mReader)

                                if (response != null) {
                                    break
                                }
                            }

                            runOnMainThread {
                                val immutableResponse = response

                                if (immutableResponse == null) {
                                    callback?.onReceiveResult(Result.failure(NullResponseException()))
                                } else {
                                    if (immutableResponse.isEmpty()) {
                                        callback?.onReceiveResult(Result.failure(EmptyResponseException()))
                                    } else {
                                        callback?.onReceiveResult(Result.success(immutableResponse))
                                    }
                                }
                            }
                        }
                    } catch (e: ReaderException) {
                        runOnMainThread { callback?.onReceiveResult(Result.failure(e)) }
                    }
                }
                Reader.CARD_ABSENT -> { // 卡片抽離
                    when (cardType) {
                        SMART_CARD_SLOT,
                        NFC_CARD_SLOT -> runOnMainThread { callback?.onCardAbsent() }
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
            if (connectedDevice === device) { return }
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

    /** Power on the smart card. */
    @Throws(ReaderException::class)
    private fun powerOnSmartCard(): Boolean = try {
        powerOnCard(SMART_CARD_SLOT)
    } catch (e: ReaderException) {
        throw e
    }

    /** Power on the NFC card. */
    @Throws(ReaderException::class)
    private fun powerOnNFCCard(): Boolean = try {
        powerOnCard(NFC_CARD_SLOT)
    } catch (e: ReaderException) {
        throw e
    }

    /** Power on the card. */
    @Throws(ReaderException::class)
    private fun powerOnCard(slotNum: Int): Boolean = try {
        val atr = mReader.power(slotNum, Reader.CARD_WARM_RESET)
        atr != null
    } catch (e: ReaderException) {
        throw e
    }

    /** 在主執行緒執行點什麼 */
    private fun runOnMainThread(r: Runnable) = Handler(Looper.getMainLooper()).post(r)
}
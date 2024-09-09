package com.advmeds.cardreadermodule.rfpro

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.InvalidCardException
import com.advmeds.cardreadermodule.UsbDeviceCallback
import com.advmeds.cardreadermodule.acs.toHexString
import lc.comproCall
import timber.log.Timber
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit


class RFProDevice(private val context: Context) {
    companion object {
        private val SELECT_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x10.toByte(),
            0xD1.toByte(), 0x58.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x11.toByte(),
            0x00.toByte()
        )

        private val READ_PROFILE_APDU = byteArrayOf(
            0x00.toByte(), 0xca.toByte(), 0x11.toByte(), 0x00.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x00.toByte()
        )

        /** 是否為C2讀卡機 */
        public fun isSupported(device: UsbDevice) =
            device.vendorId == 0x0471 && device.productId == 0xA112
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var cardIsPresent: Boolean = false
    private var timer: Timer? = null

    public var callback: UsbDeviceCallback? = null
        set(value) {
            field = WeakReference<UsbDeviceCallback>(value).get()
        }

    /** 取得已接上USB裝置列表中第一個可支援的USB裝置 */
    public val supportedDevice: UsbDevice?
        get() = usbManager.deviceList.values.find { isSupported(it) }

    /** 是否已連線 */
    public val isConnected: Boolean
        get() = hdev != null

    public var moduleVersion: String? = null
        private set

    private var hdev: Int? = null
    private val slot = 0
    private var sequence = 0

    fun connect() {
        if (hdev != null) {
            return
        }

        val icdev = comproCall.lc_init_ex(2, null, 0)

        if (icdev != -1) {
            Timber.d("lc_init_ex successful")

            hdev = icdev

            val pModVer = CharArray(64)
            //try to get module version
            val getVerResult = comproCall.lc_getver(icdev, pModVer)

            if (getVerResult == 0) {
                moduleVersion = String(pModVer).replace("\u0000", "").apply {
                    Timber.d("Module Version: $this")
                }

                callback?.onConnectDevice()

                val timer = Timer()
                timer.schedule(
                    object : TimerTask() {
                        override fun run() {
                            val cardSt = ByteArray(2)
                            val getCardStateResult = comproCall.lc_iccGetCardState(icdev, slot.toShort(), cardSt)
                            val isPresented = (0 == getCardStateResult) && (cardSt[0].toInt() == 1)

                            if (this@RFProDevice.cardIsPresent != isPresented) {
                                this@RFProDevice.cardIsPresent = isPresented

                                if (isPresented) {
                                    runOnMainThread {
                                        callback?.onCardPresent()
                                    }

                                    val response = try {
                                        val pLen_byte = ByteArray(4)
                                        val rBuf = ByteArray(512)

                                        //reset cpu card
                                        val resetResult = comproCall.lc_iccGetATR(
                                            icdev,
                                            0.toShort(),
                                            rBuf,
                                            pLen_byte
                                        )

                                        if (resetResult != 0) {
                                            throw InvalidCardException()
                                        }

//                                        test(icdev)

                                        sendCommand(icdev, SELECT_APDU)
                                            .toHexString()
                                            .run {
                                                if (!endsWith("9000")) {
                                                    throw DecodeErrorException("Transmit select error")
                                                }
                                            }

                                        val response = sendCommand(icdev, READ_PROFILE_APDU)

                                        val cardNumber = response.copyOfRange(0, 12).decodeToString()
                                        val cardName = response.copyOfRange(12, 32)
                                            .filter { it != 0x00.toByte() }
                                            .toByteArray()
                                            .toString(Charset.forName("Big5"))
                                            .replace("\u0000", "") // 有些健保卡會在姓名長度不足的情況下透過"\u0000"來補字，這會造成web上顯示亂碼
                                            .trim()
                                        val cardID = response.copyOfRange(32, 42).decodeToString()
                                        val cardBirth = response.copyOfRange(42, 49).decodeToString()
                                        val cardGender = response.copyOfRange(49, 50).decodeToString()
                                        val cardIssuedDate = response.copyOfRange(50, 57).decodeToString()
                                        val birthWest = 1911 + cardBirth.substring(0..2).toInt()
                                        val issuedWest = 1911 + cardIssuedDate.substring(0..2).toInt()

                                        val birthday = AcsResponseModel.DateBean(
                                            birthWest.toString(),
                                            cardBirth.substring(3..4),
                                            cardBirth.substring(5..6)
                                        )
                                        val issuedDate = AcsResponseModel.DateBean(
                                            issuedWest.toString(),
                                            cardIssuedDate.substring(3..4),
                                            cardIssuedDate.substring(5..6)
                                        )

                                        Result.success(
                                            AcsResponseModel(
                                                cardNo = cardNumber,
                                                icId = cardID,
                                                name = cardName,
                                                gender = when (cardGender) {
                                                    "M" -> AcsResponseModel.Gender.MALE
                                                    "F" -> AcsResponseModel.Gender.FEMALE
                                                    else -> AcsResponseModel.Gender.UNKNOWN
                                                },
                                                cardType = AcsResponseModel.CardType.HEALTH_CARD,
                                                birthday = birthday,
                                                issuedDate = issuedDate
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to decode")
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
                // Firmware version get fail.
                disconnect()
                callback?.onFailToConnectDevice()
            }
        } else {
            Timber.d("lc_init_ex failure")
        }
    }

    /** 斷線 */
    public fun disconnect() {
        val icdev = hdev ?: return

        timer?.cancel()
        timer = null
        comproCall.lc_exit(icdev)
        cardIsPresent = false
        moduleVersion = null
        hdev = null
    }

    /** 測試用指令 */
    private fun test(icdev: Int): Boolean {
        val getChallenge = byteArrayOf(
            0x00,
            0x84.toByte(),
            0x00,
            0x00,
            0x08.toByte()
        )

        val response = sendCommand(icdev, getChallenge)

        return response.isNotEmpty()
    }

    private fun sendCommand(
        icdev: Int,
        command: ByteArray,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
    ): ByteArray {
        Timber.d("RFProDevice USB Card Reader transmit with - icdev: $icdev, data: ${command.toHexString()}")

        val pLen_int = IntArray(2)
        val receiveBytes = ByteArray(bufferSize)
        val result = comproCall.lc_icc_APDU(
            icdev,
            slot.toShort(),
            command.size,
            command,
            pLen_int,
            receiveBytes
        ).apply {
            Timber.d("RFProDevice USB Card Reader receive with - icdev: $icdev, result: $this")
        }

        return if (result == 0) {
            sequence = (sequence + 1) % 0xFF

            receiveBytes.copyOf(pLen_int.first()).apply {
                Timber.d("RFProDevice USB Card Reader receive with - icdev: $icdev, response: ${toHexString()}")
            }
        } else {
            byteArrayOf()
        }
    }

    /** 在主執行緒執行點什麼 */
    private fun runOnMainThread(r: Runnable) = Handler(context.mainLooper).post(r)
}
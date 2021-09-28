package com.advmeds.cardreadermodule.castles

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Handler
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.InvalidCardException
import com.advmeds.cardreadermodule.acs.toHexString
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit

public class CastlesUsbCardReader(
    private val context: Context,
    public val callback: CastlesUsbCardReaderCallback
) {
    companion object {
        private const val USB_PERMISSION = "com.advmeds.cardreadermodule.USB_PERMISSION"

        private val SELECT_APDU = byteArrayOf(
            0x00,
            0xA4.toByte(), 0x04, 0x00, 0x10.toByte(), 0xD1.toByte(), 0x58, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11, 0x00
        )

        private val READ_PROFILE_APDU = byteArrayOf(
            0x00, 0xCA.toByte(), 0x11, 0x00, 0x02, 0x00, 0x00
        )
    }

    /** 讀卡機狀態 */
    enum class ReaderState {
        /** 讀卡機尚未連接 */
        DETACHED,

        /** 讀卡機已連接 */
        ATTACHED,

        /** 讀卡機已準備就緒，就緒意指讀卡機已授權、連接並且找到資料傳輸所需介面及節點 */
        READY;
    }

    private val slot = 0
    private var sequence = 0

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connectedDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var epOut: UsbEndpoint? = null
    private var epIn: UsbEndpoint? = null

    private val detectUsbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return

            when (intent.action) {
                USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // user choose YES for your previously popup window asking for grant permission for this usb device
                        findIntfAndEpt(usbDevice)
                    } else {
                        // user choose NO for your previously popup window asking for grant permission for this usb device

                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    callback.onReaderStateChanged(ReaderState.ATTACHED)
                    findIntfAndEpt(usbDevice)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (usbDevice == connectedDevice) {
                        disconnect()
                        callback.onReaderStateChanged(ReaderState.DETACHED)
                    }
                }
            }
        }
    }

    private var timer: Timer? = null

    /** For EZUSB device */
    public val UsbDevice.isEZUSBDevice: Boolean
        get() = (vendorId == 3238 && productId == 16)

    /** 是否有插卡 */
    public var cardIsPresented: Boolean = false

    init {
        val filter = IntentFilter(USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

        context.registerReceiver(
            detectUsbDeviceReceiver,
            filter
        )

        connect()
    }

    /** 尋找介面和終端接點 */
    private fun findIntfAndEpt(usbDevice: UsbDevice) {
        // 判斷是否有權限
        if (usbManager.hasPermission(usbDevice)) {
            var usbInterface: UsbInterface? = null

            for (i in 0 until usbDevice.interfaceCount) {
                val intf = usbDevice.getInterface(i)
                usbInterface = intf
                break
            }

            val `interface` = usbInterface ?: return

            // 打開設備，獲取 UsbDeviceConnection 對象，連接設備，用於後面的通訊
            val connection = usbManager.openDevice(usbDevice) ?: return

            if (connection.claimInterface(`interface`, true)) {
                connectedDevice = usbDevice
                usbConnection = connection
                // 用UsbDeviceConnection 與 UsbInterface 進行端點設置和通訊
                for (i in 0 until `interface`.endpointCount) {
                    val usbEp = `interface`.getEndpoint(i)

                    if (usbEp.type == UsbConstants.USB_ENDPOINT_XFER_BULK && usbEp.attributes == 0x02) {
                        when (usbEp.direction) {
                            UsbConstants.USB_DIR_OUT -> {
                                epOut = usbEp
                            }
                            UsbConstants.USB_DIR_IN -> {
                                epIn = usbEp
                            }
                        }
                    }
                }

                callback.onReaderStateChanged(ReaderState.READY)

                timer?.cancel()
                timer = Timer()
                val task = object : TimerTask() {
                    override fun run() {
                        Handler(context.mainLooper).post {
                            detectionCard()
                        }
                    }
                }
                timer?.schedule(task, 0, TimeUnit.SECONDS.toMillis(1))
            } else {
                connection.close()
            }
        } else {
            val mPermissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(USB_PERMISSION), 0
            )

            usbManager.requestPermission(usbDevice, mPermissionIntent)
        }
    }

    private fun detectionCard() {
        val getHealthIDCardCmd1 = byteArrayOf(0x65)
        val isPresented = try {
            val response = sendApdu(slot, getHealthIDCardCmd1)
            response[7] != 0x42.toByte()
        } catch (e: Exception) {
            false
        }

        if (cardIsPresented != isPresented) {
            cardIsPresented = isPresented
            callback.onCardStateChanged(isPresented)

            if (isPresented) {
                getHealthIDCard()
            }
        }
    }

    private fun powerOn(): Boolean {
        val powerOnCmd = byteArrayOf(
            0x62.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            slot.toByte(),
            sequence.toByte(),
            0x00,
            0x00,
            0x00
        )

        val response = sendCommand(powerOnCmd)

        if (response.isNotEmpty()) {
            if (response[0] == 0x80.toByte() && response[7] == 0x00.toByte()) {
                val bATR = response.copyOfRange(11, response[4] - 1 + 11)
                if (bATR[0] != 0x3B.toByte() && bATR[0] != 0x3F.toByte()) {
                    // It's memory card , don't send APDU !
                    return false
                } else {
                    return true
                }
            } else if (response[0] == 0x80.toByte() && response[7] == 0x42.toByte()) {
                // No Card !
                return false
            } else if (response[0] == 0x80.toByte() && response[7] == 0x41.toByte()) {
                // Connect Card Fail !
                return false
            } else {
                // Connect Card Fail2 !
                return false
            }
        } else {
            return false
        }
    }

    private fun powerOff(): Boolean {
        val powerOnCmd = byteArrayOf(
            0x63.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            slot.toByte(),
            sequence.toByte(),
            0x00,
            0x00,
            0x00
        )

        val response = sendCommand(powerOnCmd)

        if (response.isNotEmpty()) {
            if (response[0] == 0x81.toByte() && response[7] == 0x01.toByte()) {
                // Disconnect Card OK !
                return true
            } else if (response[0] == 0x81.toByte() && response[7] == 0x02.toByte()) {
                // No Card !
                return false
            } else {
                // Disconnect Card Fail2 !
                return false
            }
        } else {
            return false
        }
    }

    private fun getHealthIDCard() {
        if (!cardIsPresented) {
            return
        }

        try {
            val cardIsAvailable = powerOn()

            if (!cardIsAvailable) {
                throw InvalidCardException()
            }

            sendApdu(slot, SELECT_APDU)
                .toHexString()
                .run {
                    if (!endsWith("9000")) {
                        throw DecodeErrorException("Transmit select error")
                    }
                }

            val response = sendApdu(slot, READ_PROFILE_APDU)
print("")
            val cardNumber = response.copyOfRange(10, 22).decodeToString()
            val cardName = response.copyOfRange(22, 42).toString(Charset.forName("Big5"))
                .replace("\u0000", "") // 有些健保卡會在姓名長度不足的情況下透過"\u0000"來補字，這會造成web上顯示亂碼
                .trim()
            val cardID = response.copyOfRange(42, 52).decodeToString()
            val cardBirth = response.copyOfRange(52, 59).decodeToString()
            val cardGender = response.copyOfRange(59, 60).decodeToString()
            val cardIssuedDate = response.copyOfRange(60, 67).decodeToString()
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

//        powerOff()

            callback.onReceiveResult(
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
            )
        } catch (e: Exception) {
            callback.onReceiveResult(
                Result.failure(e)
            )
        }
    }

    private fun sendApdu(
        slotNum: Int,
        apdu: ByteArray
    ): ByteArray {
        val spGetHealthIDCardCmd = ByteArray(apdu.size + 10)
        spGetHealthIDCardCmd[0] = 0x57
        spGetHealthIDCardCmd[1] = 0x00
        spGetHealthIDCardCmd[2] = 0x00
        spGetHealthIDCardCmd[3] = 0x00
        spGetHealthIDCardCmd[4] = apdu.size.toByte()
        spGetHealthIDCardCmd[5] = slotNum.toByte()
        spGetHealthIDCardCmd[6] = sequence.toByte()
        spGetHealthIDCardCmd[7] = 0x00
        spGetHealthIDCardCmd[8] = 0x00
        spGetHealthIDCardCmd[9] = 0x00
        apdu.copyInto(spGetHealthIDCardCmd, 10)

        return sendCommand(spGetHealthIDCardCmd)
    }

    private fun sendCommand(
        command: ByteArray,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): ByteArray {
        val connection = requireNotNull(usbConnection) { "USB Connection not found" }

        connection.bulkTransfer(epOut, command, command.size, 5000)

        val receiveBytes = ByteArray(bufferSize)
        val size = connection.bulkTransfer(epIn, receiveBytes, receiveBytes.size, 10000)

        return if (size > 0) {
            sequence = (sequence + 1) % 0xFF

            receiveBytes.copyOf(size)
        } else {
            byteArrayOf()
        }
    }

    /** 主動找尋設備並連線之 */
    public fun connect() {
        usbManager.deviceList.values.find { it.isEZUSBDevice }?.let {
            callback.onReaderStateChanged(ReaderState.ATTACHED)
            findIntfAndEpt(it)
        }
    }

    /** 斷線並清除資料 */
    public fun disconnect() {
        timer?.cancel()
        timer = null
        usbConnection?.close()
        usbConnection = null
        connectedDevice = null
        sequence = 0
        cardIsPresented = false
        epIn = null
        epOut = null
    }
}
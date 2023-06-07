package com.advmeds.cardreadermodule.castles

import android.hardware.usb.*
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.acs.toHexString
import timber.log.Timber
import java.nio.charset.Charset

public class CastlesUsbCardReader(
    private val usbManager: UsbManager
) {
    companion object {
        private val SELECT_APDU = byteArrayOf(
            0x00,
            0xA4.toByte(), 0x04, 0x00, 0x10.toByte(), 0xD1.toByte(), 0x58, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11, 0x00
        )

        private val READ_PROFILE_APDU = byteArrayOf(
            0x00, 0xCA.toByte(), 0x11, 0x00, 0x02, 0x00, 0x00
        )
    }

    private val slot = 0
    private var sequence = 0

    public var connectedDevice: UsbDevice? = null
        private set
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var epOut: UsbEndpoint? = null
    private var epIn: UsbEndpoint? = null

    public val isOpened: Boolean
        get() = usbConnection != null
    public val cardIsPresented: Boolean
        get() {
            val getHealthIDCardCmd1 = byteArrayOf(0x65)
            return try {
                val response = sendApdu(slot, getHealthIDCardCmd1)
                response[7] != 0x42.toByte()
            } catch (e: Exception) {
                false
            }
        }

    /** For EZ USB device */
    public fun isSupported(device: UsbDevice) =
        (device.vendorId == 3238 && device.productId == 16)

    public fun open(device: UsbDevice) {
        if (connectedDevice != null) {
            if (connectedDevice == device) {
                return
            }

            close()
        }

        if (!isSupported(device)) {
            throw IllegalArgumentException("The device is not supported.")
        }

        // 打開設備，獲取 UsbDeviceConnection 對象，連接設備，用於後面的通訊
        val connection = usbManager.openDevice(device) ?: throw IllegalArgumentException("Cannot open device.")

        // 尋找介面和終端接點
        var usbInterface: UsbInterface? = null

        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            usbInterface = intf
            break
        }

        val `interface` = usbInterface ?: throw IllegalArgumentException("Cannot find interface.")

        // 尋找終端接點
        val endpoints = 0.until(`interface`.endpointCount).map { `interface`.getEndpoint(it) }
        val epOut = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.attributes == 0x02 && it.direction == UsbConstants.USB_DIR_OUT }
        val epIn = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.attributes == 0x02 && it.direction == UsbConstants.USB_DIR_IN }

        if (epOut == null || epIn == null) {
            connection.close()
            throw IllegalArgumentException("Cannot find endpoints.")
        }

        if (!connection.claimInterface(`interface`, true)) {
            connection.close()
            throw IllegalArgumentException("Cannot claim interface.")
        }

        this.connectedDevice = device
        this.usbConnection = connection
        this.usbInterface = `interface`
        this.epOut = epOut
        this.epIn = epIn
    }

    public fun powerOn(): Boolean {
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

    public fun powerOff(): Boolean {
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

    public fun getIDCardProfile(): AcsResponseModel {
        sendApdu(slot, SELECT_APDU)
            .toHexString()
            .run {
                if (!endsWith("9000")) {
                    throw DecodeErrorException("Transmit select error")
                }
            }

        val response = sendApdu(slot, READ_PROFILE_APDU)

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

        return AcsResponseModel(
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
    }

    private fun sendApdu(
        slotNum: Int,
        apdu: ByteArray
    ): ByteArray {
        Timber.d("Castles USB Card Reader transmit with - slotNum: $slotNum, data: ${apdu.toHexString()}")

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

        return sendCommand(spGetHealthIDCardCmd).also {
            Timber.d("Castles USB Card Reader receive: ${it.toHexString()}")
        }
    }

    private fun sendCommand(
        command: ByteArray,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): ByteArray {
        val connection = requireNotNull(usbConnection) { "The reader is not opened." }

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

    public fun close() {
        usbConnection?.releaseInterface(usbInterface)
        usbInterface = null
        usbConnection?.close()
        usbConnection = null
        connectedDevice = null
        epOut = null
        epIn = null
        sequence = 0
    }
}
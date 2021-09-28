package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.sendControl
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice

public class AcsUsbNfcTWDecoder : AcsUsbBaseDecoder {
    companion object {
        private val READ_NFC_CARD_NO = byteArrayOf(
            0xFF.toByte(), 0xCA.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
    }

    override fun decode(reader: Reader): AcsResponseModel {
        val model = AcsResponseModel(
            cardType = CardType.STAFF_CARD
        )

        val response = reader.sendControl(
            AcsUsbDevice.NFC_CARD_SLOT,
            Reader.IOCTL_CCID_ESCAPE,
            READ_NFC_CARD_NO
        )

        val resultString = convertNfcBytesToHex(response)

        if (resultString.contains("900000") && resultString.contains("414944")) {
            val number = resultString.split("900000").first()

            model.cardNo = number
        } else if (resultString.contains("900000")) {
            val number = resultString.split("900000").first()

            if (number.length >= 8) {
                model.cardNo = number
            }
        }

        return model
    }

    private fun convertNfcBytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()

        val hexChars = CharArray(bytes.size * 2)

        for (index in bytes.indices) {
            val v = bytes[index].toInt() and 0xFF
            hexChars[index * 2] = hexArray[v ushr 4]
            hexChars[index * 2 + 1] = hexArray[v and 0x0F]
        }

        return String(hexChars)
    }
}
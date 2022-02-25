package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.sendControl
import com.advmeds.cardreadermodule.acs.toHexString

public class AcsUsbNfcTWDecoder : AcsUsbBaseDecoder {
    companion object {
        private val READ_NFC_CARD_NO = byteArrayOf(
            0xFF.toByte(), 0xCA.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
    }

    override fun decode(reader: Reader, slot: Int): AcsResponseModel {
        val model = AcsResponseModel(
            cardType = CardType.STAFF_CARD
        )

        val response = reader.sendControl(
            slot,
            Reader.IOCTL_CCID_ESCAPE,
            READ_NFC_CARD_NO
        )

        val resultString = response.toHexString()

        if (resultString.contains("9000") && resultString.contains("414944")) {
            val number = resultString.split("9000").first()

            model.cardNo = number
        } else if (resultString.contains("9000")) {
            val number = resultString.split("9000").first()

            if (number.length >= 8) {
                model.cardNo = number
            }
        }

        return model
    }
}
package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.acs.sendApdu
import com.advmeds.cardreadermodule.acs.toHexString

public class AcsUsbNfcTWMNDDecoder : AcsUsbBaseDecoder {
    companion object {
        private val READ_NFC_CARD_NO = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            0x07.toByte(),
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x79.toByte(), 0xDB.toByte(), 0x00.toByte()
        )
        private val SELECT_NFC_CARD_NO = byteArrayOf(
            0x80.toByte(), 0xA4.toByte(), 0x02.toByte(), 0x00.toByte(),
            0x02.toByte(),
            0xDB.toByte(), 0x00.toByte()
        )
        private val READ_NFC_CARD_ONLY = byteArrayOf(
            0x80.toByte(), 0x52.toByte(), 0x00.toByte(), 0x09.toByte(),
            0x02.toByte(),
            0x02.toByte(), 0x10.toByte()
        )
    }

    override fun decode(reader: Reader, slot: Int): AcsResponseModel {
        reader.sendApdu(slot, READ_NFC_CARD_NO)
            .toHexString()
            .run {
                if (!startsWith("9000")) {
                    throw DecodeErrorException("Transmit 選取國軍智慧卡程式 error")
                }
            }

        reader.sendApdu(slot, SELECT_NFC_CARD_NO)
            .toHexString()
            .run {
                if (!startsWith("9000")) {
                    throw DecodeErrorException("Transmit 選取卡號容器物件 error")
                }
            }

        val model = AcsResponseModel(
            cardType = CardType.STAFF_CARD
        )

        val response = reader.sendApdu(slot, READ_NFC_CARD_ONLY, 18)

        // Transmit 讀取唯一識別卡號 success.
        if (response.toHexString().endsWith("9000")) {
            val id = response.copyOf(response.size - 2)

            model.cardNo = id.decodeToString()
        }

        return model
    }
}
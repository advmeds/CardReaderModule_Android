package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.DecodeErrorException
import com.advmeds.cardreadermodule.acs.toHexString
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice

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

    override fun decode(reader: Reader): AcsResponseModel {
        var response = ByteArray(100)

        reader.transmit(
            AcsUsbDevice.NFC_CARD_SLOT,
            READ_NFC_CARD_NO,
            READ_NFC_CARD_NO.size,
            response,
            response.size
        )

        var responseString = response.toHexString()

        if (!responseString.startsWith("90 00")) {
            throw DecodeErrorException("Transmit 選取國軍智慧卡程式 error")
        }

        val model = AcsResponseModel(
            cardType = CardType.STAFF_CARD
        )

        response = ByteArray(100)

        reader.transmit(
            AcsUsbDevice.NFC_CARD_SLOT,
            SELECT_NFC_CARD_NO,
            SELECT_NFC_CARD_NO.size,
            response,
            response.size
        )

        responseString = response.toHexString()

        if (responseString.startsWith("90 00")) { // Transmit 選取卡號容器物件 success.
            response = ByteArray(18)

            reader.transmit(
                AcsUsbDevice.NFC_CARD_SLOT,
                READ_NFC_CARD_ONLY,
                READ_NFC_CARD_ONLY.size,
                response,
                response.size
            )

            responseString = response.toHexString()

            if (responseString.endsWith("90 00")) { // Transmit 讀取唯一識別卡號 success.
                val id = ByteArray(16)

                System.arraycopy(
                    response,
                    0,
                    id,
                    0,
                    id.size
                )

                model.cardNo = String(id)
            }
        }

        return model
    }
}
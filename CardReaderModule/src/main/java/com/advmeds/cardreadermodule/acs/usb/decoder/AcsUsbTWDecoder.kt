package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender
import com.advmeds.cardreadermodule.acs.DecodeErrorException
import com.advmeds.cardreadermodule.acs.sendApdu
import com.advmeds.cardreadermodule.acs.toHexString
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import java.nio.charset.Charset

/** 用於解析台灣健保卡 */
public class AcsUsbTWDecoder : AcsUsbBaseDecoder {
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
    }

    override fun decode(reader: Reader): AcsResponseModel {
        val activeProtocol = reader.setProtocol(
            AcsUsbDevice.SMART_CARD_SLOT,
            Reader.PROTOCOL_T1
        )

        if (activeProtocol != Reader.PROTOCOL_T1) {
            throw DecodeErrorException("The active protocol is not equal T=1")
        }

        reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, SELECT_APDU)
            .toHexString()
            .run {
                if (!startsWith("9000")) {
                    throw DecodeErrorException("Transmit select error")
                }
            }

        // Transmit: Read profile APDU
        val response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, READ_PROFILE_APDU)

        if (response.toHexString().startsWith("9000")) {
            throw DecodeErrorException("Transmit read profile error")
        }

        val cardNumber = response.copyOfRange(0, 12).decodeToString()
        val cardName = response.copyOfRange(12, 32).toString(Charset.forName("Big5"))
            .replace("\u0000", "") // 有些健保卡會在姓名長度不足的情況下透過"\u0000"來補字，這會造成web上顯示亂碼
            .trim()
        val cardID = response.copyOfRange(32, 42).decodeToString()
        val cardBirth = response.copyOfRange(42, 49).decodeToString()
        val cardGender = response.copyOfRange(49, 50).decodeToString()
        val cardIssuedDate = response.copyOfRange(50, 57).decodeToString()

        val birthWest = 1911 + cardBirth.substring(0..2).toInt()
        val issuedWest = 1911 + cardIssuedDate.substring(0..2).toInt()

        val birthday = listOf(
            birthWest,
            cardBirth.substring(3..4),
            cardBirth.substring(5..6)
        ).joinToString("-")
        val issuedDate = listOf(
            issuedWest,
            cardIssuedDate.substring(3..4),
            cardIssuedDate.substring(5..6)
        ).joinToString("-")

        return AcsResponseModel(
            cardNo = cardNumber,
            icId = cardID,
            name = cardName,
            gender = when (cardGender) {
                "M" -> Gender.MALE
                "F" -> Gender.FEMALE
                else -> Gender.UNKNOWN
            },
            cardType = CardType.HEALTH_CARD,
            birthday = birthday,
            issuedDate = issuedDate
        )
    }
}
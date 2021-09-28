package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.AcsResponseModel.Gender
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.acs.sendApdu
import com.advmeds.cardreadermodule.acs.toHexString
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import java.nio.charset.Charset

/** 用於解析泰國ID Card */
public class AcsUsbThaiDecoder : AcsUsbBaseDecoder {
    companion object {
        private val SELECT_APDU_THAI = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x08.toByte(),
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x54.toByte(),
            0x48.toByte(), 0x00.toByte(), 0x01.toByte()
        )
        private val THAI_PERSON_INFO = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x11.toByte(), 0x02.toByte(),
            0x00.toByte(), 0xD1.toByte()
        )
        private val THAI_NATIONAL_ID = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x04.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x0D.toByte()
        )
        private val GET_RESPONSE_ID = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte()
        )
        private val GET_RESPONSE_INFO = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0xD1.toByte()
        )
        private val THAI_ISSUE_EXPIRE = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x01.toByte(), 0x67.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x12.toByte()
        )
        private val GET_RESPONSE_DATE = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x12.toByte()
        )
    }

    override fun decode(reader: Reader): AcsResponseModel {
        val activeProtocol = reader.setProtocol(
            AcsUsbDevice.SMART_CARD_SLOT,
            Reader.PROTOCOL_TX
        )

        if (activeProtocol != Reader.PROTOCOL_T0) {
            throw DecodeErrorException("The active protocol is not equal T=0")
        }

        reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, SELECT_APDU_THAI)
            .toHexString()
            .run {
                if (!startsWith("61")) {
                    throw DecodeErrorException("Transmit select error")
                }
            }

        reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, THAI_NATIONAL_ID)
            .toHexString()
            .run {
                if (!startsWith("610D")) {
                    throw DecodeErrorException("Transmit read national id error")
                }
            }

        val model = AcsResponseModel(
            cardType = CardType.HEALTH_CARD
        )

        var response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_ID, 15)

        if (response.toHexString().endsWith("9000")) {
            val id = response.copyOf(response.size - 2)

            model.cardNo = id.decodeToString()
            model.icId = id.decodeToString()
        }

        response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, THAI_PERSON_INFO)

        if (response.toHexString().startsWith("61D1")) {
            response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_INFO, 211)

            if (response.toHexString().endsWith("9000")) {
                val name = response.copyOf(90)
                val nameArray = name.toString(Charset.forName("TIS620"))
                    .split("#")
                var cardName = nameArray.first()
                if (nameArray.size > 1) {
                    for (i in 1 until nameArray.size) {
                        if (nameArray[i].isNotEmpty()) {
                            cardName += " ${nameArray[i]}"
                        }
                    }
                }
                model.name = cardName.trim()

                // From Thai Year to A.D.
                val birthYear = response.copyOfRange(200, 204).decodeToString().toInt() - 543
                val birthDate = "$birthYear${response.copyOfRange(204, 208).decodeToString()}"
                model.birthday = AcsResponseModel.DateBean(
                    birthDate.substring(0..3),
                    birthDate.substring(4..5),
                    birthDate.substring(6..7)
                )

                val genderByte = response[208]
                val cardGender = when (genderByte.toInt().toChar()) {
                    '1' -> Gender.MALE
                    '2' -> Gender.FEMALE
                    else -> Gender.UNKNOWN
                }
                model.gender = cardGender
            }

            response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, THAI_ISSUE_EXPIRE)

            if (response.toHexString().startsWith("6112")) { // Transmit issued / expired date success.
                response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, GET_RESPONSE_DATE, 20)

                if (response.toHexString().endsWith("9000")) {
                    // From Thai Year to A.D.
                    val issuedYear = response.copyOf(4).decodeToString().toInt() - 543
                    val issuedDate = "$issuedYear${response.copyOfRange(4, 8).decodeToString()}"
                    // From Thai Year to A.D.
                    val expiredYear = response.copyOfRange(8, 12).decodeToString().toInt() - 543
                    val expiredDate = "$expiredYear${response.copyOfRange(12, 16).decodeToString()}"

                    model.issuedDate = AcsResponseModel.DateBean(
                        issuedDate.substring(0..3),
                        issuedDate.substring(4..5),
                        issuedDate.substring(6..7)
                    )
                    model.expiredDate = AcsResponseModel.DateBean(
                        expiredDate.substring(0..3),
                        expiredDate.substring(4..5),
                        expiredDate.substring(6..7)
                    )
                }
            }
        }

        return model
    }
}
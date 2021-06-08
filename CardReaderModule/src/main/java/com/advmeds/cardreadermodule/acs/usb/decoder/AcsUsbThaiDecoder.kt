package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.acs.ACSUtils.Companion.toHexString
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice
import java.nio.charset.Charset
import java.util.*

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

    override fun decode(reader: Reader): AcsResponseModel? {
        var responseModel: AcsResponseModel? = null

        runCatching {
            val model = AcsResponseModel()

            val activeProtocol = reader.setProtocol(
                AcsUsbDevice.SMART_CARD_SLOT,
                Reader.PROTOCOL_TX
            )

            if (activeProtocol != Reader.PROTOCOL_T0) { // Set protocol error.
                return@runCatching model
            }

            var response = ByteArray(300)

            reader.transmit(
                AcsUsbDevice.SMART_CARD_SLOT,
                SELECT_APDU_THAI,
                SELECT_APDU_THAI.size,
                response,
                response.size
            )

            var responseString = response.toHexString()

            if (!responseString.startsWith("61 ")) { // Transmit select error
                return@runCatching model
            }

            response = ByteArray(300)

            reader.transmit(
                AcsUsbDevice.SMART_CARD_SLOT,
                THAI_NATIONAL_ID,
                THAI_NATIONAL_ID.size,
                response,
                response.size
            )

            responseString = response.toHexString()

            if (responseString.startsWith("61 0D")) { // Transmit read national id success.
                response = ByteArray(15)

                reader.transmit(
                    AcsUsbDevice.SMART_CARD_SLOT,
                    GET_RESPONSE_ID,
                    GET_RESPONSE_ID.size,
                    response,
                    response.size
                )

                responseString = response.toHexString()

                if (responseString.endsWith("90 00")) {
                    val id = ByteArray(13)

                    System.arraycopy(
                        response,
                        0,
                        id,
                        0,
                        id.size
                    )

                    model.cardNo = String(id)
                    model.icId = String(id)
                }
            } else { // Transmit read national id error.
                return@runCatching model
            }

            response = ByteArray(300)

            reader.transmit(
                AcsUsbDevice.SMART_CARD_SLOT,
                THAI_PERSON_INFO,
                THAI_PERSON_INFO.size,
                response,
                response.size
            )

            responseString = response.toHexString()

            if (responseString.startsWith("61 D1")) { // Transmit personal info success.
                response = ByteArray(211)

                reader.transmit(
                    AcsUsbDevice.SMART_CARD_SLOT,
                    GET_RESPONSE_INFO,
                    GET_RESPONSE_INFO.size,
                    response,
                    response.size
                )

                responseString = response.toHexString()

                if (responseString.endsWith("90 00")) {
                    val name = ByteArray(90)
                    System.arraycopy(
                        response,
                        0,
                        name,
                        0,
                        name.size
                    )
                    val nameArray = String(name, Charset.forName("TIS620")).split("#")
                    var cardName = nameArray.first()
                    if (nameArray.size > 1) {
                        for (i in 1 until nameArray.size) {
                            if (nameArray[i].isNotEmpty()) {
                                cardName += " ${nameArray[i]}"
                            }
                        }
                    }
                    cardName = cardName.trim()
                    model.name = cardName

                    val birthYear = ByteArray(4)
                    System.arraycopy(
                        response,
                        200,
                        birthYear,
                        0,
                        birthYear.size
                    )
                    // From Thai Year to R.O.C. Year
                    val year = String(birthYear).toInt() - 2454
                    val birthDate = ByteArray(4)
                    System.arraycopy(
                        response,
                        204,
                        birthDate,
                        0,
                        birthDate.size
                    )
                    val cardBirth = String.format("%03d", year) + String(birthDate)
                    val west = 1911 + cardBirth.substring(0..2).toInt()
                    val birthday = listOf(
                        west,
                        cardBirth.substring(3..4),
                        cardBirth.substring(5..6)
                    ).joinToString("-")

                    model.birthday = birthday

                    val genderByte = response[208]
                    val cardGender = when (genderByte.toChar()) {
                        '1' -> Gender.MALE
                        '2' -> Gender.FEMALE
                        else -> Gender.UNKNOWN
                    }
                    model.gender = cardGender
                }
            } else { // Transmit personal info error.
                return@runCatching model
            }

            response = ByteArray(300)

            reader.transmit(
                AcsUsbDevice.SMART_CARD_SLOT,
                THAI_ISSUE_EXPIRE,
                THAI_ISSUE_EXPIRE.size,
                response,
                response.size
            )

            responseString = response.toHexString()

            if (responseString.startsWith("61 12")) { // Transmit issued / expired date success.
                response = ByteArray(20)

                reader.transmit(
                    AcsUsbDevice.SMART_CARD_SLOT,
                    GET_RESPONSE_DATE,
                    GET_RESPONSE_DATE.size,
                    response,
                    response.size
                )

                responseString = response.toHexString()

                if (responseString.endsWith("90 00")) {
                    // Issued date
                    val yearArray = ByteArray(4)

                    System.arraycopy(
                        response,
                        0,
                        yearArray,
                        0,
                        yearArray.size
                    )

                    // From Thai Year to A.D.
                    var year = String(yearArray).toInt() - 543

                    val dateArray = ByteArray(4)

                    System.arraycopy(
                        response,
                        4,
                        dateArray,
                        0,
                        dateArray.size
                    )

                    var cardIssuedDate = "$year${String(dateArray)}"
                    System.arraycopy(
                        response,
                        8,
                        yearArray,
                        0,
                        yearArray.size
                    )
                    // From Thai Year to A.D.
                    year = String(yearArray).toInt() - 543
                    System.arraycopy(
                        response,
                        12,
                        dateArray,
                        0,
                        dateArray.size
                    )
                    cardIssuedDate += "/$year${String(dateArray)}"

                    val issuedDate = listOf(
                        cardIssuedDate.substring(0..3),
                        cardIssuedDate.substring(4..5),
                        cardIssuedDate.substring(6..7)
                    ).joinToString("-")
                    val expiredDate = listOf(
                        cardIssuedDate.substring(9..12),
                        cardIssuedDate.substring(13..14),
                        cardIssuedDate.substring(15..16)
                    ).joinToString("-")

                    model.issuedDate = issuedDate
                    model.expiredDate = expiredDate
                    model.cardType = CardType.HEALTH_CARD
                }
            }

            model
        }.onSuccess {
            responseModel = it
        }.onFailure {
            it.printStackTrace()
            responseModel = null
        }

        return responseModel
    }
}
package com.advmeds.cardreadermodule.acs.ble.decoder

import com.acs.bluetooth.BluetoothReader
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender
import com.advmeds.cardreadermodule.acs.toHexString

@Deprecated("Don't Use! This Decoder not correctly work.")
public class AcsBleThaiDecoder : AcsBleBaseDecoder {
    companion object {
        private val APDU_1 = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x08.toByte(),
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x54.toByte(),
            0x48.toByte(), 0x00.toByte(), 0x01.toByte()
        )

        private val APDU_2 = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x04.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x0D.toByte()
        )

        private val APDU_3 = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte()
        )

        private val APDU_4 = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x11.toByte(), 0x02.toByte(),
            0x00.toByte(), 0xD1.toByte()
        )

        private val APDU_5 = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0xD1.toByte()
        )

        private val APDU_6 = byteArrayOf(
            0x80.toByte(), 0xB0.toByte(), 0x01.toByte(), 0x67.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x12.toByte()
        )

        private val APDU_7 = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x12.toByte()
        )
    }

    private var commandPointer: ByteArray? = null

    private var cardNumber = ""
    private var cardName = ""
    private var cardID = ""
    private var cardBirth: AcsResponseModel.DateBean? = null
    private var cardGender = Gender.UNKNOWN

    private fun reset() {
        commandPointer = null

        cardNumber = ""
        cardName = ""
        cardID = ""
        cardBirth = null
        cardGender = Gender.UNKNOWN
    }

    private fun sendCommand(reader: BluetoothReader, command: ByteArray): Boolean {
        // Transmit APDU command.
        if (!reader.transmitApdu(APDU_1)) {
            reset()
            return false
        }

        commandPointer = command

        return true
    }

    override fun start(reader: BluetoothReader) {
        reset()
        sendCommand(reader, APDU_1)
    }

    override fun decode(reader: BluetoothReader, apdu: ByteArray): AcsResponseModel? {
        val response = apdu.toHexString()

        if (commandPointer === APDU_1) {
            if (response.startsWith("61")) {
                if (!sendCommand(reader, APDU_2)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_2) {
            if (response.startsWith("610D")) {
                if (!sendCommand(reader, APDU_3)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_3) {
            if (response.endsWith("9000")) {
                val id = apdu.copyOf(13)
                cardNumber = id.decodeToString()
                cardID = id.decodeToString()

                if (!sendCommand(reader, APDU_4)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_4) {
            if (response.startsWith("61D1")) {
                if (!sendCommand(reader, APDU_5)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_5) {
            if (response.endsWith("9000")) {
                val name = apdu.copyOfRange(100, 200)
                val nameArray = name.decodeToString().split("#")
                cardName = nameArray.first()
                if (nameArray.size > 1) {
                    for (i in 1 until nameArray.size) {
                        if (nameArray[i].isNotEmpty()) {
                            cardName += " ${nameArray[i]}"
                        }
                    }
                }
                cardName = cardName.trim()

                val birthYear = apdu.copyOfRange(200, 204).decodeToString().toInt() - 543
                // From Thai Year to R.O.C. Year
                val birthDate = "$birthYear${apdu.copyOfRange(204, 208).decodeToString()}"
                cardBirth = AcsResponseModel.DateBean(
                    birthDate.substring(0..3),
                    birthDate.substring(4..5),
                    birthDate.substring(6..7)
                )

                val genderByte = apdu[208]
                val genderChar = genderByte.toInt().toChar()
                this.cardGender = when (genderChar) {
                    '1' -> Gender.MALE
                    '2' -> Gender.FEMALE
                    else -> Gender.UNKNOWN
                }

                if (!sendCommand(reader, APDU_6)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_6) {
            if (response.startsWith("6112")) {
                if (!sendCommand(reader, APDU_7)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === APDU_7) {
            if (response.endsWith("9000")) {
                // From Thai Year to A.D.
                val issuedYear = apdu.copyOf(4).decodeToString().toInt() - 543
                val issuedDate = "$issuedYear${apdu.copyOfRange(4, 8).decodeToString()}"

                // From Thai Year to A.D.
                val expiredYear = apdu.copyOfRange(8, 12).decodeToString().toInt() - 543
                val expiredDate = "$expiredYear${apdu.copyOfRange(12, 16).decodeToString()}"

                return AcsResponseModel(
                    cardNumber,
                    cardID,
                    cardName,
                    cardGender,
                    CardType.HEALTH_CARD,
                    cardBirth,
                    AcsResponseModel.DateBean(
                        issuedDate.substring(0..3),
                        issuedDate.substring(4..5),
                        issuedDate.substring(6..7)
                    ),
                    AcsResponseModel.DateBean(
                        expiredDate.substring(0..3),
                        expiredDate.substring(4..5),
                        expiredDate.substring(6..7)
                    )
                )
            } else {
                reset()
                return null
            }
        } else {
            reset()

            return null
        }

        return AcsResponseModel()
    }
}
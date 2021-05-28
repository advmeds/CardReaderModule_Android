package com.advmeds.cardreadermodule.acs.ble.decoder

import com.acs.bluetooth.BluetoothReader
import com.advmeds.cardreadermodule.acs.ACSUtils.Companion.toHexString
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender

@Deprecated("Don't Use! This Decoder not correctly work.")
public class AcsBleThaiDecoder : AcsBleBaseDecoder {
    private val apduCommand1 = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x08.toByte(),
        0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x54.toByte(),
        0x48.toByte(), 0x00.toByte(), 0x01.toByte()
    )

    private val apduCommand2 = byteArrayOf(
        0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x04.toByte(), 0x02.toByte(),
        0x00.toByte(), 0x0D.toByte()
    )

    private val apduCommand3 = byteArrayOf(
        0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte()
    )

    private val apduCommand4 = byteArrayOf(
        0x80.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x11.toByte(), 0x02.toByte(),
        0x00.toByte(), 0xD1.toByte()
    )

    private val apduCommand5 = byteArrayOf(
        0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0xD1.toByte()
    )

    private val apduCommand6 = byteArrayOf(
        0x80.toByte(), 0xB0.toByte(), 0x01.toByte(), 0x67.toByte(), 0x02.toByte(),
        0x00.toByte(), 0x12.toByte()
    )

    private val apduCommand7 = byteArrayOf(
        0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x12.toByte()
    )

    private var commandPointer: ByteArray? = null

    private var cardNumber = ""
    private var cardName = ""
    private var cardID = ""
    private var cardBirth = ""
    private var cardGender = Gender.UNKNOWN
    private var cardIssuedDate = ""

    private fun reset() {
        commandPointer = null

        cardNumber = ""
        cardName = ""
        cardID = ""
        cardBirth = ""
        cardGender = Gender.UNKNOWN
        cardIssuedDate = ""
    }

    private fun sendCommand(reader: BluetoothReader, command: ByteArray): Boolean {
        // Transmit APDU command.
        if (!reader.transmitApdu(apduCommand1)) {
            reset()
            return false
        }

        commandPointer = command

        return true
    }

    override fun start(reader: BluetoothReader) {
        reset()
        sendCommand(reader, apduCommand1)
    }

    override fun decode(reader: BluetoothReader, apdu: ByteArray): AcsResponseModel? {
        val response = apdu.toHexString()

        if (commandPointer === apduCommand1) {
            if (response.startsWith("61 ")) {
                if (!sendCommand(reader, apduCommand2)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand2) {
            if (response.startsWith("61 0D")) {
                if (!sendCommand(reader, apduCommand3)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand3) {
            if (response.endsWith("90 00")) {
                val id = ByteArray(13)
                System.arraycopy(apdu, 0, id, 0, id.size)
                cardNumber = String(id)
                cardID = String(id)

                if (!sendCommand(reader, apduCommand4)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand4) {
            if (response.startsWith("61 D1")) {
                if (!sendCommand(reader, apduCommand5)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand5) {
            if (response.endsWith("90 00")) {
                val name = ByteArray(100)
                System.arraycopy(apdu, 100, name, 0, name.size)
                val nameArray = String(name).split("#")
                cardName = nameArray.first()
                if (nameArray.size > 1) {
                    for (i in 1 until nameArray.size) {
                        if (nameArray[i].isNotEmpty()) {
                            cardName += " ${nameArray[i]}"
                        }
                    }
                }
                cardName = cardName.trim()

                val birthYear = ByteArray(4)
                System.arraycopy(apdu, 200, birthYear, 0, birthYear.size)
                // From Thai Year to R.O.C. Year
                val year = String(birthYear).toInt() - 2454
                val birthDate = ByteArray(4)
                System.arraycopy(apdu, 204, birthDate, 0, birthDate.size)
                cardBirth = String.format("%03d", year) + String(birthDate)

                val genderByte = apdu[208]
                val genderChar = genderByte.toChar()
                this.cardGender = when (genderChar) {
                    '1' -> Gender.MALE
                    '2' -> Gender.FEMALE
                    else -> Gender.UNKNOWN
                }

                if (!sendCommand(reader, apduCommand6)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand6) {
            if (response.startsWith("61 12")) {
                if (!sendCommand(reader, apduCommand7)) return null
            } else {
                reset()
                return null
            }
        } else if (commandPointer === apduCommand7) {
            if (response.endsWith("90 00")) {
                val yearArray = ByteArray(4)
                System.arraycopy(apdu, 0, yearArray, 0, yearArray.size)
                // From Thai Year to A.D.
                var year = String(yearArray).toInt() - 543
                val dateArray = ByteArray(4)
                System.arraycopy(apdu, 4, dateArray, 0, dateArray.size)
                cardIssuedDate = "$year${String(dateArray)}"

                // Expired date
                System.arraycopy(apdu, 8, yearArray, 0, yearArray.size)
                // From Thai Year to A.D.
                year = String(yearArray).toInt() - 543
                System.arraycopy(apdu, 12, dateArray, 0, dateArray.size)
                // [Issued]/[Expired], 20140512/20230324
                cardIssuedDate += "/" + year + String(dateArray)

                val birthYear = 1911 + cardBirth.substring(0..2).toInt()
                val birthMonth = cardBirth.substring(3..4)
                val birthDay = cardBirth.substring(5..6)
                val issuedYear = cardIssuedDate.substring(0..3)
                val issuedMonth = cardIssuedDate.substring(4..5)
                val issuedDay = cardIssuedDate.substring(6..7)
                val expiredYear = cardIssuedDate.substring(9..12)
                val expiredMonth = cardIssuedDate.substring(13..14)
                val expiredDay = cardIssuedDate.substring(15..16)
                val birthday = listOf(
                    birthYear,
                    birthMonth,
                    birthDay
                ).joinToString("-")
                val issuedDate = listOf(
                    issuedYear,
                    issuedMonth,
                    issuedDay
                ).joinToString("-")
                val expiredDate = listOf(
                    expiredYear,
                    expiredMonth,
                    expiredDay
                ).joinToString("-")

                return AcsResponseModel(
                    cardNumber,
                    cardID,
                    cardName,
                    cardGender,
                    CardType.HEALTH_CARD,
                    birthday,
                    issuedDate,
                    expiredDate
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
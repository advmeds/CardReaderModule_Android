package com.advmeds.cardreadermodule.acs.ble.decoder

import com.acs.bluetooth.BluetoothReader
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender
import com.advmeds.cardreadermodule.acs.toHexString
import java.nio.charset.Charset

public class AcsBleTWDecoder : AcsBleBaseDecoder {
    companion object {
        private val APDU_1 = byteArrayOf(
            0x00.toByte(),
            0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x10.toByte(),
            0xD1.toByte(), 0x58.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x11.toByte(), 0x00.toByte()
        )

        private val APDU_2 = byteArrayOf(
            0x00.toByte(), 0xca.toByte(), 0x11.toByte(),
            0x00.toByte(), 0x02.toByte(), 0x00.toByte(), 0x00.toByte()
        )
    }

    override fun start(reader: BluetoothReader) {
        reader.transmitApdu(APDU_1)
    }

    override fun decode(reader: BluetoothReader, apdu: ByteArray): AcsResponseModel? {
        val response = apdu.toHexString()

        if (response.startsWith("9000")) {
            return if (!reader.transmitApdu(APDU_2)) {
                null
            } else {
                AcsResponseModel()
            }
        } else if (response.contains("9000") && response.split("9000").size > 1) {
            val cardNumber = apdu.copyOfRange(0, 12).decodeToString()
            val cardName = apdu.copyOfRange(12, 32).toString(Charset.forName("Big5"))
                .replace("\u0000", "") // 有些健保卡會在姓名長度不足的情況下透過"\u0000"來補字，這會造成web上顯示亂碼
                .trim()
            val cardID = apdu.copyOfRange(32, 42).decodeToString()
            val cardBirth = apdu.copyOfRange(42, 49).decodeToString()
            val cardGender = apdu.copyOfRange(49, 50).decodeToString()
            val cardIssuedDate = apdu.copyOfRange(50, 57).decodeToString()
            val birthYear = 1911 + cardBirth.substring(0..2).toInt()
            val birthMonth = cardBirth.substring(3..4)
            val birthDay = cardBirth.substring(5..6)
            val issuedYear = 1911 + cardIssuedDate.substring(0..2).toInt()
            val issuedMonth = cardIssuedDate.substring(3..4)
            val issuedDay = cardIssuedDate.substring(5..6)
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

            val gender = when(cardGender) {
                "M" -> Gender.MALE
                "F" -> Gender.FEMALE
                else -> Gender.UNKNOWN
            }

            return AcsResponseModel(
                cardNumber,
                cardID,
                cardName,
                gender,
                CardType.HEALTH_CARD,
                birthday,
                issuedDate,
                null
            )
        } else {
            return null
        }
    }
}
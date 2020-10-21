package com.advmeds.cardreadermodule.acs.ble.decoder

import com.acs.bluetooth.BluetoothReader
import com.advmeds.cardreadermodule.acs.ACSUtils.Companion.toHexString
import com.advmeds.cardreadermodule.acs.AcsResponseModel
import com.advmeds.cardreadermodule.acs.AcsResponseModel.CardType
import com.advmeds.cardreadermodule.acs.AcsResponseModel.Gender
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.text.ParseException

public class AcsBleTWDecoder : AcsBleBaseDecoder {
    private val apduCommand1 = byteArrayOf(
        0x00.toByte(),
        0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x10.toByte(),
        0xD1.toByte(), 0x58.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x11.toByte(), 0x00.toByte()
    )

    private val apduCommand2 = byteArrayOf(
        0x00.toByte(), 0xca.toByte(), 0x11.toByte(),
        0x00.toByte(), 0x02.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    override fun start(reader: BluetoothReader) {
        reader.transmitApdu(apduCommand1)
    }

    override fun decode(reader: BluetoothReader, apdu: ByteArray): AcsResponseModel? {
        val response = apdu.toHexString()

        if (response.startsWith("90 00")) {
            return if (!reader.transmitApdu(apduCommand2)) {
                null
            } else {
                AcsResponseModel()
            }
        } else if (
            response.contains("90 00") &&
            response.split("90 00").size > 1
        ) {
            val cardNumber = String(apdu.copyOfRange(0, 12))
            val cardName = String(apdu.copyOfRange(12, 32), Charset.forName("Big5")).trim()
            val cardID = String(apdu.copyOfRange(32, 42))
            val cardBirth = String(apdu.copyOfRange(42, 49))
            val cardGender = String(apdu.copyOfRange(49, 50))
            val cardIssuedDate = String(apdu.copyOfRange(50, 57))

            val sdf = SimpleDateFormat("yyyy/MM/dd")

            val birthWest = 1911 + cardBirth.substring(0..2).toInt()
            val issuedWest = 1911 + cardIssuedDate.substring(0..2).toInt()

            var birthday: Date? = null
            var issuedDate: Date? = null

            try {
                birthday = sdf.parse("$birthWest/${cardBirth.substring(3..4)}/${cardBirth.substring(5..6)}")
                issuedDate = sdf.parse("$issuedWest/${cardIssuedDate.substring(3..4)}/${cardIssuedDate.substring(5..6)}")
            } catch (e: ParseException) {
                e.printStackTrace()
            }

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
                if (birthday == null) null else Date(birthday.time),
                if (issuedDate == null) null else Date(issuedDate.time),
                null
            )
        } else {
            return null
        }
    }
}
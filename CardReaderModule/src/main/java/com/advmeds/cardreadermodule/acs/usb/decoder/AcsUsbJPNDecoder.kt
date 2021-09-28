package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel
import com.advmeds.cardreadermodule.DecodeErrorException
import com.advmeds.cardreadermodule.acs.sendApdu
import com.advmeds.cardreadermodule.acs.toHexString
import com.advmeds.cardreadermodule.acs.usb.AcsUsbDevice

/** 用於解析馬來西亞ID Card */
open class AcsUsbJPNDecoder : AcsUsbBaseDecoder {
    companion object {
        private val SELECT_JPN_APPLICATION = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x0A.toByte(),
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x74.toByte(),
            0x4A.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x00.toByte(), 0x10.toByte()
        )

        private val SELECT_APPLICATION_GET_RESPONSE = byteArrayOf(
            0x00.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte()
        )

        private val SET_LENGTH = byteArrayOf(
            0xC8.toByte(), 0x32.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte(),
            0x08.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        private val SELECT_INFO = byteArrayOf(
            0xCC.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x08.toByte()
        )

        private val READ_INFO = byteArrayOf(
            0xCC.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        private val JPN = arrayOf(
            byteArrayOf(
                0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ), //just an empty so can get JPN[1] for JPN_1_1, look prettier
            byteArrayOf(
                0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ),
            byteArrayOf(
                0x02.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ),
            byteArrayOf(
                0x03.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ),
            byteArrayOf(
                0x04.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ),
            byteArrayOf(
                0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            ),
            byteArrayOf(
                0x06.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()
            )
        )
    }

    private enum class ProfileColumn {
        KPT_NAME,
        IC_NO,
        GENDER,
        DATE_OF_BIRTH,
        ISSUED_DATE;

        val jpn: ByteArray
            get() = JPN[1]

        val length: Byte
            get() = when (this) {
                KPT_NAME -> 0xC8
                IC_NO -> 0x0D
                GENDER -> 0x01
                DATE_OF_BIRTH -> 0x04
                ISSUED_DATE -> 0x04
            }.toByte()

        val offset: ByteArray
            get() = when (this) {
                KPT_NAME -> byteArrayOf(0x00.toByte(), 0x00.toByte())
                IC_NO -> byteArrayOf(0x11.toByte(), 0x01.toByte())
                GENDER -> byteArrayOf(0x1E.toByte(), 0x01.toByte())
                DATE_OF_BIRTH -> byteArrayOf(0x27.toByte(), 0x01.toByte())
                ISSUED_DATE -> byteArrayOf(0x44.toByte(), 0x01.toByte())
            }
    }

    override fun decode(reader: Reader): AcsResponseModel {
        reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, SELECT_JPN_APPLICATION)
            .toHexString()
            .run {
                if (!endsWith("6105")) {
                    throw DecodeErrorException("Transmit select jpn application error")
                }
            }

        reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, SELECT_APPLICATION_GET_RESPONSE)
            .toHexString()
            .run {
                if (!endsWith("9000")) {
                    throw DecodeErrorException("Transmit select application get response error")
                }
            }

        val model = AcsResponseModel(
            cardType = AcsResponseModel.CardType.HEALTH_CARD
        )

        model.cardNo = readInfo(reader, ProfileColumn.IC_NO, true)
        model.icId = readInfo(reader, ProfileColumn.IC_NO, true)
        val name = readInfo(reader, ProfileColumn.KPT_NAME, true)
            .replace("\u0001", "") // 刪除開頭的 
            .replace("\u0004", "") // 刪除開頭的 
            .replace("\$", "") // 刪除開頭的 $
        var endIndex = 0
        for (i in name.indices) {
            if (name.getOrNull(i) == '\u0020' && name.getOrNull(i + 1) == '\u0020') {
                endIndex = i
                break
            }
        }
        model.name = name.substring(0, endIndex)
        model.gender = when (readInfo(reader, ProfileColumn.GENDER, true)) {
            "M", "L" -> AcsResponseModel.Gender.MALE
            "F", "P" -> AcsResponseModel.Gender.FEMALE
            else -> AcsResponseModel.Gender.UNKNOWN
        }
        val cardBirth = readInfo(reader, ProfileColumn.DATE_OF_BIRTH, false).trim()
        model.birthday = AcsResponseModel.DateBean(
            cardBirth.substring(0..3),
            cardBirth.substring(4..5),
            cardBirth.substring(6..7)
        )
        val cardIssued = readInfo(reader, ProfileColumn.ISSUED_DATE, false).trim()
        model.issuedDate = AcsResponseModel.DateBean(
            cardIssued.substring(0..3),
            cardIssued.substring(4..5),
            cardIssued.substring(6..7)
        )

        return model
    }

    private fun readInfo(reader: Reader, column: ProfileColumn, convert: Boolean): String {
        val setLengthCmd = SET_LENGTH
            .plus(column.length)
            .plus(0x00.toByte())

        var respondStr = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, setLengthCmd).toHexString()

        if (!respondStr.endsWith("9108")) {
            return ""
        }

        val selectInfoCmd = SELECT_INFO
            .plus(column.jpn)
            .plus(column.offset)
            .plus(column.length)
            .plus(0x00.toByte())

        respondStr = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, selectInfoCmd).toHexString()

        val readInfoCmd = READ_INFO
            .plus(column.length)

        val response = reader.sendApdu(AcsUsbDevice.SMART_CARD_SLOT, readInfoCmd)

        respondStr = response.toHexString()

        if (!respondStr.endsWith("9000")) {
            return ""
        }

        val responseWithoutCheckCode = response.copyOf(response.size - 2)

        return if (convert) {
            String(responseWithoutCheckCode).trim()
        } else {
            responseWithoutCheckCode.toHexString()
        }
    }
}

/* reference from https://github.com/SamphorsKhlok/Ionic-plugin-MyKadReader/blob/master/cordova-plugin-MyKadReader/src/android/MyKad_JPN.java
jpn-1-1
Offset  Length  Length  SDK Function Name        Description
       (Hex)   (Dec)
0000     03        3                          01 04 24
0003     96      150    JPN_OrgName              original name   //those slot dun have info C8  00 00 read starting from 0 and length 200
0099     50   30+30+20  JPN_GMPCName             GMPC name
00E9     28     20+20   JPN_KPTName              KPT name
0111     0D       13    JPN_IDNum                ID number
011E     01        1    JPN_Gender               gender
011F     08        8    JPN_OldIDNum             old ID number
0127     04        4    JPN_BirthDate            date of birth
012B     19       25    JPN_BirthPlace           place of birth
0144     04        4    JPN_DateIssued           date issued
0148     12       18    JPN_Citizenship          citizenship
015A     19       25    JPN_Race                 race
0173     0B       11    JPN_Religion             religion
017E     01        1    JPN_EastMalaysian        East Malaysian
017F     02        2    JPN_RJ                   RJ?
0181     02        2    JPN_KT                   KT?
0183     0B       11    JPN_OtherID              other ID
018E     01        1    JPN_Category             category
018F     01        1    JPN_CardVer              card version
0190     04        4    JPN_GreenCardExpiry      green card expiry date
0194     14       20    JPN_GreenCardNationality green card nationality
01A8     23       35                             All 00
jpn-1-2
0000     03        3                             01 40 03
0003    FA0     4000    JPN_Photo                JPEG photo
0FA3     08        8                             All 00
jpn-1-3
0000     03        3                             01 12 03
0003     14       20                             "R1L1",0,0...
0017    256      598    JPN_Thumb1               thumprint 1 (right thumb)
026D    256      598    JPN_Thumb2               thumprint 2 (left thumb)
04C3     08        8                             All 00
jpn-1-4
0000     03        3                             01 01 52
0003     1E       30    JPN_Address1             address line 1
0021     1E       30    JPN_Address2             address line 2
003F     1E       30    JPN_Address3             address line 3
005D     03        3    JPN_Postcode             postcode
0060     19       25    JPN_City                 city
0079     1E       30    JPN_State                state
0097     14       20                             FF 00 00...
jpn-1-5
0000     03        3                             01 12 00
0003     09        9    JPN_SocsoNum             socso number
000C     1F       31                             All 00
jpn-1-6
0000     03        3                             01 17 00
0003     0A       10    JPN_Locality             locality
000D     1E       30                             All 00
jpj-1-1
Offset  Length  Length  SDK Function Name        Description
       (Hex)   (Dec)
0000     03        3                             01 04 16
0003     01        1    JPJ_OwnerCategory        owner category
0004     0C       12    JPJ_LicenseType          licence type
0010     1E       30    JPJ_VehicleClass         vehicle class
002E     06        6    JPJ_PSVUsage             PSV usage
0034     96      150    JPJ_PSVDesc              PSV description
00CA     06        6    JPJ_GDLUsage             GDL usage
00D0     96      150    JPJ_GDLDesc              GDL description
0166     20       32    JPJ_ValidityPeriod       validity period
0186     14       20    JPJ_HandicappedReg       handicapped registration
019A     01        1    JPJ_KejaraPoints         kejara points
019B     01        1    JPJ_SuspensionNum        suspension number
019C     04        4    JPJ_LastKejaraUpdate     last kejara update
01A0     0B       11                             All 00
 */
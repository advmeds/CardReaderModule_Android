package com.advmeds.cardreadermodule.acs

import com.acs.smartcard.Reader
import kotlin.experimental.xor

/** Converts the byte array to HEX string. */
fun ByteArray.toHexString() = joinToString("") { String.format("%02X", it) }

val Byte.isHexNumber: Boolean
    get() = when (this.toInt().toChar()) {
        in '0'..'9' -> true
        in 'A'..'F' -> true
        in 'a'..'f' -> true
        else -> false
    }

fun String.hexStringToByteArray(): ByteArray {
    if (isEmpty()) return byteArrayOf()
    val hexString = replace(" ", "").replace("\n", "")
    val len = hexString.length
    if (len == 0) return ByteArray(0)
    require(len % 2 != 1) { "string length should be an even number" }
    val ret = ByteArray(len / 2)
    val tmp = hexString.toByteArray()
    var i = 0
    while (i < len) {
        if (!tmp[i].isHexNumber || !tmp[i + 1].isHexNumber) {
            throw NumberFormatException(
                "string contained invalid value"
            )
        }

        var b0 = java.lang.Byte.decode("0x" + String(byteArrayOf(tmp[i])))
            .toByte()
        b0 = (b0.toInt() shl 4).toByte()
        val b1 = java.lang.Byte.decode("0x" + String(byteArrayOf(tmp[i + 1])))
            .toByte()

        ret[i / 2] = b0 xor b1
        i += 2
    }
    return ret
}

fun Reader.sendApdu(
    slotNum: Int,
    apdu: ByteArray,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): ByteArray {
    val buffer = ByteArray(bufferSize)

    val responseSize = transmit(
        slotNum,
        apdu,
        apdu.size,
        buffer,
        buffer.size
    )

    return buffer.copyOf(responseSize)
}

fun Reader.sendControl(
    slotNum: Int,
    controlCode: Int,
    apdu: ByteArray,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): ByteArray {
    val buffer = ByteArray(bufferSize)

    val responseSize = control(
        slotNum,
        controlCode,
        apdu,
        apdu.size,
        buffer,
        buffer.size
    )

    return buffer.copyOf(responseSize)
}
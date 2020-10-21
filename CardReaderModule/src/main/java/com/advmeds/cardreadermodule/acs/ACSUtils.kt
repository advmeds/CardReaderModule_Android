package com.advmeds.cardreadermodule.acs

import kotlin.experimental.xor

public class ACSUtils {
    companion object {
        /**
         * Converts the byte array to HEX string.
         * @return the HEX string.
         */
        public fun ByteArray.toHexString() = this.joinToString(" ") { String.format("%02X", it) }

        private val Byte.isHexNumber: Boolean
            get() = when(this.toChar()) {
                in '0'..'9' -> true
                in 'A'..'F' -> true
                in 'a'..'f' -> true
                else -> false
            }

        /**
         * Checks a hexadecimal <code>String</code> that is contained hexadecimal
         * value or not.
         *
         * @return <code>true</code> the <code>string</code> contains Hex number only, <code>false</code> otherwise.
         */
        private val String.isHexNumber: Boolean
            get() {
                var flag = true
                this.forEach { if (!it.toByte().isHexNumber) { flag = false; return@forEach } }
                return flag
            }

        private fun uniteBytes(src0: Byte, src1: Byte): Byte {
            var b0 = java.lang.Byte.decode("0x" + String(byteArrayOf(src0)))
                .toByte()
            b0 = (b0.toInt() shl 4).toByte()
            val b1 = java.lang.Byte.decode("0x" + String(byteArrayOf(src1)))
                .toByte()
            return b0 xor b1
        }

        /**
         * Creates a <code>byte[]</code> representation of the hexadecimal
         * <code>String</code> passed.
         *
         * @param string the hexadecimal string to be converted.
         * @return the <code>groupArray</code> representation of <code>String</code>.
         * @throws IllegalArgumentException if <code>string</code> length is not in even number.
         * @throws NumberFormatException    if <code>string</code> cannot be parsed as a byte value.
         */
        public fun hexString2Bytes(string: String): ByteArray {
            val len = string.length
            if (len == 0) return ByteArray(0)
            require(len % 2 != 1) { "string length should be an even number" }
            val ret = ByteArray(len / 2)
            val tmp = string.toByteArray()
            var i = 0
            while (i < len) {
                if (!tmp[i].isHexNumber || !tmp[i + 1].isHexNumber) {
                    throw NumberFormatException(
                        "string contained invalid value"
                    )
                }
                ret[i / 2] = uniteBytes(tmp[i], tmp[i + 1])
                i += 2
            }
            return ret
        }

        public fun getStringinHexBytes(rawdata: String): ByteArray? {

            if (rawdata.isEmpty()) return null

            val command = rawdata.replace(" ", "").replace("\n", "")

            if (command.isEmpty() || command.length % 2 != 0 || !command.isHexNumber) return null

            return hexString2Bytes(command)
        }
    }
}
package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.acs.AcsResponseModel

public interface AcsUsbBaseDecoder {
    /**
     * 解析ACS USB讀卡機回傳的資料
     * @param reader the Bluetooth reader.
     * @return 資料模組 AcsResponseModel
     */
    fun decode(reader: Reader): AcsResponseModel?
}
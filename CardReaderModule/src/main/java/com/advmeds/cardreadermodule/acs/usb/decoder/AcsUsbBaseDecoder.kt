package com.advmeds.cardreadermodule.acs.usb.decoder

import com.acs.smartcard.Reader
import com.advmeds.cardreadermodule.AcsResponseModel

public interface AcsUsbBaseDecoder {
    /**
     * 解析ACS USB讀卡機回傳的資料
     * @param reader [com.acs.smartcard.Reader]
     * @return 資料模組 [AcsResponseModel]
     */
    fun decode(reader: Reader, slot: Int): AcsResponseModel
}
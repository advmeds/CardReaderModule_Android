package com.advmeds.cardreadermodule.acs.ble.decoder

import com.acs.bluetooth.BluetoothReader
import com.advmeds.cardreadermodule.acs.AcsResponseModel

public interface AcsBleBaseDecoder {
    /**
     * 通知Decoder可以開始解析資料
     * @param reader the Bluetooth reader.
     */
    fun start(reader: BluetoothReader)

    /**
     * 解析ACS藍牙讀卡機回傳的資料
     * @param reader the Bluetooth reader.
     * @param apdu the response APDU.
     * @return 資料模組 AcsResponseModel
     */
    fun decode(reader: BluetoothReader, apdu: ByteArray): AcsResponseModel?
}
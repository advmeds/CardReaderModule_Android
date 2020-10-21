package com.advmeds.cardreadermodule.acs.usb

import com.advmeds.cardreadermodule.acs.AcsResponseModel

public interface AcsUsbCallback {
    /** 成功連線上設備 */
    fun onConnectDevice()

    /** 無法與設備連線 */
    fun onFailToConnectDevice()

    /** 卡片插入設備 */
    fun onCardPresent()

    /**
     * 回傳插入卡片後的讀取結果
     * @param result 判斷成功或失敗，若成功則回傳AcsResponseModel；若失敗則回傳Exception
     */
    fun onReceiveResult(result: Result<AcsResponseModel>)

    /** 卡片從設備抽離 */
    fun onCardAbsent()
}
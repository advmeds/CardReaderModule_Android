package com.advmeds.cardreadermodule.acs.ble

import com.advmeds.cardreadermodule.acs.AcsResponseModel

public interface AcsBaseCallback {
    /**
     * 設備狀態更新時，會呼叫此方法
     * @param status 設備狀態
     * @see AcsBleDeviceStatus
     */
    fun onDeviceStatusChanged(status: AcsBaseDevice.AcsBleDeviceStatus)

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
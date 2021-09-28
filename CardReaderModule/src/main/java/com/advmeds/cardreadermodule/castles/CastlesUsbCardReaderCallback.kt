package com.advmeds.cardreadermodule.castles

import com.advmeds.cardreadermodule.AcsResponseModel

interface CastlesUsbCardReaderCallback {

    /**
     * 回傳USB讀卡機狀態
     * @param state 是否準備就緒
     */
    fun onReaderStateChanged(state: CastlesUsbCardReader.ReaderState)

    /**
     * 回傳卡片是否已插入讀卡機
     * @param isPresented 卡片是否已插入
     */
    fun onCardStateChanged(isPresented: Boolean)

    /**
     * 回傳插入卡片後的讀取結果
     * @param result 判斷成功或失敗，若成功則回傳AcsResponseModel；若失敗則回傳Exception
     */
    fun onReceiveResult(result: Result<AcsResponseModel>)
}
package com.advmeds.cardreadermodule

/** 偵測到卡片插入設備後，執行powerOn回傳的值為null，會拋出此異常 */
public class InvalidCardException : Exception("The card is invalid, because failed to power on the card")

/** 在解析資料時，傳送command後得到失敗的結果，或是設定協定失敗，會拋出此異常 */
public class DecodeErrorException(message: String?) : Exception(message)

/** 當解析資料後取得到的response為null時，會拋出此異常 */
public class NullResponseException(
    /** 錯誤訊息 */
    message: String? = "The response is null",
    /** 造成的原因 */
    cause: Throwable? = null
) : Exception(message, cause)
package com.advmeds.cardreadermodule.acs

/** 當decode後取得到的response為null時，會拋出此異常 */
public class NullResponseException: NullPointerException("The response is null.")

/** 當decode後取得到的response為空時，會拋出此異常 */
public class EmptyResponseException: Exception("The response is empty.")
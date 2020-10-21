package com.advmeds.cardreadermodule.acs

import java.util.*

public data class AcsResponseModel(
    /** 卡號 */
    public var cardNo: String = "",

    /** 身分證號碼 */
    public var icId: String = "",

    /** 姓名 */
    public var name: String = "",

    /**
     * 性別
     * @see Gender
     */
    public var gender: Gender = Gender.UNKNOWN,

    /**
     * 卡片類型
     * @see CardType
     */
    public var cardType: CardType = CardType.UNKNOWN,

    /** 生日 */
    public var birthday: Date? = null,

    /** 發卡日期 */
    public var issuedDate: Date? = null,

    /** 到期日期 */
    public var expiredDate: Date? = null
) {
    /** 性別 */
    public enum class Gender(rawValue: Int) {
        /** 未知 */
        UNKNOWN(0),

        /** 男性 */
        MALE(10),

        /** 女性 */
        FEMALE(20);
    }

    /** 卡片類型 */
    public enum class CardType(rawValue: Int) {
        /** 未知 */
        UNKNOWN(0),

        /** 健保卡 */
        HEALTH_CARD(1),

        /** 員工卡 */
        STAFF_CARD(2)
    }

    /**
     * 判斷Model是否為空
     * @return 若為空回傳true，反之回傳false
     */
    public fun isEmpty() = cardNo.isEmpty()
}
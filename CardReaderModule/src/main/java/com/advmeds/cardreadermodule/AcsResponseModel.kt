package com.advmeds.cardreadermodule

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
    public var birthday: DateBean? = null,

    /** 發卡日期 */
    public var issuedDate: DateBean? = null,

    /** 到期日期 */
    public var expiredDate: DateBean? = null
) {
    /** 性別 */
    public enum class Gender(val rawValue: Int) {
        /** 未知 */
        UNKNOWN(0),

        /** 男性 */
        MALE(10),

        /** 女性 */
        FEMALE(20);

        companion object {
            fun initWith(rawValue: Int) =
                values().find { it.rawValue == rawValue }
        }
    }

    /** 卡片類型 */
    public enum class CardType(val rawValue: Int) {
        /** 未知 */
        UNKNOWN(0),

        /** 健保卡 */
        HEALTH_CARD(1),

        /** 員工卡 */
        STAFF_CARD(2);

        companion object {
            fun initWith(rawValue: Int) =
                values().find { it.rawValue == rawValue }
        }
    }

    /** 日期 */
    public data class DateBean(
        /** 年份(西元) */
        val year: String,
        /** 月份 */
        val month: String,
        /** 一個月的第幾天 */
        val day: String
    )

    /**
     * 判斷Model是否為空
     * @return 若為空回傳true，反之回傳false
     */
    public fun isEmpty() = cardNo.isEmpty()
}
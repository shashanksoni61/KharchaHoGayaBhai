package com.shashanksoni.kharchahogayabhai.domain.model

/**
 * A tag on a transaction. Multiple tags are allowed (e.g. Food + Swiggy +
 * Office expense). Categories remain for reporting totals; tags are everyday
 * Indian lifestyle markers the user can also customise.
 */
data class TransactionLabel(
    val id: Long = UNSAVED_ID,
    val name: String,
    val colorHex: String,
    val isSystemDefined: Boolean,
    val sortOrder: Int,
) {
    companion object {
        const val UNSAVED_ID = 0L
    }
}

/**
 * Built-in lifestyle tags. Ids are fixed permanently.
 * Custom tags get Room auto-ids above this range.
 */
object DefaultLabels {
    // Food & delivery
    const val FOOD = 1L
    const val ZOMATO = 2L
    const val SWIGGY = 3L
    const val BLINKIT = 4L
    const val ZEPTO = 5L
    const val CHAI_TEA = 6L
    const val SWEETS = 7L
    const val DINING_OUT = 8L

    // Shopping & kirana
    const val SHOPPING = 9L
    const val AMAZON = 10L
    const val FLIPKART = 11L
    const val DMART = 12L
    const val VISHAL_MEGA_MART = 13L
    const val KIRANA = 14L
    const val PHARMACY = 15L

    // Transport & fuel
    const val UBER = 16L
    const val OLA = 17L
    const val PETROL = 18L
    const val METRO_BUS = 19L
    const val AUTO_RICKSHAW = 20L

    // Home & bills
    const val RENT_PG = 21L
    const val ELECTRICITY = 22L
    const val MOBILE_RECHARGE = 23L
    const val BROADBAND = 24L
    const val ATM_CASH = 25L

    // Money movement / review
    const val FAMILY_TRANSFER = 26L
    const val FRIEND_SPLIT = 27L
    const val SALARY = 28L
    const val UNRECOGNISED = 29L
    const val NEEDS_REVIEW = 30L

    val all: List<TransactionLabel> = listOf(
        TransactionLabel(FOOD, "Food", "FF7043", true, 1),
        TransactionLabel(ZOMATO, "Zomato", "E23744", true, 2),
        TransactionLabel(SWIGGY, "Swiggy", "FC8019", true, 3),
        TransactionLabel(BLINKIT, "Blinkit", "FFE141", true, 4),
        TransactionLabel(ZEPTO, "Zepto", "3C006A", true, 5),
        TransactionLabel(CHAI_TEA, "Chai / tea", "8D6E63", true, 6),
        TransactionLabel(SWEETS, "Sweets", "EC407A", true, 7),
        TransactionLabel(DINING_OUT, "Dining out", "FFA726", true, 8),
        TransactionLabel(SHOPPING, "Shopping", "AB47BC", true, 9),
        TransactionLabel(AMAZON, "Amazon", "FF9900", true, 10),
        TransactionLabel(FLIPKART, "Flipkart", "2874F0", true, 11),
        TransactionLabel(DMART, "DMart", "00838F", true, 12),
        TransactionLabel(VISHAL_MEGA_MART, "Vishal Mega Mart", "5C6BC0", true, 13),
        TransactionLabel(KIRANA, "Kirana / general store", "6D4C41", true, 14),
        TransactionLabel(PHARMACY, "Pharmacy", "EF5350", true, 15),
        TransactionLabel(UBER, "Uber", "000000", true, 16),
        TransactionLabel(OLA, "Ola", "C0F70A", true, 17),
        TransactionLabel(PETROL, "Petrol / fuel", "455A64", true, 18),
        TransactionLabel(METRO_BUS, "Metro / bus", "1565C0", true, 19),
        TransactionLabel(AUTO_RICKSHAW, "Auto / rickshaw", "F9A825", true, 20),
        TransactionLabel(RENT_PG, "Rent / PG", "8D6E63", true, 21),
        TransactionLabel(ELECTRICITY, "Electricity", "FFCA28", true, 22),
        TransactionLabel(MOBILE_RECHARGE, "Mobile recharge", "29B6F6", true, 23),
        TransactionLabel(BROADBAND, "Broadband / Wi‑Fi", "7E57C2", true, 24),
        TransactionLabel(ATM_CASH, "ATM cash", "78909C", true, 25),
        TransactionLabel(FAMILY_TRANSFER, "Family transfer", "66BB6A", true, 26),
        TransactionLabel(FRIEND_SPLIT, "Friend split", "26A69A", true, 27),
        TransactionLabel(SALARY, "Salary", "43A047", true, 28),
        TransactionLabel(UNRECOGNISED, "Unrecognised", "BDBDBD", true, 29),
        TransactionLabel(NEEDS_REVIEW, "Needs review", "FFA726", true, 30),
    )
}

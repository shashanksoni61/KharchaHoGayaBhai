package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * A half-open instant range, `[start, endExclusive)`.
 *
 * Half-open avoids the classic "last millisecond of the month" bug when filtering
 * by month.
 */
data class InstantRange(
    val start: Instant,
    val endExclusive: Instant,
) {
    init {
        require(!endExclusive.isBefore(start)) { "endExclusive must not precede start" }
    }

    operator fun contains(instant: Instant): Boolean =
        instant >= start && instant < endExclusive

    companion object {
        /** Calendar month boundaries in [zone]; month filtering must be timezone-aware. */
        fun ofMonth(month: YearMonth, zone: ZoneId): InstantRange = InstantRange(
            start = month.atDay(1).atStartOfDay(zone).toInstant(),
            endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(),
        )

        /** Spans [from] through [to] inclusive of both months. */
        fun ofMonths(from: YearMonth, to: YearMonth, zone: ZoneId): InstantRange = InstantRange(
            start = ofMonth(from, zone).start,
            endExclusive = ofMonth(to, zone).endExclusive,
        )
    }
}

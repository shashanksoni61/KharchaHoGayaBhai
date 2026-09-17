package com.shashanksoni.kharchahogayabhai.domain.parser

import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

/**
 * Turns one source-specific input into transaction candidates.
 *
 * [Input] is whatever that source deals in: an SMS body plus sender, a CSV
 * table, or text already extracted from a PDF. Parsers stay small and
 * independent, one per bank format where needed, and none of them knows about
 * storage, deduplication or the UI.
 *
 * The same shape leaves room for a future on-device AI parser to sit behind the
 * rule-based ones: try each rule parser, and only fall back when they all
 * decline.
 */
interface TransactionParser<Input> {

    /** Which source this parser speaks for. */
    val source: TransactionSource

    /** Human-readable parser name, e.g. "HDFC UPI SMS". Shown in import diagnostics. */
    val name: String

    /**
     * Cheap check for whether this parser recognises the input, so callers can
     * pick a parser without attempting a full parse.
     */
    fun canParse(input: Input): Boolean

    /**
     * Extracts every transaction the input describes. Returns an empty list when
     * the input holds no transactions at all; items that were recognised but not
     * understood come back with [com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus.FAILED].
     */
    fun parse(input: Input): List<ParsedTransaction>
}

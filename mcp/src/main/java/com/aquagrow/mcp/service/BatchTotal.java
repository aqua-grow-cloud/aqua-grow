package com.aquagrow.mcp.service;

import java.util.List;

/** A single summed figure for a batch (revenue or expenditure) with the notes that qualify it. */
public record BatchTotal(String batchId, String batchName, double total, int records, List<String> notes) {
}

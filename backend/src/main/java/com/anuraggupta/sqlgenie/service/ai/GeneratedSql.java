package com.anuraggupta.sqlgenie.service.ai;

/**
 * Structured result of one LLM call: the SQL and its explanation are
 * requested together in a single round trip rather than two separate calls.
 */
public record GeneratedSql(String sql, String explanation) {
}

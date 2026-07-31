package com.anuraggupta.sqlgenie.service.sql;

public interface SqlExecutorService {

    /**
     * Independently re-validates (never trusts that the caller already did)
     * then executes the SQL against the read-only role, bounded by a
     * configured query timeout and row limit.
     */
    QueryResult execute(String sql);
}

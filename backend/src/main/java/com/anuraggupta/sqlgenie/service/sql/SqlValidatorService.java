package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;

public interface SqlValidatorService {

    /**
     * Throws UnsafeSqlException if the SQL is anything other than a single,
     * simple SELECT statement referencing only allow-listed target tables.
     * Never trusts the caller's claim that the SQL is safe - re-parses and
     * re-checks independently every time.
     */
    void validate(String sql);
}

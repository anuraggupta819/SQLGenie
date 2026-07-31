package com.anuraggupta.sqlgenie.service.ai;

public interface SqlExplanationService {

    /**
     * Explains an existing SQL string in plain English, independent of any
     * original natural-language question - used when re-explaining SQL that
     * wasn't just generated in the same call (e.g. a saved favorite).
     */
    String explain(String sql);
}

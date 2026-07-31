package com.anuraggupta.sqlgenie.service.ai;

public interface SchemaIntrospectionService {

    /**
     * A plain-text description of the target schema's tables and columns,
     * suitable for embedding directly into an LLM prompt.
     */
    String describeTargetSchema();
}

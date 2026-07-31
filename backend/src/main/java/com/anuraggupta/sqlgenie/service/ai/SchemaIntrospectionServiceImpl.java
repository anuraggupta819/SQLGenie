package com.anuraggupta.sqlgenie.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchemaIntrospectionServiceImpl implements SchemaIntrospectionService {

    private static final String TARGET_SCHEMA = "target";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String describeTargetSchema() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = ?
                ORDER BY table_name, ordinal_position
                """, TARGET_SCHEMA);

        Map<String, List<String>> columnsByTable = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String tableName = (String) row.get("table_name");
            String columnDescription = row.get("column_name") + " " + row.get("data_type");
            columnsByTable.computeIfAbsent(tableName, key -> new ArrayList<>()).add(columnDescription);
        }

        StringBuilder description = new StringBuilder();
        for (Map.Entry<String, List<String>> table : columnsByTable.entrySet()) {
            description.append(TARGET_SCHEMA).append('.').append(table.getKey())
                    .append('(').append(String.join(", ", table.getValue())).append(")\n");
        }
        return description.toString().stripTrailing();
    }
}

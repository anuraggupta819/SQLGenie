package com.anuraggupta.sqlgenie.service.sql;

import java.util.List;
import java.util.Map;

public record QueryResult(List<String> columns, List<Map<String, Object>> rows, long executionTimeMs) {
}

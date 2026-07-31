package com.anuraggupta.sqlgenie.service.ai;

public interface NlToSqlService {

    GeneratedSql generateSql(String naturalLanguageQuery);
}

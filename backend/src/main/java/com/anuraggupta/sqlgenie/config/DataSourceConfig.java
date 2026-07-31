package com.anuraggupta.sqlgenie.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Two independent DataSources: the primary one (app_owner) backs JPA/Flyway
 * as usual. readOnlyDataSource connects as readonly_query_user - a role
 * that is database-enforced to have SELECT-only access to the target schema
 * and no access whatsoever to app - and is the only connection ever used to
 * run LLM-generated SQL.
 *
 * Uses the DataSourceProperties + initializeDataSourceBuilder() pattern
 * (the officially documented approach for multiple datasources) rather than
 * binding @ConfigurationProperties directly onto a bare DataSourceBuilder
 * result - the latter does not reliably resolve driver/url wiring once a
 * second DataSource-typed bean is present, which is what broke Flyway here.
 */
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.readonly")
    public DataSourceProperties readOnlyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource readOnlyDataSource(
            @Qualifier("readOnlyDataSourceProperties") DataSourceProperties readOnlyDataSourceProperties) {
        return readOnlyDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public JdbcTemplate readOnlyJdbcTemplate(
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource,
            @Value("${app.query-execution.timeout-seconds}") int timeoutSeconds,
            @Value("${app.query-execution.max-rows}") int maxRows) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        jdbcTemplate.setQueryTimeout(timeoutSeconds);
        jdbcTemplate.setMaxRows(maxRows);
        return jdbcTemplate;
    }

    @Bean
    public PlatformTransactionManager readOnlyTransactionManager(
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        return new DataSourceTransactionManager(readOnlyDataSource);
    }

    // Adding readOnlyTransactionManager above trips Spring Boot's
    // @ConditionalOnMissingBean(TransactionManager.class), so it silently
    // stops creating JPA's own "transactionManager" bean - breaking every
    // plain @Transactional method (no explicit manager named) elsewhere in
    // the app, e.g. AuthServiceImpl. Re-declaring it explicitly, named and
    // @Primary, restores the default @Transactional resolution everyone
    // else already depends on.
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

package com.example.inventory_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class ProcessedOrderEventIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();

        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        this.jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
                "DROP TABLE IF EXISTS processed_order_events"
        );

        jdbcTemplate.execute("""
                CREATE TABLE processed_order_events (
                    order_id INTEGER PRIMARY KEY,
                    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    @Test
    void duplicateOrderIdCreatesOnlyOneRecord() {
        String sql = """
                INSERT INTO processed_order_events (order_id, processed_at)
                VALUES (?, CURRENT_TIMESTAMP)
                ON CONFLICT (order_id) DO NOTHING
                """;

        int firstInsert = jdbcTemplate.update(sql, 9002);
        int duplicateInsert = jdbcTemplate.update(sql, 9002);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_order_events WHERE order_id = ?",
                Integer.class,
                9002
        );

        assertEquals(1, firstInsert);
        assertEquals(0, duplicateInsert);
        assertEquals(1, count);
    }
}
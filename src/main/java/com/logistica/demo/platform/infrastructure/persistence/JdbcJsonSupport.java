package com.logistica.demo.platform.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcJsonSupport {

    private final JdbcTemplate jdbcTemplate;
    private Boolean postgreSql;

    JdbcJsonSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    String jsonPlaceholder(String typeName) {
        return isPostgreSql() ? "CAST(? AS " + typeName + ")" : "?";
    }

    private boolean isPostgreSql() {
        if (postgreSql == null) {
            postgreSql = jdbcTemplate.execute((Connection connection) -> {
                try {
                    String databaseName = connection.getMetaData().getDatabaseProductName();
                    return databaseName.toLowerCase(Locale.ROOT).contains("postgresql");
                } catch (SQLException ex) {
                    throw new IllegalStateException("No se pudo identificar la base de datos", ex);
                }
            });
        }
        return postgreSql;
    }
}

package com.logistica.demo.shared.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class SharedDataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        String url = env.getProperty("spring.datasource.url");
        if (!StringUtils.hasText(url)) {
            String host = env.getProperty("DB_HOST", "localhost");
            String port = env.getProperty("DB_PORT", "5432");
            String dbName = env.getProperty("DB_NAME", "logistica_demo");
            url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        }

        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        String driverClassName = env.getProperty("spring.datasource.driver-class-name");

        ParsedUrl parsedUrl = parse(url);
        if (parsedUrl != null) {
            url = parsedUrl.url();
            if (!StringUtils.hasText(username) && StringUtils.hasText(parsedUrl.username())) {
                username = parsedUrl.username();
            }
            if (!StringUtils.hasText(password) && StringUtils.hasText(parsedUrl.password())) {
                password = parsedUrl.password();
            }
        }

        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password);
        return builder.build();
    }

    private ParsedUrl parse(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return null;
        }
        String url = rawUrl.trim();
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            return null;
        }

        String rest = url.substring("jdbc:postgresql://".length());
        String username = null;
        String password = null;

        int at = rest.lastIndexOf('@');
        String hostPart = rest;
        if (at > 0) {
            String userInfo = rest.substring(0, at);
            hostPart = rest.substring(at + 1);
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = userInfo.substring(0, colon);
                password = userInfo.substring(colon + 1);
            } else {
                username = userInfo;
            }
        }

        String authority = hostPart;
        String suffix = "";
        int slash = hostPart.indexOf('/');
        if (slash >= 0) {
            authority = hostPart.substring(0, slash);
            suffix = hostPart.substring(slash);
        }
        int question = authority.indexOf('?');
        String params = "";
        if (question >= 0) {
            params = authority.substring(question);
            authority = authority.substring(0, question);
        }
        if (!authority.contains(":")) {
            authority = authority + ":5432";
        }

        return new ParsedUrl("jdbc:postgresql://" + authority + params + suffix, username, password);
    }

    private record ParsedUrl(String url, String username, String password) {
    }
}
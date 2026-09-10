package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.AuditEvent;
import com.logistica.demo.platform.api.AuditEventCommand;
import com.logistica.demo.platform.api.FunctionalAuditPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcFunctionalAuditAdapter implements FunctionalAuditPort {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jsonSupport;

    public JdbcFunctionalAuditAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = new JdbcJsonSupport(jdbcTemplate);
    }

    @Override
    @Transactional
    public AuditEvent record(AuditEventCommand command) {
        String sql = """
                INSERT INTO platform.audit_events (
                    company_id, actor, effective_role, action, aggregate_type, aggregate_id,
                    occurred_at, ip_address, trace_id, changes
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, %s)
                """.formatted(jsonSupport.jsonPlaceholder("JSONB"));
        jdbcTemplate.update(
                sql,
                command.companyId(),
                command.actor(),
                command.effectiveRole(),
                command.action(),
                command.aggregateType(),
                command.aggregateId(),
                command.ipAddress(),
                command.traceId(),
                command.changesJson());
        return jdbcTemplate.query(
                        """
                        SELECT id, company_id, actor, effective_role, action, aggregate_type, aggregate_id,
                               occurred_at, ip_address, trace_id, changes
                        FROM platform.audit_events
                        WHERE actor = ? AND action = ? AND aggregate_type = ? AND aggregate_id = ?
                        ORDER BY id DESC
                        FETCH FIRST 1 ROWS ONLY
                        """,
                        (rs, rowNum) -> new AuditEvent(
                                rs.getLong("id"),
                                nullableLong(rs, "company_id"),
                                rs.getString("actor"),
                                rs.getString("effective_role"),
                                rs.getString("action"),
                                rs.getString("aggregate_type"),
                                rs.getString("aggregate_id"),
                                rs.getTimestamp("occurred_at").toInstant(),
                                rs.getString("ip_address"),
                                rs.getString("trace_id"),
                                rs.getString("changes")),
                        command.actor(),
                        command.action(),
                        command.aggregateType(),
                        command.aggregateId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo leer el evento de auditoria"));
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}

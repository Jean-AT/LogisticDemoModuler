package com.logistica.demo.logistica.consultas.service;

import com.logistica.demo.logistica.consultas.dto.LogisticsDashboardResponse;
import com.logistica.demo.logistica.consultas.dto.LogisticsTraceabilityEventResponse;
import com.logistica.demo.logistica.consultas.dto.LogisticsTraceabilityResponse;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogisticsQueryService {

    private final JdbcTemplate jdbcTemplate;

    public LogisticsQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public LogisticsDashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        return new LogisticsDashboardResponse(
                count("""
                        SELECT COUNT(*)
                        FROM logistica_demo.requerimientos
                        WHERE estado = 'ENVIADO'
                        """),
                count("""
                        SELECT COUNT(*)
                        FROM logistica_demo.requerimientos req
                        WHERE req.estado = 'APROBADO'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM logistica_demo.ordenes_compra oc
                              WHERE oc.requerimiento_id = req.id
                          )
                        """),
                countOrdersByStatus("GENERADA"),
                countOrdersByStatus("APROBADA"),
                countOrdersByStatus("PARCIALMENTE_RECIBIDA"),
                countOrdersByStatus("RECIBIDA"),
                count("""
                        SELECT COUNT(*)
                        FROM logistica_demo.recepciones_almacen
                        WHERE estado = 'REGISTRADA'
                          AND received_at >= ?
                          AND received_at < ?
                        """, startOfDay, startOfTomorrow),
                count("""
                        SELECT COUNT(*)
                        FROM logistica_demo.recepciones_almacen
                        WHERE estado = 'REVERTIDA'
                        """),
                count("""
                        SELECT COUNT(*)
                        FROM logistica_demo.stock_actual
                        WHERE stock_actual <> 0
                        """),
                sum("""
                        SELECT COALESCE(SUM(stock_actual), 0)
                        FROM logistica_demo.stock_actual
                        """),
                sum("""
                        SELECT COALESCE(SUM(detail.cantidad - detail.cantidad_recibida), 0)
                        FROM logistica_demo.orden_compra_detalles detail
                        JOIN logistica_demo.ordenes_compra orden ON orden.id = detail.orden_compra_id
                        WHERE orden.estado IN ('APROBADA', 'PARCIALMENTE_RECIBIDA')
                          AND detail.cantidad > detail.cantidad_recibida
                        """),
                sum("""
                        SELECT COALESCE(SUM(total), 0)
                        FROM logistica_demo.ordenes_compra
                        WHERE estado IN ('APROBADA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA')
                        """));
    }

    @Transactional(readOnly = true)
    public LogisticsTraceabilityResponse traceRequirement(Long requerimientoId) {
        TraceabilityHeader header = jdbcTemplate.query("""
                SELECT req.id,
                       req.numero,
                       req.estado,
                       req.needs_plan_id,
                       req.needs_line_id,
                       req.budget_control_id,
                       oc.id AS orden_compra_id,
                       oc.numero AS orden_compra_numero,
                       oc.estado AS orden_compra_estado
                FROM logistica_demo.requerimientos req
                LEFT JOIN logistica_demo.ordenes_compra oc ON oc.requerimiento_id = req.id
                WHERE req.id = ?
                """, rs -> {
                    if (!rs.next()) {
                        throw new ResourceNotFoundException("Requerimiento no encontrado.");
                    }
                    return new TraceabilityHeader(
                            rs.getLong("id"),
                            rs.getString("numero"),
                            rs.getString("estado"),
                            nullableLong(rs, "needs_plan_id"),
                            nullableLong(rs, "needs_line_id"),
                            nullableLong(rs, "budget_control_id"),
                            nullableLong(rs, "orden_compra_id"),
                            rs.getString("orden_compra_numero"),
                            rs.getString("orden_compra_estado"));
                }, requerimientoId);

        List<LogisticsTraceabilityEventResponse> events = jdbcTemplate.query("""
                SELECT stage,
                       status,
                       reference_id,
                       reference_number,
                       actor,
                       occurred_at,
                       detail
                FROM (
                    SELECT 'REQUERIMIENTO' AS stage,
                           req.estado AS status,
                           req.id AS reference_id,
                           req.numero AS reference_number,
                           req.created_by AS actor,
                           req.created_at AS occurred_at,
                           req.descripcion AS detail
                    FROM logistica_demo.requerimientos req
                    WHERE req.id = ?
                    UNION ALL
                    SELECT 'ESTADO_REQUERIMIENTO',
                           history.estado_nuevo,
                           history.id,
                           req.numero,
                           history.created_by,
                           history.created_at,
                           history.comentario
                    FROM logistica_demo.requerimiento_estado_historial history
                    JOIN logistica_demo.requerimientos req ON req.id = history.requerimiento_id
                    WHERE req.id = ?
                    UNION ALL
                    SELECT 'APROBACION',
                           approval.accion,
                           approval.id,
                           req.numero,
                           approval.created_by,
                           approval.decision_at,
                           approval.comentario
                    FROM logistica_demo.aprobaciones approval
                    JOIN logistica_demo.requerimientos req ON req.id = approval.requerimiento_id
                    WHERE req.id = ?
                    UNION ALL
                    SELECT 'ORDEN_COMPRA',
                           oc.estado,
                           oc.id,
                           oc.numero,
                           oc.created_by,
                           oc.generated_at,
                           'Total ' || oc.moneda || ' ' || oc.total
                    FROM logistica_demo.ordenes_compra oc
                    WHERE oc.requerimiento_id = ?
                    UNION ALL
                    SELECT 'APROBACION_OC',
                           oc.estado,
                           oc.id,
                           oc.numero,
                           oc.approved_by,
                           oc.approved_at,
                           'Control presupuestal ' || COALESCE(CAST(oc.budget_control_id AS VARCHAR), '-')
                    FROM logistica_demo.ordenes_compra oc
                    WHERE oc.requerimiento_id = ?
                      AND oc.approved_at IS NOT NULL
                    UNION ALL
                    SELECT 'RECEPCION',
                           receipt.estado,
                           receipt.id,
                           receipt.numero,
                           receipt.actor,
                           receipt.received_at,
                           'Orden ' || oc.numero
                    FROM logistica_demo.recepciones_almacen receipt
                    JOIN logistica_demo.ordenes_compra oc ON oc.id = receipt.orden_compra_id
                    WHERE oc.requerimiento_id = ?
                    UNION ALL
                    SELECT 'REVERSION_RECEPCION',
                           receipt.estado,
                           receipt.id,
                           receipt.numero,
                           receipt.reversed_by,
                           receipt.reversed_at,
                           receipt.reversal_reason
                    FROM logistica_demo.recepciones_almacen receipt
                    JOIN logistica_demo.ordenes_compra oc ON oc.id = receipt.orden_compra_id
                    WHERE oc.requerimiento_id = ?
                      AND receipt.reversed_at IS NOT NULL
                    UNION ALL
                    SELECT 'KARDEX',
                           movement.tipo,
                           movement.id,
                           movement.source_number,
                           movement.actor,
                           movement.occurred_at,
                           item.code || ' / ' || almacen.code || ' / ' || movement.cantidad_delta
                    FROM logistica_demo.kardex_movimientos movement
                    JOIN logistica_demo.items item ON item.id = movement.item_id
                    JOIN logistica_demo.almacenes almacen ON almacen.id = movement.almacen_id
                    JOIN logistica_demo.recepciones_almacen receipt ON receipt.id = movement.source_id
                    JOIN logistica_demo.ordenes_compra oc ON oc.id = receipt.orden_compra_id
                    WHERE oc.requerimiento_id = ?
                ) timeline
                WHERE occurred_at IS NOT NULL
                ORDER BY occurred_at ASC, reference_id ASC
                """, this::mapEvent, requerimientoId, requerimientoId, requerimientoId, requerimientoId,
                requerimientoId, requerimientoId, requerimientoId, requerimientoId);

        return new LogisticsTraceabilityResponse(
                header.requerimientoId(),
                header.requerimientoNumero(),
                header.requerimientoEstado(),
                header.needsPlanId(),
                header.needsLineId(),
                header.budgetControlId(),
                header.ordenCompraId(),
                header.ordenCompraNumero(),
                header.ordenCompraEstado(),
                events);
    }

    private long countOrdersByStatus(String status) {
        return count("""
                SELECT COUNT(*)
                FROM logistica_demo.ordenes_compra
                WHERE estado = ?
                """, status);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal sum(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private LogisticsTraceabilityEventResponse mapEvent(ResultSet rs, int rowNum) throws SQLException {
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        return new LogisticsTraceabilityEventResponse(
                rs.getString("stage"),
                rs.getString("status"),
                rs.getLong("reference_id"),
                rs.getString("reference_number"),
                rs.getString("actor"),
                occurredAt == null ? null : occurredAt.toLocalDateTime(),
                rs.getString("detail"));
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record TraceabilityHeader(
            Long requerimientoId,
            String requerimientoNumero,
            String requerimientoEstado,
            Long needsPlanId,
            Long needsLineId,
            Long budgetControlId,
            Long ordenCompraId,
            String ordenCompraNumero,
            String ordenCompraEstado) {
    }
}

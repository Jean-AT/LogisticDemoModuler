package com.logistica.demo.logistica.inventario.service;

import com.logistica.demo.logistica.compras.domain.RecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacenDetalle;
import com.logistica.demo.logistica.inventario.dto.KardexMovimientoResponse;
import com.logistica.demo.logistica.inventario.dto.StockProjectionResponse;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final String RECEIPT_SOURCE = "RECEPCION_ALMACEN";
    private static final String RECEIPT_REVERSAL_SOURCE = "REVERSION_RECEPCION";

    private final JdbcTemplate jdbcTemplate;

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void registerReceipt(RecepcionAlmacen recepcion) {
        for (RecepcionAlmacenDetalle detalle : recepcion.getDetalles()) {
            insertMovement(
                    detalle,
                    "ENTRADA_RECEPCION",
                    RECEIPT_SOURCE,
                    BigDecimal.valueOf(detalle.getCantidadRecibida()),
                    recepcion);
        }
    }

    @Transactional
    public void registerReceiptReversal(RecepcionAlmacen recepcion) {
        for (RecepcionAlmacenDetalle detalle : recepcion.getDetalles()) {
            insertMovement(
                    detalle,
                    "REVERSA_RECEPCION",
                    RECEIPT_REVERSAL_SOURCE,
                    BigDecimal.valueOf(detalle.getCantidadRecibida()).negate(),
                    recepcion);
        }
    }

    @Transactional(readOnly = true)
    public List<KardexMovimientoResponse> kardex(Long itemId, Long almacenId) {
        if (itemId == null || itemId <= 0) {
            throw new ResourceNotFoundException("Item no encontrado.");
        }
        if (almacenId == null || almacenId <= 0) {
            throw new ResourceNotFoundException("Almacen no encontrado.");
        }
        List<KardexMovimientoResponse> movements = jdbcTemplate.query("""
                SELECT km.id,
                       km.item_id,
                       item.code AS item_code,
                       item.name AS item_name,
                       km.almacen_id,
                       almacen.code AS almacen_code,
                       almacen.name AS almacen_name,
                       km.tipo,
                       km.source_type,
                       km.source_id,
                       km.source_line_id,
                       km.source_number,
                       km.cantidad_delta,
                       km.actor,
                       km.occurred_at
                FROM logistica_demo.kardex_movimientos km
                JOIN logistica_demo.items item ON item.id = km.item_id
                JOIN logistica_demo.almacenes almacen ON almacen.id = km.almacen_id
                WHERE km.item_id = ?
                  AND km.almacen_id = ?
                ORDER BY km.occurred_at ASC, km.id ASC
                """, (rs, rowNum) -> mapMovement(rs, BigDecimal.ZERO), itemId, almacenId);

        BigDecimal balance = BigDecimal.ZERO;
        List<KardexMovimientoResponse> withBalance = new ArrayList<>();
        for (KardexMovimientoResponse movement : movements) {
            balance = balance.add(movement.cantidadDelta());
            withBalance.add(new KardexMovimientoResponse(
                    movement.id(),
                    movement.itemId(),
                    movement.itemCode(),
                    movement.itemName(),
                    movement.almacenId(),
                    movement.almacenCode(),
                    movement.almacenName(),
                    movement.tipo(),
                    movement.sourceType(),
                    movement.sourceId(),
                    movement.sourceLineId(),
                    movement.sourceNumber(),
                    movement.cantidadDelta(),
                    balance,
                    movement.actor(),
                    movement.occurredAt()));
        }
        return withBalance;
    }

    @Transactional(readOnly = true)
    public StockProjectionResponse projection(Long itemId, Long almacenId) {
        return jdbcTemplate.query("""
                SELECT item.id AS item_id,
                       item.code AS item_code,
                       item.name AS item_name,
                       almacen.id AS almacen_id,
                       almacen.code AS almacen_code,
                       almacen.name AS almacen_name,
                       COALESCE(stock.stock_actual, 0) AS stock_actual,
                       COALESCE(pending.cantidad_pendiente, 0) AS cantidad_pendiente
                FROM logistica_demo.items item
                JOIN logistica_demo.almacenes almacen ON almacen.id = ?
                LEFT JOIN logistica_demo.stock_actual stock
                  ON stock.item_id = item.id
                 AND stock.almacen_id = almacen.id
                LEFT JOIN (
                    SELECT detail.item_id,
                           detail.almacen_id,
                           SUM(detail.cantidad - detail.cantidad_recibida)::NUMERIC(18, 4) AS cantidad_pendiente
                    FROM logistica_demo.orden_compra_detalles detail
                    JOIN logistica_demo.ordenes_compra orden ON orden.id = detail.orden_compra_id
                    WHERE orden.estado IN ('APROBADA', 'PARCIALMENTE_RECIBIDA')
                      AND detail.cantidad > detail.cantidad_recibida
                    GROUP BY detail.item_id, detail.almacen_id
                ) pending
                  ON pending.item_id = item.id
                 AND pending.almacen_id = almacen.id
                WHERE item.id = ?
                """, rs -> {
                    if (!rs.next()) {
                        throw new ResourceNotFoundException("Item o almacen no encontrado.");
                    }
                    BigDecimal stockActual = rs.getBigDecimal("stock_actual");
                    BigDecimal pendingQuantity = rs.getBigDecimal("cantidad_pendiente");
                    return new StockProjectionResponse(
                            rs.getLong("item_id"),
                            rs.getString("item_code"),
                            rs.getString("item_name"),
                            rs.getLong("almacen_id"),
                            rs.getString("almacen_code"),
                            rs.getString("almacen_name"),
                            stockActual,
                            pendingQuantity,
                            stockActual.add(pendingQuantity));
                }, almacenId, itemId);
    }

    private void insertMovement(
            RecepcionAlmacenDetalle detalle,
            String type,
            String sourceType,
            BigDecimal quantityDelta,
            RecepcionAlmacen recepcion) {
        Integer existingMovements = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM logistica_demo.kardex_movimientos
                WHERE source_type = ?
                  AND source_line_id = ?
                """, Integer.class, sourceType, detalle.getId());
        if (existingMovements != null && existingMovements > 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO logistica_demo.kardex_movimientos (
                    item_id,
                    almacen_id,
                    tipo,
                    source_type,
                    source_id,
                    source_line_id,
                    source_number,
                    cantidad_delta,
                    actor,
                    occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                detalle.getOrdenCompraDetalle().getItem().getId(),
                detalle.getOrdenCompraDetalle().getAlmacen().getId(),
                type,
                sourceType,
                recepcion.getId(),
                detalle.getId(),
                recepcion.getNumero(),
                quantityDelta,
                recepcion.getActor(),
                Timestamp.valueOf(recepcion.getReceivedAt()));
    }

    private KardexMovimientoResponse mapMovement(ResultSet rs, BigDecimal balance) throws SQLException {
        return new KardexMovimientoResponse(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getString("item_code"),
                rs.getString("item_name"),
                rs.getLong("almacen_id"),
                rs.getString("almacen_code"),
                rs.getString("almacen_name"),
                rs.getString("tipo"),
                rs.getString("source_type"),
                rs.getLong("source_id"),
                rs.getLong("source_line_id"),
                rs.getString("source_number"),
                rs.getBigDecimal("cantidad_delta"),
                balance,
                rs.getString("actor"),
                rs.getTimestamp("occurred_at").toLocalDateTime());
    }
}

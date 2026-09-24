package com.logistica.demo.presupuesto.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.presupuesto.api.ReleaseBudgetCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetControlJdbcAdapter implements BudgetControlUseCase {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private final JdbcTemplate jdbcTemplate;
    private final BudgetAvailabilityJdbcAdapter availability;
    private final IdempotencyPort idempotency;
    private final ObjectMapper objectMapper;

    public BudgetControlJdbcAdapter(
            JdbcTemplate jdbcTemplate,
            BudgetAvailabilityJdbcAdapter availability,
            IdempotencyPort idempotency) {
        this.jdbcTemplate = jdbcTemplate;
        this.availability = availability;
        this.idempotency = idempotency;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Override
    @Transactional
    public BudgetControlResult precommit(PrecommitBudgetCommand command) {
        validate(command.source(), command.allocations(), command.idempotencyKey(), command.actor());
        return withIdempotency("budget.control.precommit", command.idempotencyKey(), command, () -> doPrecommit(command));
    }

    @Override
    @Transactional
    public BudgetControlResult commit(CommitBudgetCommand command) {
        validate(command.source(), command.allocations(), command.idempotencyKey(), command.actor());
        if (command.budgetControlId() == null || command.budgetControlId() <= 0) {
            throw new IllegalArgumentException("budgetControlId es obligatorio");
        }
        return withIdempotency("budget.control.commit", command.idempotencyKey(), command, () -> doCommit(command));
    }

    @Override
    @Transactional
    public BudgetControlResult release(ReleaseBudgetCommand command) {
        validate(command.source(), command.allocations(), command.idempotencyKey(), command.actor());
        if (command.budgetControlId() == null || command.budgetControlId() <= 0) {
            throw new IllegalArgumentException("budgetControlId es obligatorio");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("reason es obligatorio");
        }
        return withIdempotency("budget.control.release", command.idempotencyKey(), command, () -> doRelease(command));
    }

    private BudgetControlResult doPrecommit(PrecommitBudgetCommand command) {
        if (hasActivePrecommit(command.source())) {
            throw new BusinessRuleException("Existe un precompromiso activo para el documento origen.");
        }
        Instant now = Instant.now();
        Moneda currency = commonCurrency(command.allocations());
        long controlId = insertControl(command.source(), BudgetControlStatus.PRECOMMITTED, command.actor(), now);
        Money affected = zero(currency);
        Money lastAvailable = zero(currency);
        for (BudgetAllocation allocation : command.allocations()) {
            BudgetLineState state = lockedLine(allocation);
            if (state.availableAmount().compareTo(allocation.amount().amount()) < 0) {
                throw new BusinessRuleException("Saldo presupuestal insuficiente para precomprometer.");
            }
            jdbcTemplate.update(
                    """
                    UPDATE presupuesto.budget_lines
                    SET precommitted_amount = precommitted_amount + ?,
                        updated_by = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE id = ?
                    """,
                    allocation.amount().amount(),
                    command.actor(),
                    Timestamp.from(now),
                    state.budgetLineId());
            insertControlLine(controlId, state.budgetLineId(), allocation.amount(), allocation.amount().amount(), BigDecimal.ZERO);
            insertMovement(state.budgetLineId(), "PRECOMMITMENT", command.source(), allocation.amount(), command.idempotencyKey(), command.actor(), now);
            affected = affected.add(allocation.amount());
            lastAvailable = availability.findStateForUpdate(allocation.dimension(), allocation.amount().currency())
                    .orElseThrow()
                    .toAvailability()
                    .available();
        }
        return new BudgetControlResult(controlId, BudgetControlStatus.PRECOMMITTED, affected, lastAvailable);
    }

    private BudgetControlResult doCommit(CommitBudgetCommand command) {
        ControlHeader control = control(command.budgetControlId());
        if (control.status() != BudgetControlStatus.PRECOMMITTED) {
            throw new BusinessRuleException("Solo se puede comprometer un control PRECOMMITTED.");
        }
        Instant now = Instant.now();
        Moneda currency = commonCurrency(command.allocations());
        Money affected = zero(currency);
        Money lastAvailable = zero(currency);
        for (BudgetAllocation allocation : command.allocations()) {
            BudgetLineState state = lockedLine(allocation);
            ControlLine line = controlLine(command.budgetControlId(), state.budgetLineId());
            if (line.precommittedAmount().compareTo(allocation.amount().amount()) < 0) {
                throw new BusinessRuleException("El compromiso supera el precompromiso disponible.");
            }
            jdbcTemplate.update(
                    """
                    UPDATE presupuesto.budget_lines
                    SET precommitted_amount = precommitted_amount - ?,
                        committed_amount = committed_amount + ?,
                        updated_by = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE id = ?
                    """,
                    allocation.amount().amount(),
                    allocation.amount().amount(),
                    command.actor(),
                    Timestamp.from(now),
                    state.budgetLineId());
            jdbcTemplate.update(
                    """
                UPDATE presupuesto.budget_control_lines
                    SET precommitted_amount = precommitted_amount - ?,
                        committed_amount = committed_amount + ?
                    WHERE id = ?
                    """,
                    allocation.amount().amount(),
                    allocation.amount().amount(),
                    line.id());
            insertMovement(state.budgetLineId(), "PRECOMMITMENT_RELEASE", command.source(), allocation.amount(), command.idempotencyKey(), command.actor(), now);
            insertMovement(state.budgetLineId(), "COMMITMENT", command.source(), allocation.amount(), command.idempotencyKey(), command.actor(), now);
            affected = affected.add(allocation.amount());
            lastAvailable = availability.findStateForUpdate(allocation.dimension(), allocation.amount().currency())
                    .orElseThrow()
                    .toAvailability()
                    .available();
        }
        updateControlStatus(command.budgetControlId(), BudgetControlStatus.COMMITTED, command.actor(), now);
        return new BudgetControlResult(command.budgetControlId(), BudgetControlStatus.COMMITTED, affected, lastAvailable);
    }

    private BudgetControlResult doRelease(ReleaseBudgetCommand command) {
        ControlHeader control = control(command.budgetControlId());
        if (control.status() == BudgetControlStatus.RELEASED || control.status() == BudgetControlStatus.REJECTED) {
            throw new BusinessRuleException("El control presupuestal ya esta cerrado.");
        }
        Instant now = Instant.now();
        Moneda currency = commonCurrency(command.allocations());
        Money affected = zero(currency);
        Money lastAvailable = zero(currency);
        for (BudgetAllocation allocation : command.allocations()) {
            BudgetLineState state = lockedLine(allocation);
            ControlLine line = controlLine(command.budgetControlId(), state.budgetLineId());
            BigDecimal remaining = allocation.amount().amount();
            if (line.openAmount().compareTo(remaining) < 0) {
                throw new BusinessRuleException("La liberacion supera el saldo del documento origen.");
            }
            BigDecimal releasePrecommit = line.precommittedAmount().min(remaining).max(BigDecimal.ZERO);
            remaining = remaining.subtract(releasePrecommit);
            BigDecimal releaseCommit = line.committedAmount().min(remaining).max(BigDecimal.ZERO);
            jdbcTemplate.update(
                    """
                    UPDATE presupuesto.budget_lines
                    SET precommitted_amount = precommitted_amount - ?,
                        committed_amount = committed_amount - ?,
                        updated_by = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE id = ?
                    """,
                    releasePrecommit,
                    releaseCommit,
                    command.actor(),
                    Timestamp.from(now),
                    state.budgetLineId());
            jdbcTemplate.update(
                    """
                    UPDATE presupuesto.budget_control_lines
                    SET precommitted_amount = precommitted_amount - ?,
                        committed_amount = committed_amount - ?,
                        released_amount = released_amount + ?
                    WHERE id = ?
                    """,
                    releasePrecommit,
                    releaseCommit,
                    allocation.amount().amount(),
                    line.id());
            if (releasePrecommit.signum() > 0) {
                insertMovement(state.budgetLineId(), "PRECOMMITMENT_RELEASE", command.source(), new Money(releasePrecommit, currency), command.idempotencyKey(), command.actor(), now);
            }
            if (releaseCommit.signum() > 0) {
                insertMovement(state.budgetLineId(), "COMMITMENT_RELEASE", command.source(), new Money(releaseCommit, currency), command.idempotencyKey(), command.actor(), now);
            }
            affected = affected.add(allocation.amount());
            lastAvailable = availability.findStateForUpdate(allocation.dimension(), allocation.amount().currency())
                    .orElseThrow()
                    .toAvailability()
                    .available();
        }
        BudgetControlStatus nextStatus = allReleased(command.budgetControlId()) ? BudgetControlStatus.RELEASED : control.status();
        updateControlStatus(command.budgetControlId(), nextStatus, command.actor(), now);
        return new BudgetControlResult(command.budgetControlId(), nextStatus, affected, lastAvailable);
    }

    private BudgetLineState lockedLine(BudgetAllocation allocation) {
        return availability.findStateForUpdate(allocation.dimension(), allocation.amount().currency())
                .orElseThrow(() -> new BusinessRuleException("No existe una linea PIM aprobada para la dimension presupuestal."));
    }

    private void insertControlLine(long controlId, long budgetLineId, Money amount, BigDecimal precommitted, BigDecimal committed) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_control_lines (
                    budget_control_id, budget_line_id, amount, currency_code,
                    precommitted_amount, committed_amount, released_amount
                ) VALUES (?, ?, ?, ?, ?, ?, 0)
                """,
                controlId,
                budgetLineId,
                amount.amount(),
                amount.currency().name(),
                precommitted,
                committed);
    }

    private void insertMovement(
            long budgetLineId,
            String movementType,
            DocumentReference source,
            Money amount,
            IdempotencyKey idempotencyKey,
            String actor,
            Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_movements (
                    budget_line_id, movement_type, source_module, source_type, source_id,
                    source_number, amount, currency_code, idempotency_key, actor, registered_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                budgetLineId,
                movementType,
                source.module(),
                source.type(),
                source.id(),
                source.number(),
                amount.amount(),
                amount.currency().name(),
                idempotencyKey.value() + ":" + movementType,
                actor,
                Timestamp.from(now));
    }

    private long insertControl(DocumentReference source, BudgetControlStatus status, String actor, Instant now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO presupuesto.budget_controls (
                        source_module, source_type, source_id, source_number,
                        status, actor, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"});
            ps.setString(1, source.module());
            ps.setString(2, source.type());
            ps.setLong(3, source.id());
            ps.setString(4, source.number());
            ps.setString(5, status.name());
            ps.setString(6, actor);
            ps.setTimestamp(7, Timestamp.from(now));
            ps.setTimestamp(8, Timestamp.from(now));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el id del control presupuestal");
        }
        return key.longValue();
    }

    private boolean hasActivePrecommit(DocumentReference source) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM presupuesto.budget_controls
                WHERE source_module = ?
                  AND source_type = ?
                  AND source_id = ?
                  AND status = 'PRECOMMITTED'
                """,
                Integer.class,
                source.module(),
                source.type(),
                source.id());
        return count != null && count > 0;
    }

    private ControlHeader control(long controlId) {
        List<ControlHeader> rows = jdbcTemplate.query(
                "SELECT id, status FROM presupuesto.budget_controls WHERE id = ?",
                (rs, rowNum) -> new ControlHeader(
                        rs.getLong("id"),
                        BudgetControlStatus.valueOf(rs.getString("status"))),
                controlId);
        if (rows.isEmpty()) {
            throw new BusinessRuleException("No existe el control presupuestal.");
        }
        return rows.get(0);
    }

    private ControlLine controlLine(long controlId, long budgetLineId) {
        List<ControlLine> rows = jdbcTemplate.query(
                """
                SELECT id, precommitted_amount, committed_amount, released_amount
                FROM presupuesto.budget_control_lines
                WHERE budget_control_id = ?
                  AND budget_line_id = ?
                """,
                (rs, rowNum) -> new ControlLine(
                        rs.getLong("id"),
                        rs.getBigDecimal("precommitted_amount"),
                        rs.getBigDecimal("committed_amount"),
                        rs.getBigDecimal("released_amount")),
                controlId,
                budgetLineId);
        if (rows.isEmpty()) {
            throw new BusinessRuleException("La linea no pertenece al control presupuestal.");
        }
        return rows.get(0);
    }

    private boolean allReleased(long controlId) {
        Boolean allReleased = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) = 0
                FROM presupuesto.budget_control_lines
                WHERE budget_control_id = ?
                  AND precommitted_amount + committed_amount > 0
                """,
                Boolean.class,
                controlId);
        return Boolean.TRUE.equals(allReleased);
    }

    private void updateControlStatus(long controlId, BudgetControlStatus status, String actor, Instant now) {
        jdbcTemplate.update(
                """
                UPDATE presupuesto.budget_controls
                SET status = ?,
                    actor = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                status.name(),
                actor,
                Timestamp.from(now),
                controlId);
    }

    private <T> BudgetControlResult withIdempotency(String scope, IdempotencyKey key, T command, BudgetOperation operation) {
        Instant now = Instant.now();
        var claim = idempotency.acquire(scope, key, requestHash(command), now);
        if (claim.status() == IdempotencyClaimStatus.COMPLETED) {
            return replay(claim.storedResponse());
        }
        if (claim.status() == IdempotencyClaimStatus.IN_PROGRESS) {
            throw new BusinessRuleException("La operacion presupuestal ya esta en proceso.");
        }
        try {
            BudgetControlResult result = operation.execute();
            idempotency.complete(claim.claimId(), stored(result), now);
            return result;
        } catch (RuntimeException ex) {
            idempotency.release(claim.claimId());
            throw ex;
        }
    }

    private void validate(DocumentReference source, List<BudgetAllocation> allocations, IdempotencyKey idempotencyKey, String actor) {
        Objects.requireNonNull(source, "source es obligatorio");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey es obligatorio");
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations es obligatorio");
        }
        commonCurrency(allocations);
    }

    private Moneda commonCurrency(List<BudgetAllocation> allocations) {
        Moneda currency = allocations.get(0).amount().currency();
        for (BudgetAllocation allocation : allocations) {
            if (allocation.amount().amount().signum() <= 0) {
                throw new IllegalArgumentException("amount debe ser positivo");
            }
            if (allocation.amount().currency() != currency) {
                throw new IllegalArgumentException("Todas las asignaciones deben usar la misma moneda.");
            }
        }
        return currency;
    }

    private Money zero(Moneda currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    private <T> String requestHash(T command) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(command);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de la operacion presupuestal", ex);
        }
    }

    private StoredResponse stored(BudgetControlResult result) {
        try {
            return new StoredResponse(200, JSON_CONTENT_TYPE, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la respuesta del control presupuestal", ex);
        }
    }

    private BudgetControlResult replay(StoredResponse response) {
        try {
            return objectMapper.readValue(response.body(), BudgetControlResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo leer la respuesta idempotente del control presupuestal", ex);
        }
    }

    private record ControlHeader(long id, BudgetControlStatus status) {
    }

    private record ControlLine(
            long id,
            BigDecimal precommittedAmount,
            BigDecimal committedAmount,
            BigDecimal releasedAmount) {

        BigDecimal openAmount() {
            return precommittedAmount.add(committedAmount);
        }
    }

    @FunctionalInterface
    private interface BudgetOperation {
        BudgetControlResult execute();
    }
}

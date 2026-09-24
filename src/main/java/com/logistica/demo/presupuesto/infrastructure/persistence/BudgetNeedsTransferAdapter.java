package com.logistica.demo.presupuesto.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferCommand;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferLine;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferMonth;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferPort;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.presupuesto.api.BudgetTransferResult;
import com.logistica.demo.presupuesto.api.TransferNeedsCommand;
import com.logistica.demo.presupuesto.api.TransferNeedsToBudgetUseCase;
import com.logistica.demo.presupuesto.api.TransferredNeedLine;
import com.logistica.demo.presupuesto.api.TransferredNeedMonth;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
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
public class BudgetNeedsTransferAdapter implements TransferNeedsToBudgetUseCase, NeedsBudgetTransferPort {

    private static final String OPERATION = "budget.transfer";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final JdbcTemplate jdbcTemplate;
    private final IdempotencyPort idempotency;
    private final ObjectMapper objectMapper;

    public BudgetNeedsTransferAdapter(JdbcTemplate jdbcTemplate, IdempotencyPort idempotency) {
        this.jdbcTemplate = jdbcTemplate;
        this.idempotency = idempotency;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Override
    @Transactional
    public NeedsBudgetTransferResult transfer(NeedsBudgetTransferCommand command) {
        BudgetTransferResult result = transfer(new TransferNeedsCommand(
                command.consolidationId(),
                command.companyId(),
                command.fiscalYear(),
                command.lines().stream().map(this::toBudgetLine).toList(),
                command.idempotencyKey(),
                command.actor()));
        return new NeedsBudgetTransferResult(
                result.transferId(),
                result.unitBudgetExerciseId(),
                result.transferredLines(),
                result.replayed());
    }

    @Override
    @Transactional
    public BudgetTransferResult transfer(TransferNeedsCommand command) {
        validate(command);
        Instant now = Instant.now();
        var claim = idempotency.acquire(OPERATION, command.idempotencyKey(), requestHash(command), now);
        if (claim.status() == IdempotencyClaimStatus.COMPLETED) {
            return replay(claim.storedResponse());
        }
        if (claim.status() == IdempotencyClaimStatus.IN_PROGRESS) {
            throw new BusinessRuleException("La transferencia de Presupuesto ya esta en proceso.");
        }

        try {
            BudgetTransferResult existing = findExistingTransfer(command.consolidationId());
            if (existing != null) {
                BudgetTransferResult replayed = new BudgetTransferResult(
                        existing.transferId(),
                        existing.unitBudgetExerciseId(),
                        existing.transferredLines(),
                        true);
                idempotency.complete(claim.claimId(), stored(replayed), now);
                return replayed;
            }

            long exerciseId = ensureUnitBudgetExercise(command.companyId(), command.fiscalYear(), command.actor(), now);
            long transferId = insertTransfer(command, exerciseId, now);
            for (TransferredNeedLine line : command.lines()) {
                transferLine(command, transferId, exerciseId, line, now);
            }
            BudgetTransferResult result = new BudgetTransferResult(transferId, exerciseId, command.lines().size(), false);
            idempotency.complete(claim.claimId(), stored(result), now);
            return result;
        } catch (RuntimeException ex) {
            idempotency.release(claim.claimId());
            throw ex;
        }
    }

    private void transferLine(
            TransferNeedsCommand command,
            long transferId,
            long exerciseId,
            TransferredNeedLine line,
            Instant now) {
        for (TransferredNeedMonth month : line.months()) {
            FiscalDimension dimension = month.dimension();
            long budgetLineId = ensureBudgetLine(exerciseId, dimension, month.estimatedAmount().currency(), command.actor(), now);
            increaseAssignedAmount(budgetLineId, month.estimatedAmount());
            insertMovement(command, budgetLineId, month.estimatedAmount(), now);
            insertTransferLine(transferId, budgetLineId, line, month);
        }
    }

    private TransferredNeedLine toBudgetLine(NeedsBudgetTransferLine line) {
        return new TransferredNeedLine(
                line.needsPlanId(),
                line.needsLineId(),
                line.catalogItemId(),
                line.months().stream().map(this::toBudgetMonth).toList());
    }

    private TransferredNeedMonth toBudgetMonth(NeedsBudgetTransferMonth month) {
        return new TransferredNeedMonth(month.dimension(), month.approvedQuantity(), month.estimatedAmount());
    }

    private long ensureUnitBudgetExercise(Long companyId, int fiscalYear, String actor, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_exercises (
                    company_id, fiscal_year, exercise_type, status, approved_at,
                    created_by, created_at, updated_by, updated_at
                )
                SELECT ?, ?, 'UNIDADES', 'APPROVED', ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM presupuesto.budget_exercises
                    WHERE company_id = ? AND fiscal_year = ? AND exercise_type = 'UNIDADES'
                )
                """,
                companyId,
                fiscalYear,
                timestamp(now),
                actor,
                timestamp(now),
                actor,
                timestamp(now),
                companyId,
                fiscalYear);
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM presupuesto.budget_exercises
                WHERE company_id = ? AND fiscal_year = ? AND exercise_type = 'UNIDADES'
                """,
                Long.class,
                companyId,
                fiscalYear);
    }

    private long insertTransfer(TransferNeedsCommand command, long exerciseId, Instant now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO presupuesto.budget_transfers (
                        consolidation_id, company_id, fiscal_year, unit_budget_exercise_id,
                        idempotency_key, transferred_lines, actor, transferred_at,
                        created_by, created_at, updated_by, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"});
            ps.setLong(1, command.consolidationId());
            ps.setLong(2, command.companyId());
            ps.setInt(3, command.fiscalYear());
            ps.setLong(4, exerciseId);
            ps.setString(5, command.idempotencyKey().value());
            ps.setInt(6, command.lines().size());
            ps.setString(7, command.actor());
            ps.setTimestamp(8, timestamp(now));
            ps.setString(9, command.actor());
            ps.setTimestamp(10, timestamp(now));
            ps.setString(11, command.actor());
            ps.setTimestamp(12, timestamp(now));
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    private long ensureBudgetLine(
            long exerciseId,
            FiscalDimension dimension,
            Moneda currency,
            String actor,
            Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_lines (
                    budget_exercise_id, company_id, fiscal_year, month,
                    cost_center_id, financing_source_id, goal_id, expense_classifier_id,
                    currency_code, assigned_amount, precommitted_amount, committed_amount,
                    created_by, created_at, updated_by, updated_at
                )
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM presupuesto.budget_lines
                    WHERE budget_exercise_id = ?
                      AND month = ?
                      AND cost_center_id = ?
                      AND financing_source_id = ?
                      AND goal_id = ?
                      AND expense_classifier_id = ?
                      AND currency_code = ?
                )
                """,
                exerciseId,
                dimension.companyId(),
                dimension.year(),
                dimension.month(),
                dimension.costCenterId(),
                dimension.financingSourceId(),
                dimension.goalId(),
                dimension.expenseClassifierId(),
                currency.name(),
                actor,
                timestamp(now),
                actor,
                timestamp(now),
                exerciseId,
                dimension.month(),
                dimension.costCenterId(),
                dimension.financingSourceId(),
                dimension.goalId(),
                dimension.expenseClassifierId(),
                currency.name());
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM presupuesto.budget_lines
                WHERE budget_exercise_id = ?
                  AND month = ?
                  AND cost_center_id = ?
                  AND financing_source_id = ?
                  AND goal_id = ?
                  AND expense_classifier_id = ?
                  AND currency_code = ?
                """,
                Long.class,
                exerciseId,
                dimension.month(),
                dimension.costCenterId(),
                dimension.financingSourceId(),
                dimension.goalId(),
                dimension.expenseClassifierId(),
                currency.name());
    }

    private void increaseAssignedAmount(long budgetLineId, Money amount) {
        jdbcTemplate.update(
                """
                UPDATE presupuesto.budget_lines
                SET assigned_amount = assigned_amount + ?,
                    updated_at = ?
                WHERE id = ?
                """,
                amount.amount(),
                timestamp(Instant.now()),
                budgetLineId);
    }

    private void insertMovement(TransferNeedsCommand command, long budgetLineId, Money amount, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_movements (
                    budget_line_id, movement_type, source_module, source_type, source_id,
                    source_number, amount, currency_code, actor, registered_at
                ) VALUES (?, 'ASSIGNMENT', 'CUADRO', 'CONSOLIDATION', ?, ?, ?, ?, ?, ?)
                """,
                budgetLineId,
                command.consolidationId(),
                "CN-CONS-" + command.consolidationId(),
                amount.amount(),
                amount.currency().name(),
                command.actor(),
                timestamp(now));
    }

    private void insertTransferLine(
            long transferId,
            long budgetLineId,
            TransferredNeedLine line,
            TransferredNeedMonth month) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_transfer_lines (
                    transfer_id, budget_line_id, needs_plan_id, needs_line_id,
                    catalog_item_id, month, approved_quantity, amount
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                transferId,
                budgetLineId,
                line.needsPlanId(),
                line.needsLineId(),
                line.catalogItemId(),
                month.dimension().month(),
                month.approvedQuantity(),
                month.estimatedAmount().amount());
    }

    private BudgetTransferResult findExistingTransfer(Long consolidationId) {
        List<BudgetTransferResult> rows = jdbcTemplate.query(
                """
                SELECT id, unit_budget_exercise_id, transferred_lines
                FROM presupuesto.budget_transfers
                WHERE consolidation_id = ?
                """,
                (rs, rowNum) -> new BudgetTransferResult(
                        rs.getLong("id"),
                        rs.getLong("unit_budget_exercise_id"),
                        rs.getInt("transferred_lines"),
                        true),
                consolidationId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void validate(TransferNeedsCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        requireId(command.consolidationId(), "consolidationId");
        requireId(command.companyId(), "companyId");
        if (command.fiscalYear() < 2000 || command.fiscalYear() > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        if (command.lines().isEmpty()) {
            throw new IllegalArgumentException("La transferencia debe contener lineas.");
        }
        if (command.actor() == null || command.actor().isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        for (TransferredNeedLine line : command.lines()) {
            requireId(line.needsPlanId(), "needsPlanId");
            requireId(line.needsLineId(), "needsLineId");
            requireId(line.catalogItemId(), "catalogItemId");
            if (line.months().isEmpty()) {
                throw new IllegalArgumentException("Cada linea transferida debe contener meses.");
            }
            for (TransferredNeedMonth month : line.months()) {
                Objects.requireNonNull(month.dimension(), "dimension es obligatoria");
                if (!command.companyId().equals(month.dimension().companyId()) || command.fiscalYear() != month.dimension().year()) {
                    throw new IllegalArgumentException("La dimension transferida no pertenece a la compania y ejercicio del comando.");
                }
                if (month.approvedQuantity() == null || month.approvedQuantity().signum() < 0) {
                    throw new IllegalArgumentException("approvedQuantity no puede ser negativa.");
                }
                Money amount = Objects.requireNonNull(month.estimatedAmount(), "estimatedAmount es obligatorio");
                if (amount.amount().signum() < 0) {
                    throw new IllegalArgumentException("estimatedAmount no puede ser negativo.");
                }
            }
        }
    }

    private String requestHash(TransferNeedsCommand command) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(command);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de la transferencia presupuestal", ex);
        }
    }

    private StoredResponse stored(BudgetTransferResult result) {
        try {
            return new StoredResponse(200, JSON_CONTENT_TYPE, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la respuesta de transferencia presupuestal", ex);
        }
    }

    private BudgetTransferResult replay(StoredResponse response) {
        try {
            BudgetTransferResult result = objectMapper.readValue(response.body(), BudgetTransferResult.class);
            return new BudgetTransferResult(
                    result.transferId(),
                    result.unitBudgetExerciseId(),
                    result.transferredLines(),
                    true);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo leer la respuesta idempotente de transferencia presupuestal", ex);
        }
    }

    private static void requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el id generado");
        }
        return key.longValue();
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }
}

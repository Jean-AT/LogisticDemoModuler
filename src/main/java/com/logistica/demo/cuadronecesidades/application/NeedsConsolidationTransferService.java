package com.logistica.demo.cuadronecesidades.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferCommand;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferLine;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferMonth;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferPort;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.cuadronecesidades.api.event.NeedsConsolidationTransferredEvent;
import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.ProgramacionMensualNecesidad;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.ConsolidacionCuadroRepository;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.event.OutboxPort;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyClaimStatus;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyPort;
import com.logistica.demo.sharedkernel.idempotency.StoredResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NeedsConsolidationTransferService {

    private static final String OPERATION = "needs.consolidation.transfer";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final ConsolidacionCuadroRepository consolidations;
    private final ObjectProvider<NeedsBudgetTransferPort> budgetTransfers;
    private final IdempotencyPort idempotency;
    private final OutboxPort outbox;
    private final ObjectMapper objectMapper;

    public NeedsConsolidationTransferService(
            ConsolidacionCuadroRepository consolidations,
            ObjectProvider<NeedsBudgetTransferPort> budgetTransfers,
            IdempotencyPort idempotency,
            OutboxPort outbox) {
        this.consolidations = consolidations;
        this.budgetTransfers = budgetTransfers;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Transactional
    public NeedsBudgetTransferResult transfer(
            Long consolidationId,
            IdempotencyKey idempotencyKey,
            String actor,
            OffsetDateTime now) {
        ConsolidacionCuadro consolidation = consolidations.findById(consolidationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consolidacion no encontrada: " + consolidationId));
        NeedsBudgetTransferCommand command = toBudgetCommand(consolidation, idempotencyKey, actor);
        var claim = idempotency.acquire(OPERATION, idempotencyKey, requestHash(command), now.toInstant());
        if (claim.status() == IdempotencyClaimStatus.COMPLETED) {
            return replay(claim.storedResponse());
        }
        if (claim.status() == IdempotencyClaimStatus.IN_PROGRESS) {
            throw new BusinessRuleException("La transferencia del consolidado ya esta en proceso.");
        }

        try {
            NeedsBudgetTransferResult result = budgetTransferUseCase().transfer(command);
            consolidation.markTransferred(result.transferId(), result.unitBudgetExerciseId(), now);
            consolidations.save(consolidation);
            outbox.append(new NeedsConsolidationTransferredEvent(
                    UUID.randomUUID(),
                    now.toInstant(),
                    consolidation.getId(),
                    result.transferId(),
                    consolidation.getCompanyId(),
                    consolidation.getFiscalYear()));
            idempotency.complete(claim.claimId(), stored(result), now.toInstant());
            return result;
        } catch (RuntimeException ex) {
            idempotency.release(claim.claimId());
            throw ex;
        }
    }

    private NeedsBudgetTransferCommand toBudgetCommand(
            ConsolidacionCuadro consolidation,
            IdempotencyKey idempotencyKey,
            String actor) {
        List<NeedsBudgetTransferLine> lines = consolidation.getSources().stream()
                .flatMap(source -> source.getPlan().getDetails().stream().map(detail ->
                        transferredLine(source.getPlan(), detail)))
                .toList();
        return new NeedsBudgetTransferCommand(
                consolidation.getId(),
                consolidation.getCompanyId(),
                consolidation.getFiscalYear(),
                lines,
                idempotencyKey,
                actor);
    }

    private NeedsBudgetTransferLine transferredLine(CuadroNecesidad plan, CuadroNecesidadDetalle detail) {
        List<NeedsBudgetTransferMonth> months = detail.getMonthlyNeeds().stream()
                .map(month -> transferredMonth(plan, detail, month))
                .toList();
        return new NeedsBudgetTransferLine(
                plan.getId(),
                detail.getId(),
                detail.getCatalogItemId(),
                months);
    }

    private NeedsBudgetTransferMonth transferredMonth(
            CuadroNecesidad plan,
            CuadroNecesidadDetalle detail,
            ProgramacionMensualNecesidad month) {
        return new NeedsBudgetTransferMonth(
                new FiscalDimension(
                        plan.getCompanyId(),
                        plan.getFiscalYear(),
                        month.getMonth(),
                        plan.getCostCenterId(),
                        plan.getFinancingSourceId(),
                        plan.getGoalId(),
                        detail.getExpenseClassifierId()),
                month.getApprovedQuantity(),
                new Money(month.getApprovedQuantity().multiply(detail.getEstimatedUnitPrice()), Moneda.PEN));
    }

    private NeedsBudgetTransferPort budgetTransferUseCase() {
        return budgetTransfers.getIfAvailable(() -> {
            throw new BusinessRuleException("El modulo de Presupuesto no esta disponible para recibir transferencias.");
        });
    }

    private String requestHash(NeedsBudgetTransferCommand command) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(command);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("No se pudo calcular la huella de la transferencia", ex);
        }
    }

    private StoredResponse stored(NeedsBudgetTransferResult result) {
        try {
            return new StoredResponse(200, JSON_CONTENT_TYPE, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudo serializar el resultado de transferencia", ex);
        }
    }

    private NeedsBudgetTransferResult replay(StoredResponse response) {
        try {
            NeedsBudgetTransferResult result = objectMapper.readValue(response.body(), NeedsBudgetTransferResult.class);
            return new NeedsBudgetTransferResult(
                    result.transferId(),
                    result.unitBudgetExerciseId(),
                    result.transferredLines(),
                    true);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo leer la respuesta idempotente de transferencia", ex);
        }
    }
}

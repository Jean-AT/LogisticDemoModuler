package com.logistica.demo.presupuesto.controller;

import com.logistica.demo.presupuesto.api.ApproveBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetAvailabilityQuery;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.BudgetPlanResult;
import com.logistica.demo.presupuesto.api.BudgetPlanUseCase;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.presupuesto.api.GenerateBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.presupuesto.api.ReleaseBudgetCommand;
import com.logistica.demo.presupuesto.api.ReviewBudgetPlanCommand;
import com.logistica.demo.presupuesto.dto.BudgetAllocationRequest;
import com.logistica.demo.presupuesto.dto.BudgetAvailabilityResponse;
import com.logistica.demo.presupuesto.dto.BudgetControlRequest;
import com.logistica.demo.presupuesto.dto.BudgetDimensionRequest;
import com.logistica.demo.presupuesto.dto.BudgetDocumentRequest;
import com.logistica.demo.presupuesto.dto.BudgetPlanRequest;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/budget")
public class BudgetController {

    private final BudgetPlanUseCase budgetPlan;
    private final BudgetAvailabilityQuery availabilityQuery;
    private final BudgetControlUseCase budgetControl;
    private final CurrentUserService currentUserService;

    public BudgetController(
            BudgetPlanUseCase budgetPlan,
            BudgetAvailabilityQuery availabilityQuery,
            BudgetControlUseCase budgetControl,
            CurrentUserService currentUserService) {
        this.budgetPlan = budgetPlan;
        this.availabilityQuery = availabilityQuery;
        this.budgetControl = budgetControl;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/plans/pia/generate")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetPlanResult generatePia(@RequestBody BudgetPlanRequest request) {
        return budgetPlan.generatePia(new GenerateBudgetPlanCommand(
                request.companyId(),
                request.fiscalYear(),
                currentUserService.getUsername()));
    }

    @PostMapping("/plans/pia/review")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetPlanResult reviewPia(@RequestBody BudgetPlanRequest request) {
        return budgetPlan.reviewPia(new ReviewBudgetPlanCommand(
                request.companyId(),
                request.fiscalYear(),
                currentUserService.getUsername(),
                request.notes()));
    }

    @PostMapping("/plans/pia/approve")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetPlanResult approvePia(@RequestBody BudgetPlanRequest request) {
        return budgetPlan.approvePiaAndCreateInitialPim(new ApproveBudgetPlanCommand(
                request.companyId(),
                request.fiscalYear(),
                currentUserService.getUsername()));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR', 'ADMIN')")
    public BudgetAvailabilityResponse availability(
            @RequestParam Long companyId,
            @RequestParam int fiscalYear,
            @RequestParam int month,
            @RequestParam Long costCenterId,
            @RequestParam Long financingSourceId,
            @RequestParam Long goalId,
            @RequestParam Long expenseClassifierId,
            @RequestParam String currency) {
        FiscalDimension dimension = new FiscalDimension(
                companyId,
                fiscalYear,
                month,
                costCenterId,
                financingSourceId,
                goalId,
                expenseClassifierId);
        return availabilityQuery.findAvailability(dimension, Moneda.valueOf(currency))
                .map(BudgetAvailabilityResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad presupuestal no encontrada"));
    }

    @PostMapping("/controls/precommit")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetControlResult precommit(
            @RequestBody BudgetControlRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return budgetControl.precommit(new PrecommitBudgetCommand(
                source(request.source()),
                allocations(request.allocations()),
                idempotencyKey(idempotencyKey),
                currentUserService.getUsername()));
    }

    @PostMapping("/controls/{id}/commit")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetControlResult commit(
            @PathVariable Long id,
            @RequestBody BudgetControlRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return budgetControl.commit(new CommitBudgetCommand(
                id,
                source(request.source()),
                allocations(request.allocations()),
                idempotencyKey(idempotencyKey),
                currentUserService.getUsername()));
    }

    @PostMapping("/controls/{id}/release")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public BudgetControlResult release(
            @PathVariable Long id,
            @RequestBody BudgetControlRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return budgetControl.release(new ReleaseBudgetCommand(
                id,
                source(request.source()),
                allocations(request.allocations()),
                request.reason(),
                idempotencyKey(idempotencyKey),
                currentUserService.getUsername()));
    }

    private IdempotencyKey idempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("La cabecera Idempotency-Key es obligatoria.");
        }
        return new IdempotencyKey(value);
    }

    private DocumentReference source(BudgetDocumentRequest request) {
        return new DocumentReference(request.module(), request.type(), request.id(), request.number());
    }

    private List<BudgetAllocation> allocations(List<BudgetAllocationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("allocations es obligatorio.");
        }
        return requests.stream().map(this::allocation).toList();
    }

    private BudgetAllocation allocation(BudgetAllocationRequest request) {
        BudgetDimensionRequest dimension = request.dimension();
        return new BudgetAllocation(
                new FiscalDimension(
                        dimension.companyId(),
                        dimension.fiscalYear(),
                        dimension.month(),
                        dimension.costCenterId(),
                        dimension.financingSourceId(),
                        dimension.goalId(),
                        dimension.expenseClassifierId()),
                new Money(request.amount(), Moneda.valueOf(request.currency())));
    }
}

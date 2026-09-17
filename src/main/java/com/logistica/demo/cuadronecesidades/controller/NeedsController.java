package com.logistica.demo.cuadronecesidades.controller;

import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.cuadronecesidades.application.NeedsPlanningService;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.dto.CreateNeedsPlanRequest;
import com.logistica.demo.cuadronecesidades.dto.NeedsConsolidationResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedsPlanResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedsTraceabilityResponse;
import com.logistica.demo.cuadronecesidades.dto.ReplaceNeedsDetailsRequest;
import com.logistica.demo.cuadronecesidades.dto.ReviewNeedsPlanRequest;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import com.logistica.demo.sharedkernel.web.PageResponse;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/needs")
public class NeedsController {

    private final NeedsPlanningService needsPlanning;
    private final CurrentUserService currentUserService;

    public NeedsController(NeedsPlanningService needsPlanning, CurrentUserService currentUserService) {
        this.needsPlanning = needsPlanning;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR', 'ADMIN')")
    public PageResponse<NeedsPlanResponse> listPlans(
            @RequestParam Long companyId,
            @RequestParam int fiscalYear,
            @RequestParam(required = false) EstadoCuadroNecesidad status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return needsPlanning.listPlans(companyId, fiscalYear, status, page, size);
    }

    @GetMapping("/plans/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR', 'ADMIN')")
    public NeedsPlanResponse getPlan(@PathVariable Long id) {
        return needsPlanning.getPlan(id);
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public NeedsPlanResponse createPlan(@RequestBody CreateNeedsPlanRequest request) {
        return needsPlanning.createPlan(request);
    }

    @PutMapping("/plans/{id}/details")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public NeedsPlanResponse replaceDetails(
            @PathVariable Long id,
            @RequestBody ReplaceNeedsDetailsRequest request) {
        return needsPlanning.replaceDetails(id, request, OffsetDateTime.now());
    }

    @PostMapping("/plans/{id}/submit")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public NeedsPlanResponse submit(@PathVariable Long id) {
        return needsPlanning.submit(id, OffsetDateTime.now());
    }

    @PostMapping("/plans/{id}/review")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsPlanResponse review(
            @PathVariable Long id,
            @RequestBody ReviewNeedsPlanRequest request) {
        return needsPlanning.review(id, request, OffsetDateTime.now());
    }

    @PostMapping("/plans/{id}/observe")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsPlanResponse observe(@PathVariable Long id) {
        return needsPlanning.observe(id, OffsetDateTime.now());
    }

    @PostMapping("/plans/{id}/reject")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsPlanResponse reject(@PathVariable Long id) {
        return needsPlanning.reject(id, OffsetDateTime.now());
    }

    @GetMapping("/consolidations")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public PageResponse<NeedsConsolidationResponse> listConsolidations(
            @RequestParam Long companyId,
            @RequestParam int fiscalYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return needsPlanning.listConsolidations(companyId, fiscalYear, page, size);
    }

    @GetMapping("/consolidations/{id}")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsConsolidationResponse getConsolidation(@PathVariable Long id) {
        return needsPlanning.getConsolidation(id);
    }

    @PostMapping("/consolidations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsConsolidationResponse consolidate(
            @RequestParam Long companyId,
            @RequestParam int fiscalYear) {
        return needsPlanning.consolidate(companyId, fiscalYear, OffsetDateTime.now());
    }

    @PostMapping("/consolidations/{id}/reverse")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsConsolidationResponse reverseConsolidation(@PathVariable Long id) {
        return needsPlanning.reverseConsolidation(id, OffsetDateTime.now());
    }

    @PostMapping("/consolidations/{id}/transfer")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public NeedsBudgetTransferResult transfer(
            @PathVariable Long id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("La cabecera Idempotency-Key es obligatoria.");
        }
        return needsPlanning.transfer(
                id,
                new IdempotencyKey(idempotencyKey),
                currentUserService.getUsername(),
                OffsetDateTime.now());
    }

    @GetMapping("/balances/{lineId}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR', 'ADMIN')")
    public NeedsLineBalance findBalance(@RequestParam Long companyId, @PathVariable Long lineId) {
        return needsPlanning.findBalance(companyId, lineId);
    }

    @GetMapping("/traceability/plans/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR', 'ADMIN')")
    public NeedsTraceabilityResponse tracePlan(@PathVariable Long id) {
        return needsPlanning.tracePlan(id);
    }
}

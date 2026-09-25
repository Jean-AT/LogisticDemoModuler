package com.logistica.demo.logistica.consultas.controller;

import com.logistica.demo.logistica.consultas.dto.LogisticsDashboardResponse;
import com.logistica.demo.logistica.consultas.dto.LogisticsTraceabilityResponse;
import com.logistica.demo.logistica.consultas.service.LogisticsQueryService;
import com.logistica.demo.shared.report.PdfHeaderData;
import com.logistica.demo.shared.report.PdfHeaderRequest;
import com.logistica.demo.shared.report.PdfReportService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/logistica", ApiPaths.V1 + "/logistica"})
public class LogisticsQueryController {

    private final LogisticsQueryService logisticsQueryService;
    private final PdfReportService pdfReportService;

    public LogisticsQueryController(
            LogisticsQueryService logisticsQueryService,
            PdfReportService pdfReportService) {
        this.logisticsQueryService = logisticsQueryService;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('COMPRAS', 'APROBADOR', 'ADMIN')")
    public LogisticsDashboardResponse dashboard() {
        return logisticsQueryService.dashboard();
    }

    @GetMapping("/trazabilidad/requerimientos/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS', 'APROBADOR', 'ADMIN')")
    public LogisticsTraceabilityResponse traceRequirement(@PathVariable Long id) {
        return logisticsQueryService.traceRequirement(id);
    }

    @GetMapping("/trazabilidad/requerimientos/{id}/pdf")
    @PreAuthorize("hasAnyRole('COMPRAS', 'APROBADOR', 'ADMIN')")
    public ResponseEntity<byte[]> traceRequirementPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String areaSolicitante,
            @RequestParam(required = false) String oficinaQueAprueba,
            @RequestParam(required = false) String asunto,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String fechaDocumento,
            @RequestParam(required = false) String destinatario,
            @RequestParam(required = false) String cargoDestinatario,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String pieFirma) {
        LogisticsTraceabilityResponse traceability = logisticsQueryService.traceRequirement(id);
        PdfHeaderData headerData = PdfHeaderRequest.of(
                entidad, areaSolicitante, oficinaQueAprueba, asunto, referencia,
                fechaDocumento, destinatario, cargoDestinatario, observaciones, pieFirma);
        byte[] pdf = pdfReportService.buildLogisticsTraceabilityPdf(traceability, headerData);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"trazabilidad-%s.pdf\"".formatted(
                        traceability.requerimientoNumero()))
                .body(pdf);
    }
}

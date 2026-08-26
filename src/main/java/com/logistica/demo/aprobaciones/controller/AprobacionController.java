package com.logistica.demo.aprobaciones.controller;

import com.logistica.demo.aprobaciones.dto.AprobacionDecisionRequest;
import com.logistica.demo.aprobaciones.service.AprobacionService;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.shared.dto.PageResponse;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.report.PdfHeaderData;
import com.logistica.demo.shared.report.PdfHeaderRequest;
import com.logistica.demo.shared.report.PdfReportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aprobaciones")
public class AprobacionController {

    private final AprobacionService aprobacionService;
    private final PdfReportService pdfReportService;

    public AprobacionController(AprobacionService aprobacionService, PdfReportService pdfReportService) {
        this.aprobacionService = aprobacionService;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN', 'COMPRAS')")
    public PageResponse<RequerimientoResponse> list(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) EstadoRequerimiento estado,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return aprobacionService.list(
                id,
                estado,
                numero,
                proveedorId,
                parseDateFilter(fechaDesde, "fechaDesde"),
                parseDateFilter(fechaHasta, "fechaHasta"),
                page,
                size);
    }

    @GetMapping("/{requerimientoId}/pdf")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN', 'COMPRAS')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long requerimientoId,
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
        RequerimientoResponse requerimiento = aprobacionService.getByRequerimientoId(requerimientoId);
        PdfHeaderData headerData = PdfHeaderRequest.of(
                entidad, areaSolicitante, oficinaQueAprueba, asunto, referencia,
                fechaDocumento, destinatario, cargoDestinatario, observaciones, pieFirma);
        byte[] pdf = pdfReportService.buildAprobacionPdf(requerimiento, headerData);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"aprobacion-%s.pdf\"".formatted(requerimiento.numero()))
                .body(pdf);
    }

    @PostMapping("/{requerimientoId}/aprobar")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public RequerimientoResponse aprobar(
            @PathVariable Long requerimientoId,
            @RequestBody(required = false) AprobacionDecisionRequest request) {
        return aprobacionService.aprobar(requerimientoId, request);
    }

    @PostMapping("/{requerimientoId}/observar")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public RequerimientoResponse observar(
            @PathVariable Long requerimientoId,
            @RequestBody AprobacionDecisionRequest request) {
        return aprobacionService.observar(requerimientoId, request);
    }

    @PostMapping("/{requerimientoId}/rechazar")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMIN')")
    public RequerimientoResponse rechazar(
            @PathVariable Long requerimientoId,
            @RequestBody AprobacionDecisionRequest request) {
        return aprobacionService.rechazar(requerimientoId, request);
    }

    private LocalDate parseDateFilter(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value).toLocalDate();
            } catch (DateTimeParseException ex) {
                throw new BadRequestException(
                        "Parametro '%s' invalido. Use formato yyyy-MM-dd o yyyy-MM-ddTHH:mm:ss.".formatted(fieldName));
            }
        }
    }
}

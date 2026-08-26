package com.logistica.demo.requerimientos.controller;

import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.dto.RequerimientoCreateRequest;
import com.logistica.demo.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.requerimientos.service.RequerimientoService;
import com.logistica.demo.shared.dto.PageResponse;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.report.PdfHeaderData;
import com.logistica.demo.shared.report.PdfHeaderRequest;
import com.logistica.demo.shared.report.PdfReportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requerimientos")
public class RequerimientoController {

    private final RequerimientoService requerimientoService;
    private final PdfReportService pdfReportService;

    public RequerimientoController(RequerimientoService requerimientoService, PdfReportService pdfReportService) {
        this.requerimientoService = requerimientoService;
        this.pdfReportService = pdfReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public RequerimientoResponse create(@RequestBody RequerimientoCreateRequest request) {
        return requerimientoService.create(request);
    }

    @GetMapping
    public PageResponse<RequerimientoResponse> list(
            @RequestParam(required = false) EstadoRequerimiento estado,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return requerimientoService.list(
                estado,
                numero,
                proveedorId,
                parseDateFilter(fechaDesde, "fechaDesde"),
                parseDateFilter(fechaHasta, "fechaHasta"),
                page,
                size);
    }

    @GetMapping("/{id}")
    public RequerimientoResponse getById(@PathVariable Long id) {
        return requerimientoService.getById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
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
        RequerimientoResponse requerimiento = requerimientoService.getById(id);
        PdfHeaderData headerData = PdfHeaderRequest.of(
                entidad, areaSolicitante, oficinaQueAprueba, asunto, referencia,
                fechaDocumento, destinatario, cargoDestinatario, observaciones, pieFirma);
        byte[] pdf = pdfReportService.buildRequerimientoPdf(requerimiento, headerData);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"requerimiento-%s.pdf\"".formatted(requerimiento.numero()))
                .body(pdf);
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public RequerimientoResponse enviar(@PathVariable Long id) {
        return requerimientoService.enviar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'ADMIN')")
    public RequerimientoResponse update(@PathVariable Long id, @RequestBody RequerimientoCreateRequest request) {
        return requerimientoService.update(id, request);
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

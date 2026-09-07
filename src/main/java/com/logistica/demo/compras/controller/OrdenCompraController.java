package com.logistica.demo.compras.controller;

import com.logistica.demo.compras.dto.OrdenCompraResponse;
import com.logistica.demo.compras.service.OrdenCompraService;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import com.logistica.demo.sharedkernel.web.PageResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/ordenes-compra", ApiPaths.V1 + "/ordenes-compra"})
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;
    private final PdfReportService pdfReportService;

    public OrdenCompraController(OrdenCompraService ordenCompraService, PdfReportService pdfReportService) {
        this.ordenCompraService = ordenCompraService;
        this.pdfReportService = pdfReportService;
    }

    @PostMapping("/desde-requerimiento/{requerimientoId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public OrdenCompraResponse generarDesdeRequerimiento(@PathVariable Long requerimientoId) {
        return ordenCompraService.generarDesdeRequerimiento(requerimientoId);
    }

    @GetMapping
    public PageResponse<OrdenCompraResponse> listar(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Moneda moneda,
            @RequestParam(required = false) Long requerimientoId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ordenCompraService.listar(
                numero,
                proveedorId,
                moneda,
                requerimientoId,
                parseDateFilter(fechaDesde, "fechaDesde"),
                parseDateFilter(fechaHasta, "fechaHasta"),
                page,
                size);
    }

    @GetMapping("/{id}")
    public OrdenCompraResponse getById(@PathVariable Long id) {
        return ordenCompraService.getById(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
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
        OrdenCompraResponse ordenCompra = ordenCompraService.getById(id);
        PdfHeaderData headerData = PdfHeaderRequest.of(
                entidad, areaSolicitante, oficinaQueAprueba, asunto, referencia,
                fechaDocumento, destinatario, cargoDestinatario, observaciones, pieFirma);
        byte[] pdf = pdfReportService.buildOrdenCompraPdf(ordenCompra, headerData);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"orden-compra-%s.pdf\"".formatted(ordenCompra.numero()))
                .body(pdf);
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

package com.logistica.demo.shared.report;

import java.util.Map;

public record PdfHeaderRequest(
        String entidad,
        String areaSolicitante,
        String oficinaQueAprueba,
        String asunto,
        String referencia,
        String fechaDocumento,
        String destinatario,
        String cargoDestinatario,
        String observaciones,
        String pieFirma) {

    public PdfHeaderData toData() {
        return new PdfHeaderData(
                normalize(entidad),
                normalize(areaSolicitante),
                normalize(oficinaQueAprueba),
                normalize(asunto),
                normalize(referencia),
                normalize(fechaDocumento),
                normalize(destinatario),
                normalize(cargoDestinatario),
                normalize(observaciones),
                normalize(pieFirma));
    }

    public static PdfHeaderData fromQueryParams(Map<String, String> params) {
        return new PdfHeaderRequest(
                params.get("entidad"),
                params.get("areaSolicitante"),
                params.get("oficinaQueAprueba"),
                params.get("asunto"),
                params.get("referencia"),
                params.get("fechaDocumento"),
                params.get("destinatario"),
                params.get("cargoDestinatario"),
                params.get("observaciones"),
                params.get("pieFirma")).toData();
    }

    public static PdfHeaderData of(
            String entidad,
            String areaSolicitante,
            String oficinaQueAprueba,
            String asunto,
            String referencia,
            String fechaDocumento,
            String destinatario,
            String cargoDestinatario,
            String observaciones,
            String pieFirma) {
        return new PdfHeaderRequest(
                entidad,
                areaSolicitante,
                oficinaQueAprueba,
                asunto,
                referencia,
                fechaDocumento,
                destinatario,
                cargoDestinatario,
                observaciones,
                pieFirma).toData();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

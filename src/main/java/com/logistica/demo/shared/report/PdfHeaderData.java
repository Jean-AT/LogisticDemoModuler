package com.logistica.demo.shared.report;

public record PdfHeaderData(
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

    public boolean isEmpty() {
        return isBlank(entidad)
                && isBlank(areaSolicitante)
                && isBlank(oficinaQueAprueba)
                && isBlank(asunto)
                && isBlank(referencia)
                && isBlank(fechaDocumento)
                && isBlank(destinatario)
                && isBlank(cargoDestinatario)
                && isBlank(observaciones)
                && isBlank(pieFirma);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

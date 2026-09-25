package com.logistica.demo.shared.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.logistica.consultas.dto.LogisticsTraceabilityEventResponse;
import com.logistica.demo.logistica.consultas.dto.LogisticsTraceabilityResponse;
import com.logistica.demo.shared.config.DemoReportProperties;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfReportServiceTest {

    @Test
    void shouldGenerateLogisticsTraceabilityPdf() throws Exception {
        PdfReportService service = new PdfReportService(new DemoReportProperties("Logistica Demo SAC"));
        LogisticsTraceabilityResponse traceability = new LogisticsTraceabilityResponse(
                90L,
                "REQ-000090",
                "CONVERTIDO_OC",
                44L,
                22L,
                700L,
                600L,
                "OC-000600",
                "RECIBIDA",
                List.of(new LogisticsTraceabilityEventResponse(
                        "RECEPCION",
                        "REGISTRADA",
                        900L,
                        "REC-000900",
                        "compras",
                        LocalDateTime.parse("2026-09-25T09:30:00"),
                        "Orden OC-000600")));

        byte[] pdf = service.buildLogisticsTraceabilityPdf(traceability, null);

        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        }
        assertTrue(text.contains("TRAZABILIDAD LOGISTICA"));
        assertTrue(text.contains("REQ-000090"));
        assertTrue(text.contains("RECEPCION"));
        assertTrue(text.contains("REC-000900"));
    }
}

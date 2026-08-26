package com.logistica.demo.shared.report;

import com.logistica.demo.aprobaciones.dto.AprobacionResponse;
import com.logistica.demo.compras.dto.OrdenCompraDetalleResponse;
import com.logistica.demo.compras.dto.OrdenCompraResponse;
import com.logistica.demo.requerimientos.dto.RequerimientoDetalleResponse;
import com.logistica.demo.requerimientos.dto.RequerimientoEstadoHistorialResponse;
import com.logistica.demo.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.shared.config.DemoReportProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class PdfReportService {

    private static final float PAGE_MARGIN = 48f;
    private static final float PAGE_BOTTOM_MARGIN = 48f;
    private static final float LINE_HEIGHT = 14f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private final DemoReportProperties reportProperties;

    public PdfReportService(DemoReportProperties reportProperties) {
        this.reportProperties = reportProperties;
    }

    public byte[] buildRequerimientoPdf(RequerimientoResponse requerimiento) {
        return buildRequerimientoPdf(requerimiento, null);
    }

    public byte[] buildRequerimientoPdf(RequerimientoResponse requerimiento, PdfHeaderData headerData) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DocumentWriter writer = new DocumentWriter(document);
            writer.writeHeader(
                    "REPORTE DE REQUERIMIENTO",
                    requerimiento.numero(),
                    headerData,
                    List.of(
                            "Empresa: " + reportProperties.companyName(),
                            "Solicitado por: " + requerimiento.createdBy(),
                            "Estado actual: " + requerimiento.estado(),
                            "Fecha de solicitud: " + formatDateTime(requerimiento.createdAt())));

            writer.writeSectionTitle("Resumen");
            writer.writeLabelValue("Descripcion", requerimiento.descripcion());
            writer.writeLabelValue("Proveedor", requerimiento.proveedor().code() + " - " + requerimiento.proveedor().name());
            writer.writeLabelValue("Moneda", requerimiento.moneda().name());
            writer.writeLabelValue("Ultima actualizacion", formatDateTime(requerimiento.updatedAt()));

            writer.writeSectionTitle("Detalle");
            for (RequerimientoDetalleResponse detalle : requerimiento.detalles()) {
                writer.writeBulletLine("%s - %s".formatted(detalle.itemCode(), detalle.itemName()));
                writer.writeSubLine("Almacen: %s | Cantidad: %s | Precio estimado: %s | Subtotal: %s".formatted(
                        detalle.almacenCode(),
                        detalle.cantidad(),
                        formatAmount(detalle.precioUnitarioEstimado()),
                        formatAmount(detalle.subtotalLinea())));
            }

            writer.writeSectionTitle("Historial de estados");
            for (RequerimientoEstadoHistorialResponse historial : requerimiento.historialEstados()) {
                writer.writeBulletLine("%s -> %s".formatted(
                        historial.estadoAnterior() == null ? "INICIAL" : historial.estadoAnterior(),
                        historial.estadoNuevo()));
                writer.writeSubLine("Usuario: %s | Fecha: %s".formatted(
                        historial.usuario(),
                        formatDateTime(historial.fechaHora())));
                if (historial.comentario() != null && !historial.comentario().isBlank()) {
                    writer.writeSubLine("Comentario: " + historial.comentario());
                }
            }

            if (!requerimiento.aprobaciones().isEmpty()) {
                writer.writeSectionTitle("Aprobaciones");
                for (AprobacionResponse aprobacion : requerimiento.aprobaciones()) {
                    writer.writeBulletLine("%s por %s".formatted(aprobacion.accion(), aprobacion.usuario()));
                    writer.writeSubLine("Fecha: %s".formatted(formatDateTime(aprobacion.decisionAt())));
                    if (aprobacion.comentario() != null && !aprobacion.comentario().isBlank()) {
                        writer.writeSubLine("Comentario: " + aprobacion.comentario());
                    }
                }
            }

            writer.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del requerimiento.", ex);
        }
    }

    public byte[] buildAprobacionPdf(RequerimientoResponse requerimiento) {
        return buildAprobacionPdf(requerimiento, null);
    }

    public byte[] buildAprobacionPdf(RequerimientoResponse requerimiento, PdfHeaderData headerData) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DocumentWriter writer = new DocumentWriter(document);
            String aprobador = requerimiento.aprobaciones().isEmpty()
                    ? "Pendiente"
                    : requerimiento.aprobaciones().get(requerimiento.aprobaciones().size() - 1).usuario();
            writer.writeHeader(
                    "REPORTE DE APROBACION",
                    requerimiento.numero(),
                    headerData,
                    List.of(
                            "Empresa: " + reportProperties.companyName(),
                            "Solicitado por: " + requerimiento.createdBy(),
                            "Gestionado por: " + aprobador,
                            "Estado actual: " + requerimiento.estado()));

            writer.writeSectionTitle("Solicitud");
            writer.writeLabelValue("Descripcion", requerimiento.descripcion());
            writer.writeLabelValue("Proveedor", requerimiento.proveedor().code() + " - " + requerimiento.proveedor().name());
            writer.writeLabelValue("Creado", formatDateTime(requerimiento.createdAt()));

            writer.writeSectionTitle("Decisiones de aprobacion");
            if (requerimiento.aprobaciones().isEmpty()) {
                writer.writeSubLine("No hay decisiones registradas todavia.");
            } else {
                for (AprobacionResponse aprobacion : requerimiento.aprobaciones()) {
                    writer.writeBulletLine("%s por %s".formatted(aprobacion.accion(), aprobacion.usuario()));
                    writer.writeSubLine("Fecha: %s".formatted(formatDateTime(aprobacion.decisionAt())));
                    writer.writeSubLine("Comentario: " + normalizeBlank(aprobacion.comentario()));
                }
            }

            writer.writeSectionTitle("Historial de estados");
            for (RequerimientoEstadoHistorialResponse historial : requerimiento.historialEstados()) {
                writer.writeBulletLine("%s -> %s".formatted(
                        historial.estadoAnterior() == null ? "INICIAL" : historial.estadoAnterior(),
                        historial.estadoNuevo()));
                writer.writeSubLine("Usuario: %s | Fecha: %s".formatted(
                        historial.usuario(),
                        formatDateTime(historial.fechaHora())));
            }

            writer.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de aprobacion.", ex);
        }
    }

    public byte[] buildOrdenCompraPdf(OrdenCompraResponse ordenCompra) {
        return buildOrdenCompraPdf(ordenCompra, null);
    }

    public byte[] buildOrdenCompraPdf(OrdenCompraResponse ordenCompra, PdfHeaderData headerData) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DocumentWriter writer = new DocumentWriter(document);
            writer.writeHeader(
                    "ORDEN DE COMPRA",
                    ordenCompra.numero(),
                    headerData,
                    List.of(
                            "Empresa: " + reportProperties.companyName(),
                            "Compra generada por: " + ordenCompra.createdBy(),
                            "Requerimiento origen: " + ordenCompra.requerimientoNumero(),
                            "Fecha de emision: " + formatDateTime(ordenCompra.generatedAt())));

            writer.writeSectionTitle("Resumen");
            writer.writeLabelValue("Proveedor", ordenCompra.proveedor().code() + " - " + ordenCompra.proveedor().name());
            writer.writeLabelValue("Moneda", ordenCompra.moneda().name());
            writer.writeLabelValue("Tipo de cambio", formatAmount(ordenCompra.tipoCambio()));
            writer.writeLabelValue("Subtotal", formatAmount(ordenCompra.subtotal()));
            writer.writeLabelValue("IGV", formatAmount(ordenCompra.igv()));
            writer.writeLabelValue("Total", formatAmount(ordenCompra.total()));

            writer.writeSectionTitle("Detalle de compra");
            for (OrdenCompraDetalleResponse detalle : ordenCompra.detalles()) {
                writer.writeBulletLine("%s - %s".formatted(detalle.itemCode(), detalle.itemName()));
                writer.writeSubLine("Almacen: %s | Cantidad: %s | Precio: %s | Subtotal: %s".formatted(
                        detalle.almacenCode(),
                        detalle.cantidad(),
                        formatAmount(detalle.precioUnitario()),
                        formatAmount(detalle.subtotalLinea())));
            }

            writer.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden de compra.", ex);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatAmount(BigDecimal value) {
        return value == null ? "-" : String.format(Locale.US, "%.2f", value);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static final class DocumentWriter {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private DocumentWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void writeHeader(String title, String documentNumber, PdfHeaderData headerData, List<String> fallbackLines) throws IOException {
            writeText(title, FONT_BOLD, 18f, PAGE_MARGIN, y);
            y -= 22f;
            writeText("Documento: " + documentNumber, FONT_BOLD, 12f, PAGE_MARGIN, y);
            y -= 18f;
            if (headerData != null && !headerData.isEmpty()) {
                writeHeaderData(headerData);
            } else {
                for (String headerLine : fallbackLines) {
                    writeWrappedLine(headerLine, FONT_REGULAR, 10f, PAGE_MARGIN, getContentWidth());
                }
            }
            y -= 10f;
            drawDivider();
            y -= 10f;
        }

        private void writeHeaderData(PdfHeaderData headerData) throws IOException {
            writeLabelValue("Entidad", headerData.entidad());
            writeLabelValue("Area solicitante", headerData.areaSolicitante());
            writeLabelValue("Oficina que aprueba", headerData.oficinaQueAprueba());
            writeLabelValue("Asunto", headerData.asunto());
            writeLabelValue("Referencia", headerData.referencia());
            writeLabelValue("Fecha del documento", headerData.fechaDocumento());
            writeLabelValue("Destinatario", headerData.destinatario());
            writeLabelValue("Cargo del destinatario", headerData.cargoDestinatario());
            writeLabelValue("Observaciones", headerData.observaciones());
            writeLabelValue("Pie de firma", headerData.pieFirma());
        }

        private void writeSectionTitle(String title) throws IOException {
            ensureSpace(24f);
            writeText(title, FONT_BOLD, 13f, PAGE_MARGIN, y);
            y -= 18f;
        }

        private void writeLabelValue(String label, String value) throws IOException {
            writeWrappedLine("%s: %s".formatted(label, value == null || value.isBlank() ? "-" : value),
                    FONT_REGULAR, 10.5f, PAGE_MARGIN, getContentWidth());
        }

        private void writeBulletLine(String value) throws IOException {
            writeWrappedLine("- " + value, FONT_BOLD, 10.5f, PAGE_MARGIN, getContentWidth());
        }

        private void writeSubLine(String value) throws IOException {
            writeWrappedLine(value, FONT_REGULAR, 10f, PAGE_MARGIN + 12f, getContentWidth() - 12f);
        }

        private void writeWrappedLine(String text, PDType1Font font, float fontSize, float x, float maxWidth) throws IOException {
            for (String line : wrapText(text, font, fontSize, maxWidth)) {
                ensureSpace(LINE_HEIGHT);
                writeText(line, font, fontSize, x, y);
                y -= LINE_HEIGHT;
            }
        }

        private void drawDivider() throws IOException {
            ensureSpace(4f);
            contentStream.setLineWidth(1f);
            contentStream.moveTo(PAGE_MARGIN, y);
            contentStream.lineTo(page.getMediaBox().getWidth() - PAGE_MARGIN, y);
            contentStream.stroke();
        }

        private void writeText(String text, PDType1Font font, float fontSize, float x, float textY) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, textY);
            contentStream.showText(text);
            contentStream.endText();
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight >= PAGE_BOTTOM_MARGIN) {
                return;
            }
            newPage();
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void finish() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private float getContentWidth() {
            return page.getMediaBox().getWidth() - (PAGE_MARGIN * 2);
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String normalized = text == null || text.isBlank() ? "-" : text.trim();
            String[] words = normalized.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width <= maxWidth || currentLine.isEmpty()) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                    continue;
                }
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
            return lines;
        }
    }
}

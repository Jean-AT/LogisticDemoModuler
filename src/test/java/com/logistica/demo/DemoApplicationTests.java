package com.logistica.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DemoApplicationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldExposeVersionedApiAndProblemDetails() throws Exception {
        HttpResponse<String> login = send(
                "POST",
                "/api/v1/auth/login",
                "{\"username\":\"solicitante\",\"password\":\"demo123\"}",
                null,
                null);
        assertEquals(200, login.statusCode());

        HttpResponse<String> invalidRequest = send(
                "GET",
                "/api/v1/requerimientos?estado=ESTADO_INVALIDO",
                null,
                "solicitante",
                "demo123");
        JsonNode problem = objectMapper.readTree(invalidRequest.body());

        assertEquals(400, invalidRequest.statusCode());
        assertTrue(invalidRequest.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
        assertEquals("INVALID_PARAMETER", problem.get("code").asText());
        assertEquals("Parametro 'estado' invalido.", problem.get("detail").asText());
        assertEquals("/api/v1/requerimientos", problem.get("instance").asText());
        assertTrue(problem.hasNonNull("traceId"));
        assertTrue(problem.hasNonNull("timestamp"));
    }

    @Test
    void shouldExposeHealthProbesWithoutAuthentication() throws Exception {
        HttpResponse<String> response = send("GET", "/actuator/health", null, null, null);
        JsonNode health = objectMapper.readTree(response.body());
        HttpResponse<String> liveness = send("GET", "/actuator/health/liveness", null, null, null);
        HttpResponse<String> readiness = send("GET", "/actuator/health/readiness", null, null, null);

        assertEquals(200, response.statusCode());
        assertEquals("UP", health.get("status").asText());
        assertEquals(200, liveness.statusCode());
        assertEquals(200, readiness.statusCode());
        assertTrue(response.headers().firstValue("X-Trace-Id").isPresent());
    }

    @Test
    void shouldPropagateTraceIdToResponseAndProblemDetail() throws Exception {
        String traceId = "mvp1-arc006-test";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/requerimientos?estado=INVALIDO"))
                .header("Authorization", basicAuth("solicitante", "demo123"))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("X-Trace-Id", traceId)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode problem = objectMapper.readTree(response.body());

        assertEquals(400, response.statusCode());
        assertEquals(traceId, response.headers().firstValue("X-Trace-Id").orElseThrow());
        assertEquals(traceId, problem.get("traceId").asText());
    }

    @Test
    void shouldCreateDraftRequisition() throws Exception {
        String requestBody = buildRequisitionRequest("PEN", 2, "150.50");
        HttpResponse<String> response = send("POST", "/api/requerimientos", requestBody, "solicitante", "demo123");
        JsonNode json = objectMapper.readTree(response.body());

        assertEquals(201, response.statusCode());
        assertTrue(json.get("numero").asText().startsWith("REQ-"));
        assertEquals("BORRADOR", json.get("estado").asText());
        assertEquals("solicitante", json.get("createdBy").asText());
        assertDecimalEquals("301.00", json.get("detalles").get(0).get("subtotalLinea"));
        assertEquals("BORRADOR", json.get("historialEstados").get(0).get("estadoNuevo").asText());
        assertEquals("solicitante", json.get("historialEstados").get(0).get("usuario").asText());
    }

    @Test
    void shouldCompleteApprovalToPurchaseOrderFlow() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("USD", 2, "100.01"),
                "solicitante",
                "demo123");
        assertEquals(201, createResult.statusCode());

        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        HttpResponse<String> enviar = send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123");
        assertEquals(200, enviar.statusCode());
        assertEquals("ENVIADO", objectMapper.readTree(enviar.body()).get("estado").asText());

        HttpResponse<String> aprobar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/aprobar",
                "{\"comentario\":\"Aprobado para demo\"}",
                "aprobador",
                "demo123");
        JsonNode aprobacion = objectMapper.readTree(aprobar.body());
        assertEquals(200, aprobar.statusCode());
        assertEquals("APROBADO", aprobacion.get("estado").asText());
        assertEquals("APROBAR", aprobacion.get("aprobaciones").get(0).get("accion").asText());
        assertEquals("aprobador", aprobacion.get("aprobaciones").get(0).get("usuario").asText());

        HttpResponse<String> generarOc = send(
                "POST",
                "/api/ordenes-compra/desde-requerimiento/" + requerimientoId,
                null,
                "compras",
                "demo123");
        JsonNode orden = objectMapper.readTree(generarOc.body());
        assertEquals(201, generarOc.statusCode());
        assertTrue(orden.get("numero").asText().startsWith("OC-"));
        assertEquals("USD", orden.get("moneda").asText());
        assertDecimalEquals("3.35", orden.get("tipoCambio"));
        assertDecimalEquals("200.02", orden.get("subtotal"));
        assertDecimalEquals("36.00", orden.get("igv"));
        assertDecimalEquals("236.02", orden.get("total"));

        HttpResponse<String> getRequerimiento = send("GET", "/api/requerimientos/" + requerimientoId, null, "solicitante", "demo123");
        JsonNode requerimientoFinal = objectMapper.readTree(getRequerimiento.body());
        assertEquals(200, getRequerimiento.statusCode());
        assertEquals("CONVERTIDO_OC", requerimientoFinal.get("estado").asText());
        assertTrue(requerimientoFinal.get("ordenCompra").get("numero").asText().startsWith("OC-"));
        assertEquals(4, requerimientoFinal.get("historialEstados").size());
    }

    @Test
    void shouldRejectPurchaseOrderGenerationForNonApprovedRequisition() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "89.90"),
                "solicitante",
                "demo123");
        assertEquals(201, createResult.statusCode());

        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        HttpResponse<String> generarOc = send(
                "POST",
                "/api/ordenes-compra/desde-requerimiento/" + requerimientoId,
                null,
                "compras",
                "demo123");
        assertEquals(409, generarOc.statusCode());
        assertEquals(
                "Solo se puede generar una orden desde un requerimiento APROBADO.",
                objectMapper.readTree(generarOc.body()).get("message").asText());
    }

    @Test
    void shouldForbidPurchaseOrderGenerationForNonPurchasingRole() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "50.00"),
                "solicitante",
                "demo123");
        assertEquals(201, createResult.statusCode());

        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        HttpResponse<String> generarOc = send(
                "POST",
                "/api/ordenes-compra/desde-requerimiento/" + requerimientoId,
                null,
                "solicitante",
                "demo123");
        assertEquals(403, generarOc.statusCode());
    }

    @Test
    void shouldListRequisitionsUsingIsoDateTimeFilters() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "50.00"),
                "solicitante",
                "demo123");
        assertEquals(201, createResult.statusCode());

        LocalDate today = LocalDate.now();
        String path = "/api/requerimientos?numero=REQ&fechaDesde=%s&fechaHasta=%s".formatted(
                today.atStartOfDay(),
                LocalDateTime.of(today.plusDays(1), java.time.LocalTime.MAX));

        HttpResponse<String> response = send("GET", path, null, "solicitante", "demo123");
        JsonNode body = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertTrue(body.get("content").isArray());
        assertTrue(body.get("content").size() >= 1);
    }

    @Test
    void shouldRestrictSolicitanteToOwnRequisitionsAndAllowAdminToSeeAll() throws Exception {
        HttpResponse<String> solicitanteCreate = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "60.00"),
                "solicitante",
                "demo123");
        assertEquals(201, solicitanteCreate.statusCode());
        String solicitanteNumero = objectMapper.readTree(solicitanteCreate.body()).get("numero").asText();

        HttpResponse<String> adminCreate = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("USD", 1, "40.00"),
                "admin",
                "demo123");
        assertEquals(201, adminCreate.statusCode());
        String adminNumero = objectMapper.readTree(adminCreate.body()).get("numero").asText();

        HttpResponse<String> solicitanteList = send(
                "GET",
                "/api/requerimientos?numero=" + solicitanteNumero,
                null,
                "solicitante",
                "demo123");
        JsonNode solicitanteBody = objectMapper.readTree(solicitanteList.body());
        assertEquals(200, solicitanteList.statusCode());
        assertEquals(1, solicitanteBody.get("content").size());
        assertEquals("solicitante", solicitanteBody.get("content").get(0).get("createdBy").asText());

        HttpResponse<String> hiddenFromSolicitante = send(
                "GET",
                "/api/requerimientos?numero=" + adminNumero,
                null,
                "solicitante",
                "demo123");
        assertEquals(200, hiddenFromSolicitante.statusCode());
        assertEquals(0, objectMapper.readTree(hiddenFromSolicitante.body()).get("content").size());

        HttpResponse<String> adminList = send(
                "GET",
                "/api/requerimientos?numero=" + adminNumero,
                null,
                "admin",
                "demo123");
        JsonNode adminBody = objectMapper.readTree(adminList.body());
        assertEquals(200, adminList.statusCode());
        assertEquals(1, adminBody.get("content").size());

        HttpResponse<String> aprobadorList = send(
                "GET",
                "/api/requerimientos?numero=" + adminNumero,
                null,
                "aprobador",
                "demo123");
        JsonNode aprobadorBody = objectMapper.readTree(aprobadorList.body());
        assertEquals(200, aprobadorList.statusCode());
        assertEquals(1, aprobadorBody.get("content").size());
    }

    @Test
    void shouldAllowApprovingObservedRequisitionAndKeepHistory() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "85.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());

        HttpResponse<String> observar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/observar",
                "{\"comentario\":\"Falta sustento\"}",
                "aprobador",
                "demo123");
        assertEquals(200, observar.statusCode());
        assertEquals("OBSERVADO", objectMapper.readTree(observar.body()).get("estado").asText());

        HttpResponse<String> aprobar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/aprobar",
                "{\"comentario\":\"Sustento recibido\"}",
                "aprobador",
                "demo123");
        JsonNode body = objectMapper.readTree(aprobar.body());
        assertEquals(200, aprobar.statusCode());
        assertEquals("APROBADO", body.get("estado").asText());
        assertEquals(4, body.get("historialEstados").size());
        assertEquals("OBSERVADO", body.get("historialEstados").get(3).get("estadoAnterior").asText());
        assertEquals("APROBADO", body.get("historialEstados").get(3).get("estadoNuevo").asText());
    }

    @Test
    void shouldRejectInvalidStateTransitionAfterApproval() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "85.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());
        assertEquals(
                200,
                send(
                        "POST",
                        "/api/aprobaciones/" + requerimientoId + "/aprobar",
                        "{\"comentario\":\"OK\"}",
                        "aprobador",
                        "demo123").statusCode());

        HttpResponse<String> rechazar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/rechazar",
                "{\"comentario\":\"Ya no aplica\"}",
                "aprobador",
                "demo123");
        assertEquals(409, rechazar.statusCode());
        assertEquals(
                "Solo se puede decidir un requerimiento en estado ENVIADO u OBSERVADO.",
                objectMapper.readTree(rechazar.body()).get("message").asText());
    }

    @Test
    void shouldCreateMasterDataForAuthorizedRolesOnly() throws Exception {
        HttpResponse<String> createProveedor = send(
                "POST",
                "/api/proveedores",
                "{\"code\":\"PRV-900\",\"name\":\"Proveedor Test\"}",
                "solicitante",
                "demo123");
        assertEquals(201, createProveedor.statusCode());

        HttpResponse<String> forbiddenCreate = send(
                "POST",
                "/api/items",
                "{\"code\":\"ITM-900\",\"name\":\"Item Test\",\"unitMeasure\":\"UND\"}",
                "aprobador",
                "demo123");
        assertEquals(403, forbiddenCreate.statusCode());
    }

    @Test
    void shouldListApprovalsWithOptionalFiltersForAuthorizedRoles() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "95.00"),
                "solicitante",
                "demo123");
        JsonNode created = objectMapper.readTree(createResult.body());
        long requerimientoId = created.get("id").asLong();
        String numero = created.get("numero").asText();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());

        LocalDate today = LocalDate.now();
        String path = "/api/aprobaciones?id=%d&numero=%s&estado=ENVIADO&fechaDesde=%s&fechaHasta=%s".formatted(
                requerimientoId,
                numero,
                today,
                today);

        HttpResponse<String> response = send("GET", path, null, "aprobador", "demo123");
        JsonNode body = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertTrue(body.get("content").isArray());
        assertEquals(1, body.get("content").size());
        assertEquals(requerimientoId, body.get("content").get(0).get("id").asLong());
        assertEquals("ENVIADO", body.get("content").get(0).get("estado").asText());
    }

    @Test
    void shouldForbidApprovalsListingForSolicitante() throws Exception {
        HttpResponse<String> response = send("GET", "/api/aprobaciones", null, "solicitante", "demo123");
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldGeneratePdfReportsWithSharedHeader() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("USD", 2, "110.00"),
                "solicitante",
                "demo123");
        JsonNode created = objectMapper.readTree(createResult.body());
        long requerimientoId = created.get("id").asLong();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());
        assertEquals(
                200,
                send(
                        "POST",
                        "/api/aprobaciones/" + requerimientoId + "/aprobar",
                        "{\"comentario\":\"Listo para compra\"}",
                        "aprobador",
                        "demo123").statusCode());

        HttpResponse<String> ordenCreate = send(
                "POST",
                "/api/ordenes-compra/desde-requerimiento/" + requerimientoId,
                null,
                "compras",
                "demo123");
        long ordenId = objectMapper.readTree(ordenCreate.body()).get("id").asLong();

        HttpResponse<byte[]> requerimientoPdf = sendBytes(
                "GET",
                "/api/requerimientos/" + requerimientoId + "/pdf",
                null,
                "solicitante",
                "demo123");
        assertEquals(200, requerimientoPdf.statusCode());
        assertEquals("application/pdf", requerimientoPdf.headers().firstValue("Content-Type").orElse(""));
        String requerimientoText = extractPdfText(requerimientoPdf.body());
        assertTrue(requerimientoText.contains("REPORTE DE REQUERIMIENTO"));
        assertTrue(requerimientoText.contains("Empresa: Logistica Demo SAC"));
        assertTrue(requerimientoText.contains("Solicitado por: solicitante"));

        HttpResponse<byte[]> aprobacionPdf = sendBytes(
                "GET",
                "/api/aprobaciones/" + requerimientoId + "/pdf",
                null,
                "aprobador",
                "demo123");
        assertEquals(200, aprobacionPdf.statusCode());
        String aprobacionText = extractPdfText(aprobacionPdf.body());
        assertTrue(aprobacionText.contains("REPORTE DE APROBACION"));
        assertTrue(aprobacionText.contains("Gestionado por: aprobador"));

        HttpResponse<byte[]> ordenPdf = sendBytes(
                "GET",
                "/api/ordenes-compra/" + ordenId + "/pdf",
                null,
                "compras",
                "demo123");
        assertEquals(200, ordenPdf.statusCode());
        String ordenText = extractPdfText(ordenPdf.body());
        assertTrue(ordenText.contains("ORDEN DE COMPRA"));
        assertTrue(ordenText.contains("Compra generada por: compras"));
        assertTrue(ordenText.contains("Empresa: Logistica Demo SAC"));
    }

    @Test
    void shouldReturnBadRequestForInvalidDateFilterFormat() throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/api/requerimientos?fechaDesde=no-es-fecha",
                null,
                "solicitante",
                "demo123");
        JsonNode body = objectMapper.readTree(response.body());

        assertEquals(400, response.statusCode());
        assertEquals(
                "Parametro 'fechaDesde' invalido. Use formato yyyy-MM-dd o yyyy-MM-ddTHH:mm:ss.",
                body.get("message").asText());
    }

    @Test
    void shouldGeneratePdfWithConfigurableHeaderData() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "55.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        HttpResponse<byte[]> pdf = sendBytes(
                "GET",
                "/api/requerimientos/" + requerimientoId
                        + "/pdf?entidad=Mi%20Entidad&asunto=Compra%20de%20bienes&oficinaQueAprueba=Logistica",
                null,
                "solicitante",
                "demo123");
        assertEquals(200, pdf.statusCode());
        String text = extractPdfText(pdf.body());
        assertTrue(text.contains("Entidad: Mi Entidad"));
        assertTrue(text.contains("Asunto: Compra de bienes"));
        assertTrue(text.contains("Oficina que aprueba: Logistica"));
    }

    private String buildRequisitionRequest(String moneda, int cantidad, String precioUnitario) throws Exception {
        long proveedorId = firstId("/api/proveedores");
        long itemId = firstId("/api/items");
        long almacenId = firstId("/api/almacenes");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("descripcion", "Compra de mobiliario demo");
        request.put("proveedorId", proveedorId);
        request.put("moneda", moneda);

        ObjectNode detalle = objectMapper.createObjectNode();
        detalle.put("itemId", itemId);
        detalle.put("almacenId", almacenId);
        detalle.put("cantidad", cantidad);
        detalle.put("precioUnitarioEstimado", precioUnitario);

        request.putArray("detalles").add(detalle);
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void shouldLoginWithJwtAndFetchCurrentUser() throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login", "{\"username\":\"solicitante\",\"password\":\"demo123\"}", null, null);
        assertEquals(200, login.statusCode());
        JsonNode loginBody = objectMapper.readTree(login.body());
        String token = loginBody.get("token").asText();
        assertNotNull(token);
        assertEquals("Bearer", loginBody.get("tokenType").asText());
        assertEquals("solicitante", loginBody.get("user").get("username").asText());
        assertEquals("SOLICITANTE", loginBody.get("user").get("role").asText());

        HttpResponse<String> me = sendBearer("GET", "/api/auth/me", null, token);
        assertEquals(200, me.statusCode());
        JsonNode meBody = objectMapper.readTree(me.body());
        assertEquals("solicitante", meBody.get("username").asText());
        assertEquals("Solicitante Demo", meBody.get("fullName").asText());

        HttpResponse<String> badLogin = send("POST", "/api/auth/login", "{\"username\":\"solicitante\",\"password\":\"incorrecto\"}", null, null);
        assertEquals(401, badLogin.statusCode());
    }

    @Test
    void shouldEditDraftRequisitionViaPut() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "70.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        String updated = buildRequisitionRequest("PEN", 3, "25.00");
        HttpResponse<String> update = send("PUT", "/api/requerimientos/" + requerimientoId, updated, "solicitante", "demo123");
        assertEquals(200, update.statusCode());
        JsonNode updatedBody = objectMapper.readTree(update.body());
        assertEquals("BORRADOR", updatedBody.get("estado").asText());
        assertDecimalEquals("75.00", updatedBody.get("detalles").get(0).get("subtotalLinea"));
    }

    @Test
    void shouldCorrectAndResendObservedRequisition() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "90.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());
        assertEquals(
                200,
                send(
                        "POST",
                        "/api/aprobaciones/" + requerimientoId + "/observar",
                        "{\"comentario\":\"Ajustar cantidades\"}",
                        "aprobador",
                        "demo123").statusCode());

        HttpResponse<String> update = send(
                "PUT",
                "/api/requerimientos/" + requerimientoId,
                buildRequisitionRequest("PEN", 2, "45.00"),
                "solicitante",
                "demo123");
        assertEquals(200, update.statusCode());
        assertEquals("BORRADOR", objectMapper.readTree(update.body()).get("estado").asText());

        HttpResponse<String> resend = send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123");
        assertEquals(200, resend.statusCode());
        assertEquals("ENVIADO", objectMapper.readTree(resend.body()).get("estado").asText());
    }

    @Test
    void shouldProvideDashboardMetrics() throws Exception {
        HttpResponse<String> response = send("GET", "/api/dashboard", null, "admin", "demo123");
        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.has("totalRequerimientos"));
        assertTrue(body.has("montoEstimadoPendiente"));
        assertTrue(body.has("miActividad"));
        assertTrue(body.get("miActividad").has("total"));
    }

    private long firstId(String path) throws Exception {
        HttpResponse<String> response = send("GET", path, null, "solicitante", "demo123");
        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body);
        return body.get(0).get("id").asLong();
    }

    private HttpResponse<String> send(String method, String path, String body, String username, String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (username != null) {
            builder.header("Authorization", basicAuth(username, password));
        }

        if (body != null) {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendBearer(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (body != null) {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendBytes(String method, String path, String body, String username, String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", basicAuth(username, password))
                .header("Accept", MediaType.APPLICATION_PDF_VALUE);

        if (body != null) {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private void assertDecimalEquals(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.asText())));
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

}

package com.logistica.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferCommand;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferPort;
import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.cuadronecesidades.domain.TipoVentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.VentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.VentanaCuadroNecesidadRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:logistica-demo-api;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=MONTH")
@ActiveProfiles("test")
class DemoApplicationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> accessTokens = new ConcurrentHashMap<>();

    @LocalServerPort
    private int port;

    @Autowired
    private VentanaCuadroNecesidadRepository needsWindows;

    @Autowired
    private RecordingNeedsBudgetTransferPort needsBudgetTransferPort;

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
                .header("Authorization", "Bearer " + accessToken("solicitante", "demo123"))
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
    void shouldRejectLegacyApprovalWithoutBudgetTraceability() throws Exception {
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
        assertEquals(409, aprobar.statusCode());
        assertEquals(
                "El requerimiento no tiene trazabilidad presupuestal. Cree el requerimiento desde Cuadro o vincule presupuesto antes de aprobar.",
                aprobacion.get("message").asText());

        HttpResponse<String> getRequerimiento = send("GET", "/api/requerimientos/" + requerimientoId, null, "solicitante", "demo123");
        JsonNode requerimientoFinal = objectMapper.readTree(getRequerimiento.body());
        assertEquals(200, getRequerimiento.statusCode());
        assertEquals("ENVIADO", requerimientoFinal.get("estado").asText());
        assertTrue(!requerimientoFinal.has("ordenCompra") || requerimientoFinal.get("ordenCompra").isNull());
        assertEquals(2, requerimientoFinal.get("historialEstados").size());
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
    void shouldAllowObservingAndCorrectingRequisitionWithoutBudgetImpact() throws Exception {
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

        HttpResponse<String> update = send(
                "PUT",
                "/api/requerimientos/" + requerimientoId,
                buildRequisitionRequest("PEN", 2, "42.50"),
                "solicitante",
                "demo123");
        JsonNode body = objectMapper.readTree(update.body());
        assertEquals(200, update.statusCode());
        assertEquals("BORRADOR", body.get("estado").asText());
        assertEquals(4, body.get("historialEstados").size());
        assertEquals("OBSERVADO", body.get("historialEstados").get(3).get("estadoAnterior").asText());
        assertEquals("BORRADOR", body.get("historialEstados").get(3).get("estadoNuevo").asText());
    }

    @Test
    void shouldRejectLegacyApprovalAndStillAllowRejectionFromSubmittedState() throws Exception {
        HttpResponse<String> createResult = send(
                "POST",
                "/api/requerimientos",
                buildRequisitionRequest("PEN", 1, "85.00"),
                "solicitante",
                "demo123");
        long requerimientoId = objectMapper.readTree(createResult.body()).get("id").asLong();

        assertEquals(200, send("POST", "/api/requerimientos/" + requerimientoId + "/enviar", null, "solicitante", "demo123").statusCode());
        HttpResponse<String> aprobar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/aprobar",
                "{\"comentario\":\"OK\"}",
                "aprobador",
                "demo123");
        assertEquals(409, aprobar.statusCode());

        HttpResponse<String> rechazar = send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/rechazar",
                "{\"comentario\":\"Ya no aplica\"}",
                "aprobador",
                "demo123");
        assertEquals(200, rechazar.statusCode());
        assertEquals("RECHAZADO", objectMapper.readTree(rechazar.body()).get("estado").asText());
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
        assertEquals(200, send(
                "POST",
                "/api/aprobaciones/" + requerimientoId + "/observar",
                "{\"comentario\":\"Revisar sustento\"}",
                "aprobador",
                "demo123").statusCode());

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
        assertTrue(aprobacionText.contains("OBSERVAR"));
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
        String token = loginBody.get("accessToken").asText();
        assertNotNull(token);
        assertTrue(loginBody.get("refreshToken").asText().length() >= 32);
        assertTrue(loginBody.get("expiresIn").asLong() > 0);
        assertEquals("Bearer", loginBody.get("tokenType").asText());
        assertEquals("solicitante", loginBody.get("user").get("username").asText());
        assertTrue(loginBody.get("user").get("roles").toString().contains("SOLICITANTE"));
        assertTrue(loginBody.get("user").get("permissions").isArray());
        assertTrue(loginBody.get("user").get("scopes").isArray());

        HttpResponse<String> me = sendBearer("GET", "/api/auth/me", null, token);
        assertEquals(200, me.statusCode());
        JsonNode meBody = objectMapper.readTree(me.body());
        assertEquals("solicitante", meBody.get("username").asText());
        assertEquals("Solicitante Demo", meBody.get("fullName").asText());

        HttpResponse<String> badLogin = send("POST", "/api/auth/login", "{\"username\":\"solicitante\",\"password\":\"incorrecto\"}", null, null);
        assertEquals(401, badLogin.statusCode());
    }

    @Test
    void shouldRotateRefreshTokenAndRejectItsReuse() throws Exception {
        JsonNode login = login("solicitante", "demo123");
        String firstRefreshToken = login.get("refreshToken").asText();

        HttpResponse<String> refresh = send(
                "POST", "/api/v1/auth/refresh",
                "{\"refreshToken\":\"" + firstRefreshToken + "\"}", null, null);
        assertEquals(200, refresh.statusCode());
        JsonNode refreshed = objectMapper.readTree(refresh.body());
        assertNotNull(refreshed.get("accessToken").asText());
        assertTrue(!firstRefreshToken.equals(refreshed.get("refreshToken").asText()));
        assertEquals(200, sendBearer("GET", "/api/v1/auth/me", null, refreshed.get("accessToken").asText()).statusCode());

        HttpResponse<String> reused = send(
                "POST", "/api/v1/auth/refresh",
                "{\"refreshToken\":\"" + firstRefreshToken + "\"}", null, null);
        assertEquals(401, reused.statusCode());
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        String refreshToken = login("solicitante", "demo123").get("refreshToken").asText();
        String request = "{\"refreshToken\":\"" + refreshToken + "\"}";

        assertEquals(204, send("POST", "/api/v1/auth/logout", request, null, null).statusCode());
        assertEquals(401, send("POST", "/api/v1/auth/refresh", request, null, null).statusCode());
    }

    @Test
    void shouldRejectHttpBasicAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/me"))
                .header("Authorization", basicAuth("solicitante", "demo123"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
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

    @Test
    void shouldExposePlatformAdministrationApi() throws Exception {
        HttpResponse<String> items = send(
                "GET",
                "/api/v1/platform/catalog/items?companyId=1",
                null,
                "solicitante",
                "demo123");
        assertEquals(200, items.statusCode());
        JsonNode itemsBody = objectMapper.readTree(items.body());
        assertEquals(10, itemsBody.size());
        assertEquals("ITM-001", itemsBody.get(0).get("code").asText());
        assertTrue(itemsBody.get(0).has("unitOfMeasure"));
        assertTrue(itemsBody.get(0).has("expenseClassifier"));

        HttpResponse<String> service = send(
                "GET",
                "/api/v1/platform/catalog/items/serv-002?companyId=1",
                null,
                "solicitante",
                "demo123");
        assertEquals(200, service.statusCode());
        JsonNode serviceBody = objectMapper.readTree(service.body());
        assertEquals("SERVICE", serviceBody.get("itemType").asText());
        assertEquals("GLB", serviceBody.get("unitOfMeasure").get("code").asText());

        HttpResponse<String> forbiddenWrite = send(
                "PUT",
                "/api/v1/platform/fiscal-periods",
                "{\"companyId\":1,\"fiscalYear\":2026,\"month\":10,"
                        + "\"startsOn\":\"2026-10-01\",\"endsOn\":\"2026-10-31\",\"status\":\"OPEN\"}",
                "solicitante",
                "demo123");
        assertEquals(403, forbiddenWrite.statusCode());

        HttpResponse<String> definedPeriod = send(
                "PUT",
                "/api/v1/platform/fiscal-periods",
                "{\"companyId\":1,\"fiscalYear\":2026,\"month\":10,"
                        + "\"startsOn\":\"2026-10-01\",\"endsOn\":\"2026-10-31\",\"status\":\"OPEN\"}",
                "admin",
                "demo123");
        assertEquals(200, definedPeriod.statusCode());
        assertEquals("OPEN", objectMapper.readTree(definedPeriod.body()).get("status").asText());

        HttpResponse<String> closePeriod = send(
                "POST",
                "/api/v1/platform/fiscal-periods/1/2026/10/close",
                null,
                "admin",
                "demo123");
        assertEquals(200, closePeriod.statusCode());
        assertEquals("CLOSED", objectMapper.readTree(closePeriod.body()).get("status").asText());

        HttpResponse<String> configureSequence = send(
                "PUT",
                "/api/v1/platform/document-sequences",
                "{\"companyId\":1,\"fiscalYear\":2026,\"documentType\":\"REQ\",\"prefix\":\"REQ-2026\",\"currentValue\":99}",
                "admin",
                "demo123");
        assertEquals(204, configureSequence.statusCode());

        HttpResponse<String> nextNumber = send(
                "POST",
                "/api/v1/platform/document-sequences/next",
                "{\"companyId\":1,\"fiscalYear\":2026,\"documentType\":\"REQ\"}",
                "admin",
                "demo123");
        assertEquals(200, nextNumber.statusCode());
        assertEquals("REQ-2026-000100", objectMapper.readTree(nextNumber.body()).get("formatted").asText());

        HttpResponse<String> access = send(
                "GET",
                "/api/v1/platform/security/users/solicitante/access",
                null,
                "admin",
                "demo123");
        assertEquals(200, access.statusCode());
        JsonNode accessBody = objectMapper.readTree(access.body());
        assertEquals("solicitante", accessBody.get("username").asText());
        assertTrue(accessBody.get("grants").toString().contains("PLATFORM.MASTER.READ"));
    }

    @Test
    void shouldExposeNeedsScreensApi() throws Exception {
        String request = buildNeedsPlanRequest();
        HttpResponse<String> create = send(
                "POST",
                "/api/v1/needs/plans",
                request,
                "solicitante",
                "demo123");
        JsonNode created = objectMapper.readTree(create.body());

        assertEquals(201, create.statusCode());
        assertEquals("DRAFT", created.get("status").asText());
        assertEquals(12, created.get("details").get(0).get("months").size());

        long planId = created.get("id").asLong();
        HttpResponse<String> list = send(
                "GET",
                "/api/v1/needs/plans?companyId=1&fiscalYear=2026&status=DRAFT",
                null,
                "solicitante",
                "demo123");
        JsonNode listBody = objectMapper.readTree(list.body());
        assertEquals(200, list.statusCode());
        assertTrue(listBody.get("content").toString().contains("Plan anual API"));

        HttpResponse<String> detail = send(
                "GET",
                "/api/v1/needs/plans/" + planId,
                null,
                "aprobador",
                "demo123");
        assertEquals(200, detail.statusCode());
        assertEquals(planId, objectMapper.readTree(detail.body()).get("id").asLong());

        HttpResponse<String> traceability = send(
                "GET",
                "/api/v1/needs/traceability/plans/" + planId,
                null,
                "solicitante",
                "demo123");
        JsonNode traceabilityBody = objectMapper.readTree(traceability.body());
        assertEquals(200, traceability.statusCode());
        assertEquals(planId, traceabilityBody.get("plan").get("id").asLong());
        assertTrue(traceabilityBody.get("consolidations").isArray());
    }

    @Test
    void shouldCompleteAnnualNeedsFlowThroughApiWithIdempotentTransferAndBalances() throws Exception {
        needsBudgetTransferPort.reset();
        ensureOpenNeedsWindows(1L, 2026);

        HttpResponse<String> create = send(
                "POST",
                "/api/v1/needs/plans",
                buildNeedsPlanRequest(1, "Plan anual API flujo"),
                "solicitante",
                "demo123");
        JsonNode created = objectMapper.readTree(create.body());

        assertEquals(201, create.statusCode());
        long planId = created.get("id").asLong();
        long needsLineId = created.get("details").get(0).get("id").asLong();

        HttpResponse<String> submit = send(
                "POST",
                "/api/v1/needs/plans/" + planId + "/submit",
                null,
                "solicitante",
                "demo123");
        assertEquals(200, submit.statusCode());
        assertEquals("SUBMITTED", objectMapper.readTree(submit.body()).get("status").asText());

        HttpResponse<String> review = send(
                "POST",
                "/api/v1/needs/plans/" + planId + "/review",
                buildNeedsReviewRequest(),
                "aprobador",
                "demo123");
        assertEquals(200, review.statusCode());
        assertEquals("REVIEWED", objectMapper.readTree(review.body()).get("status").asText());

        HttpResponse<String> consolidate = send(
                "POST",
                "/api/v1/needs/consolidations?companyId=1&fiscalYear=2026",
                null,
                "aprobador",
                "demo123");
        JsonNode consolidated = objectMapper.readTree(consolidate.body());

        assertEquals(201, consolidate.statusCode());
        assertEquals("CONSOLIDATED", consolidated.get("status").asText());
        assertEquals(1, consolidated.get("sources").size());

        long consolidationId = consolidated.get("id").asLong();
        String idempotencyKey = "needs-transfer-cn-t07-" + consolidationId;

        HttpResponse<String> firstTransfer = sendWithHeader(
                "POST",
                "/api/v1/needs/consolidations/" + consolidationId + "/transfer",
                null,
                "aprobador",
                "demo123",
                "Idempotency-Key",
                idempotencyKey);
        JsonNode firstTransferBody = objectMapper.readTree(firstTransfer.body());
        assertEquals(200, firstTransfer.statusCode());
        assertNotNull(firstTransferBody.get("transferId").asText());
        assertEquals(false, firstTransferBody.get("replayed").asBoolean());

        HttpResponse<String> replayTransfer = sendWithHeader(
                "POST",
                "/api/v1/needs/consolidations/" + consolidationId + "/transfer",
                null,
                "aprobador",
                "demo123",
                "Idempotency-Key",
                idempotencyKey);
        JsonNode replayTransferBody = objectMapper.readTree(replayTransfer.body());
        assertEquals(200, replayTransfer.statusCode());
        assertEquals(firstTransferBody.get("transferId").asLong(), replayTransferBody.get("transferId").asLong());
        assertEquals(true, replayTransferBody.get("replayed").asBoolean());
        assertEquals(1, needsBudgetTransferPort.callCount());

        HttpResponse<String> balance = send(
                "GET",
                "/api/v1/needs/balances/" + needsLineId + "?companyId=1",
                null,
                "solicitante",
                "demo123");
        JsonNode balanceBody = objectMapper.readTree(balance.body());
        assertEquals(200, balance.statusCode());
        assertDecimalEquals("12.0000", balanceBody.get("approvedQuantity"));
        assertDecimalEquals("12.0000", balanceBody.get("availableQuantity"));

        HttpResponse<String> traceability = send(
                "GET",
                "/api/v1/needs/traceability/plans/" + planId,
                null,
                "solicitante",
                "demo123");
        JsonNode traceabilityBody = objectMapper.readTree(traceability.body());
        assertEquals(200, traceability.statusCode());
        assertEquals("TRANSFERRED", traceabilityBody.get("plan").get("status").asText());
        assertEquals("TRANSFERRED", traceabilityBody.get("consolidations").get(0).get("status").asText());
    }

    @Test
    void shouldEnforcePlatformAdministrationSecurityAndValidation() throws Exception {
        HttpResponse<String> anonymous = send(
                "GET",
                "/api/v1/platform/catalog/items?companyId=1",
                null,
                null,
                null);
        assertEquals(401, anonymous.statusCode());

        HttpResponse<String> forbiddenSecurityRead = send(
                "GET",
                "/api/v1/platform/security/users/admin/access",
                null,
                "solicitante",
                "demo123");
        assertEquals(403, forbiddenSecurityRead.statusCode());

        HttpResponse<String> invalidPeriodLookup = send(
                "GET",
                "/api/v1/platform/fiscal-periods?companyId=1",
                null,
                "admin",
                "demo123");
        assertEquals(400, invalidPeriodLookup.statusCode());
        JsonNode problem = objectMapper.readTree(invalidPeriodLookup.body());
        assertEquals("BAD_REQUEST", problem.get("code").asText());
    }

    private long firstId(String path) throws Exception {
        HttpResponse<String> response = send("GET", path, null, "solicitante", "demo123");
        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body);
        return body.get(0).get("id").asLong();
    }

    private String buildNeedsPlanRequest() throws Exception {
        return buildNeedsPlanRequest(0, "Plan anual API");
    }

    private String buildNeedsPlanRequest(int costCenterIndex, String title) throws Exception {
        JsonNode costCenters = objectMapper.readTree(send(
                "GET",
                "/api/v1/platform/catalog/cost-centers?companyId=1",
                null,
                "solicitante",
                "demo123").body());
        JsonNode financingSources = objectMapper.readTree(send(
                "GET",
                "/api/v1/platform/catalog/financing-sources?companyId=1",
                null,
                "solicitante",
                "demo123").body());
        JsonNode goals = objectMapper.readTree(send(
                "GET",
                "/api/v1/platform/catalog/goals?companyId=1&fiscalYear=2026",
                null,
                "solicitante",
                "demo123").body());
        JsonNode items = objectMapper.readTree(send(
                "GET",
                "/api/v1/platform/catalog/items?companyId=1",
                null,
                "solicitante",
                "demo123").body());
        JsonNode item = items.get(0);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("companyId", 1);
        request.put("fiscalYear", 2026);
        request.put("costCenterId", costCenters.get(costCenterIndex).get("id").asLong());
        request.put("financingSourceId", financingSources.get(0).get("id").asLong());
        request.put("goalId", goals.get(0).get("id").asLong());
        request.put("title", title);

        ObjectNode detail = objectMapper.createObjectNode();
        detail.put("lineNumber", 1);
        detail.put("catalogItemId", item.get("id").asLong());
        detail.put("expenseClassifierId", item.get("expenseClassifier").get("id").asLong());
        detail.put("unitOfMeasureId", item.get("unitOfMeasure").get("id").asLong());
        detail.put("itemCode", item.get("code").asText());
        detail.put("itemName", item.get("name").asText());
        detail.put("unitCode", item.get("unitOfMeasure").get("code").asText());
        detail.put("requestedQuantity", "12.0000");
        detail.put("estimatedUnitPrice", "100.00");
        var months = detail.putArray("months");
        for (int month = 1; month <= 12; month++) {
            months.addObject()
                    .put("month", month)
                    .put("requestedQuantity", "1.0000");
        }
        request.putArray("details").add(detail);
        return objectMapper.writeValueAsString(request);
    }

    private String buildNeedsReviewRequest() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode revision = objectMapper.createObjectNode();
        revision.put("lineNumber", 1);
        revision.put("reviewedQuantity", "12.0000");
        revision.put("approvedQuantity", "12.0000");
        var months = revision.putArray("months");
        for (int month = 1; month <= 12; month++) {
            months.addObject()
                    .put("month", month)
                    .put("reviewedQuantity", "1.0000")
                    .put("approvedQuantity", "1.0000");
        }
        request.putArray("revisions").add(revision);
        return objectMapper.writeValueAsString(request);
    }

    private void ensureOpenNeedsWindows(Long companyId, int fiscalYear) {
        OffsetDateTime now = OffsetDateTime.now();
        for (TipoVentanaCuadroNecesidad type : TipoVentanaCuadroNecesidad.values()) {
            needsWindows.findByCompanyIdAndFiscalYearAndWindowTypeAndActiveTrue(companyId, fiscalYear, type)
                    .orElseGet(() -> needsWindows.save(new VentanaCuadroNecesidad(
                            companyId,
                            fiscalYear,
                            type,
                            now.minusDays(1),
                            now.plusDays(30))));
        }
    }

    private HttpResponse<String> send(String method, String path, String body, String username, String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (username != null) {
            builder.header("Authorization", "Bearer " + accessToken(username, password));
        }

        if (body != null) {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendWithHeader(
            String method,
            String path,
            String body,
            String username,
            String password,
            String header,
            String value) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header(header, value);

        if (username != null) {
            builder.header("Authorization", "Bearer " + accessToken(username, password));
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
                .header("Authorization", "Bearer " + accessToken(username, password))
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

    private String accessToken(String username, String password) throws Exception {
        String cacheKey = username + ":" + password;
        String cached = accessTokens.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String token = login(username, password).get("accessToken").asText();
        accessTokens.put(cacheKey, token);
        return token;
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private void assertDecimalEquals(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.asText())));
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @TestConfiguration
    static class NeedsBudgetTransferTestConfig {

        @Bean
        @Primary
        RecordingNeedsBudgetTransferPort recordingNeedsBudgetTransferPort() {
            return new RecordingNeedsBudgetTransferPort();
        }
    }

    static class RecordingNeedsBudgetTransferPort implements NeedsBudgetTransferPort {

        private final AtomicLong calls = new AtomicLong();

        @Override
        public NeedsBudgetTransferResult transfer(NeedsBudgetTransferCommand command) {
            calls.incrementAndGet();
            return new NeedsBudgetTransferResult(
                    5000L + command.consolidationId(),
                    7000L + command.fiscalYear(),
                    command.lines().size(),
                    false);
        }

        void reset() {
            calls.set(0);
        }

        long callCount() {
            return calls.get();
        }
    }
}

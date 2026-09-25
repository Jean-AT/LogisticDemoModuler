package com.logistica.demo.logistica.consultas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class LogisticsQueryServiceTest {

    private JdbcTemplate jdbcTemplate;
    private LogisticsQueryService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new LogisticsQueryService(jdbcTemplate);
    }

    @Test
    void shouldBuildDashboardFromOperationalQueries() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(2L, 1L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("10.0000"), new BigDecimal("11.0000"), new BigDecimal("12.34"));

        var dashboard = service.dashboard();

        assertEquals(2L, dashboard.requerimientosPendientesAprobacion());
        assertEquals(1L, dashboard.requerimientosAprobadosSinOc());
        assertEquals(3L, dashboard.ordenesGeneradas());
        assertEquals(4L, dashboard.ordenesAprobadas());
        assertEquals(5L, dashboard.ordenesParcialmenteRecibidas());
        assertEquals(6L, dashboard.ordenesRecibidas());
        assertEquals(7L, dashboard.recepcionesRegistradasHoy());
        assertEquals(8L, dashboard.recepcionesRevertidas());
        assertEquals(9L, dashboard.posicionesStock());
        assertEquals(new BigDecimal("10.0000"), dashboard.stockTotal());
        assertEquals(new BigDecimal("11.0000"), dashboard.cantidadPendienteRecepcion());
        assertEquals(new BigDecimal("12.34"), dashboard.montoOrdenesAprobadas());
    }
}

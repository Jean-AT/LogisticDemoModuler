package com.logistica.demo.logistica.finanzas.service;

import java.math.BigDecimal;

public record DocumentTotals(BigDecimal subtotal, BigDecimal igv, BigDecimal total, BigDecimal tipoCambio) {
}

package com.logistica.demo.finanzas.service;

import com.logistica.demo.shared.config.DemoFinanceProperties;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinancialCalculatorService {

    private static final BigDecimal PEN_EXCHANGE_RATE = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);

    private final DemoFinanceProperties financeProperties;

    public FinancialCalculatorService(DemoFinanceProperties financeProperties) {
        this.financeProperties = financeProperties;
    }

    public BigDecimal calculateLineSubtotal(Integer cantidad, BigDecimal precioUnitario) {
        return precioUnitario
                .multiply(BigDecimal.valueOf(cantidad.longValue()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public DocumentTotals calculateDocumentTotals(List<BigDecimal> subtotales, Moneda moneda) {
        BigDecimal subtotal = subtotales.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(financeProperties.igvRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        return new DocumentTotals(subtotal, igv, total, resolveExchangeRate(moneda));
    }

    public BigDecimal resolveExchangeRate(Moneda moneda) {
        if (moneda == Moneda.USD) {
            return financeProperties.exchangeRate().setScale(4, RoundingMode.HALF_UP);
        }
        return PEN_EXCHANGE_RATE;
    }
}

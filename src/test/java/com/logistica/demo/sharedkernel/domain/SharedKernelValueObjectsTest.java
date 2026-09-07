package com.logistica.demo.sharedkernel.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SharedKernelValueObjectsTest {

    @Test
    void moneyNormalizesScaleAndRequiresSameCurrency() {
        Money base = new Money(new BigDecimal("10.005"), Moneda.PEN);
        Money addition = new Money(new BigDecimal("2.00"), Moneda.PEN);

        assertEquals(new BigDecimal("10.01"), base.amount());
        assertEquals(new BigDecimal("12.01"), base.add(addition).amount());
        assertThrows(
                IllegalArgumentException.class,
                () -> base.add(new Money(BigDecimal.ONE, Moneda.USD)));
    }

    @Test
    void fiscalDimensionRejectsInvalidMonth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FiscalDimension(1L, 2026, 13, 2L, 3L, 4L, 5L));
    }

    @Test
    void documentReferenceNormalizesText() {
        DocumentReference reference = new DocumentReference(" logistica ", " OC ", 10L, " OC-000010 ");

        assertEquals("logistica", reference.module());
        assertEquals("OC", reference.type());
        assertEquals("OC-000010", reference.number());
    }
}

package com.logistica.demo.sharedkernel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, Moneda currency) {

    public Money {
        Objects.requireNonNull(amount, "amount es obligatorio");
        Objects.requireNonNull(currency, "currency es obligatorio");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other es obligatorio");
        if (currency != other.currency) {
            throw new IllegalArgumentException("No se pueden operar importes de monedas diferentes.");
        }
    }
}


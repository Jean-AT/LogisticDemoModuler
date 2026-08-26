package com.logistica.demo.maestros.dto;

public record ItemCreateRequest(
        String code,
        String name,
        String unitMeasure) {
}

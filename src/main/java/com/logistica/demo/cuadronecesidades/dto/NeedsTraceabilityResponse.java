package com.logistica.demo.cuadronecesidades.dto;

import java.util.List;

public record NeedsTraceabilityResponse(
        NeedsPlanResponse plan,
        List<NeedsConsolidationResponse> consolidations) {
}

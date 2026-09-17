package com.logistica.demo.cuadronecesidades.dto;

import java.util.List;

public record ReviewNeedsPlanRequest(List<ReviewNeedLineRequest> revisions) {
}

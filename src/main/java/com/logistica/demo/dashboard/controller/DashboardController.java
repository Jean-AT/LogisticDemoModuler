package com.logistica.demo.dashboard.controller;

import com.logistica.demo.dashboard.dto.DashboardResponse;
import com.logistica.demo.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse resumen() {
        return dashboardService.resumen();
    }
}

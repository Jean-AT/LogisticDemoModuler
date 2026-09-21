package com.logistica.demo.logistica.dashboard.controller;

import com.logistica.demo.logistica.dashboard.dto.DashboardResponse;
import com.logistica.demo.logistica.dashboard.service.DashboardService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/dashboard", ApiPaths.V1 + "/dashboard"})
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

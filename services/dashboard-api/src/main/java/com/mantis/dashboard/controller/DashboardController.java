package com.mantis.dashboard.controller;

import com.mantis.dashboard.model.AlertEntity;
import com.mantis.dashboard.model.MachineState;
import com.mantis.dashboard.service.MachineStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all for demo
@RequiredArgsConstructor
public class DashboardController {

    private final MachineStateService machineStateService;

    @GetMapping("/machines")
    public List<MachineState> getAllMachines() {
        return machineStateService.getAllMachines();
    }

    @GetMapping("/machines/{id}")
    public ResponseEntity<MachineState> getMachine(@PathVariable String id) {
        return machineStateService.getMachine(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/alerts")
    public List<AlertEntity> getRecentAlerts() {
        return machineStateService.getRecentAlerts();
    }
}

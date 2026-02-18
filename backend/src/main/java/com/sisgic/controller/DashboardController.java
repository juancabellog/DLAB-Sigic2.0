package com.sisgic.controller;

import com.sisgic.entity.SyncStatus;
import com.sisgic.repository.SyncStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private SyncStatusRepository syncStatusRepository;

    /**
     * Obtiene la fecha de última actualización de los factores de impacto
     * @return Fecha formateada o null si no existe
     */
    @GetMapping("/impact-metrics-update")
    public ResponseEntity<Map<String, Object>> getImpactMetricsUpdate() {
        Map<String, Object> response = new HashMap<>();
        
        Optional<SyncStatus> syncStatus = syncStatusRepository.findByJobKey("impact_factors");
        
        if (syncStatus.isPresent() && syncStatus.get().getLastSuccessAt() != null) {
            // Formatear la fecha como "02 Feb 2026"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
            String formattedDate = syncStatus.get().getLastSuccessAt()
                .toLocalDateTime()
                .format(formatter);
            
            response.put("lastUpdate", formattedDate);
            response.put("status", syncStatus.get().getStatus());
        } else {
            response.put("lastUpdate", null);
            response.put("status", null);
        }
        
        return ResponseEntity.ok(response);
    }
}

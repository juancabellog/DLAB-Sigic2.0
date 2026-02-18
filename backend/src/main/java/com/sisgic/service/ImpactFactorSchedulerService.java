package com.sisgic.service;

import com.sisgic.entity.SyncStatus;
import com.sisgic.repository.SyncStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ImpactFactorSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ImpactFactorSchedulerService.class);
    private static final String JOB_KEY = "impact_factors";
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private SyncStatusRepository syncStatusRepository;

    /**
     * Tarea programada que se ejecuta todos los días a la 1:00 AM
     * Actualiza los factores de impacto llamando al procedimiento almacenado updateImpactFactor()
     */
    @Scheduled(cron = "0 0 1 * * ?") // 1:00 AM todos los días
    @Transactional
    public void updateImpactFactors() {
        logger.info("Starting scheduled task: updateImpactFactors at {}", LocalDateTime.now());
        
        try {
            // Actualizar status a "Corriendo" (2)
            updateSyncStatus(2, null, Timestamp.valueOf(LocalDateTime.now()));
            
            // Ejecutar el procedimiento almacenado
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("updateImpactFactor");
            query.execute();
            
            // Actualizar status a "Exitoso" (1) y last_success_at
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            updateSyncStatus(1, now, now);
            
            logger.info("Successfully completed updateImpactFactors at {}", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error executing updateImpactFactors: {}", e.getMessage(), e);
            
            // Actualizar status a "Terminó con error" (3)
            updateSyncStatus(3, null, Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    /**
     * Actualiza o crea el registro en sync_status
     */
    private void updateSyncStatus(Integer status, Timestamp lastSuccessAt, Timestamp lastRunAt) {
        Optional<SyncStatus> existingStatus = syncStatusRepository.findByJobKey(JOB_KEY);
        
        SyncStatus syncStatus;
        if (existingStatus.isPresent()) {
            syncStatus = existingStatus.get();
        } else {
            syncStatus = new SyncStatus();
            syncStatus.setJobKey(JOB_KEY);
        }
        
        syncStatus.setStatus(status);
        syncStatus.setLastRunAt(lastRunAt);
        
        if (lastSuccessAt != null) {
            syncStatus.setLastSuccessAt(lastSuccessAt);
        }
        
        syncStatusRepository.save(syncStatus);
    }
}

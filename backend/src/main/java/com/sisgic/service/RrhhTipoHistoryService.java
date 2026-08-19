package com.sisgic.service;

import com.sisgic.entity.RRHH;
import com.sisgic.entity.RrhhTipo;
import com.sisgic.entity.RrhhTipoId;
import com.sisgic.entity.TipoRRHH;
import com.sisgic.repository.RRHHRepository;
import com.sisgic.repository.RrhhTipoRepository;
import com.sisgic.repository.TipoRRHHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class RrhhTipoHistoryService {

    private static final int DEFAULT_PROGRESS_REPORT = 0;

    @Autowired
    private RrhhTipoRepository rrhhTipoRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoRRHHRepository tipoRRHHRepository;

    /**
     * Updates RRHH.idTipoRRHH and appends history in rrhh_tipo:
     * - closes any open rows (fechaTermino = today)
     * - opens a new row for the new tipo (fechaInicio = today, fechaTermino = null)
     * No-op when the tipo did not change.
     */
    public boolean changeTipoRrhh(Long idRRHH, Long newTipoId) {
        if (idRRHH == null || newTipoId == null) {
            return false;
        }

        RRHH person = rrhhRepository.findById(idRRHH).orElse(null);
        if (person == null) {
            return false;
        }

        TipoRRHH newTipo = tipoRRHHRepository.findById(newTipoId).orElse(null);
        if (newTipo == null) {
            return false;
        }

        Long currentTipoId = person.getTipoRRHH() != null ? person.getTipoRRHH().getId() : null;
        if (Objects.equals(currentTipoId, newTipoId)) {
            ensureActiveHistoryRow(idRRHH, newTipoId);
            return false;
        }

        LocalDate today = LocalDate.now();
        closeActiveRows(idRRHH, today);

        // If previous tipo had no history yet, backfill a closed segment ending today.
        if (currentTipoId != null) {
            backfillClosedPreviousIfMissing(idRRHH, currentTipoId, today);
        }

        openOrReopen(idRRHH, newTipoId, today);

        person.setTipoRRHH(newTipo);
        rrhhRepository.save(person);
        return true;
    }

    /**
     * Records the initial tipo for a newly created RRHH (open history row).
     */
    public void recordInitialTipo(Long idRRHH, Long tipoId) {
        if (idRRHH == null || tipoId == null) {
            return;
        }
        ensureActiveHistoryRow(idRRHH, tipoId);
    }

    private void ensureActiveHistoryRow(Long idRRHH, Long tipoId) {
        List<RrhhTipo> active = rrhhTipoRepository.findActiveByIdRRHH(idRRHH);
        boolean alreadyOpen = active.stream().anyMatch(r -> Objects.equals(r.getIdTipoRRHH(), tipoId));
        if (alreadyOpen) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (!active.isEmpty()) {
            closeActiveRows(idRRHH, today);
        }
        openOrReopen(idRRHH, tipoId, today);
    }

    private void closeActiveRows(Long idRRHH, LocalDate endDate) {
        List<RrhhTipo> active = rrhhTipoRepository.findActiveByIdRRHH(idRRHH);
        for (RrhhTipo row : active) {
            LocalDate termino = endDate;
            if (row.getFechaInicio() != null && row.getFechaInicio().isAfter(termino)) {
                termino = row.getFechaInicio();
            }
            row.setFechaTermino(termino);
            rrhhTipoRepository.save(row);
        }
    }

    private void backfillClosedPreviousIfMissing(Long idRRHH, Long previousTipoId, LocalDate today) {
        List<RrhhTipo> all = rrhhTipoRepository.findByIdRRHHOrderByFechaInicioDesc(idRRHH);
        boolean hasAnyForPrevious = all.stream().anyMatch(r -> Objects.equals(r.getIdTipoRRHH(), previousTipoId));
        if (hasAnyForPrevious) {
            return;
        }
        RrhhTipoId id = new RrhhTipoId(idRRHH, previousTipoId, today);
        if (rrhhTipoRepository.existsById(id)) {
            return;
        }
        RrhhTipo closed = new RrhhTipo();
        closed.setIdRRHH(idRRHH);
        closed.setIdTipoRRHH(previousTipoId);
        closed.setFechaInicio(today);
        closed.setFechaTermino(today);
        closed.setProgressReport(DEFAULT_PROGRESS_REPORT);
        rrhhTipoRepository.save(closed);
    }

    private void openOrReopen(Long idRRHH, Long tipoId, LocalDate startDate) {
        RrhhTipoId id = new RrhhTipoId(idRRHH, tipoId, startDate);
        Optional<RrhhTipo> existing = rrhhTipoRepository.findById(id);
        if (existing.isPresent()) {
            RrhhTipo row = existing.get();
            row.setFechaTermino(null);
            if (row.getProgressReport() == null) {
                row.setProgressReport(DEFAULT_PROGRESS_REPORT);
            }
            rrhhTipoRepository.save(row);
            return;
        }

        RrhhTipo row = new RrhhTipo();
        row.setIdRRHH(idRRHH);
        row.setIdTipoRRHH(tipoId);
        row.setFechaInicio(startDate);
        row.setFechaTermino(null);
        row.setProgressReport(DEFAULT_PROGRESS_REPORT);
        rrhhTipoRepository.save(row);
    }
}

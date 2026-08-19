package com.sisgic.controller;

import com.sisgic.dto.ProyectoDTO;
import com.sisgic.entity.Proyecto;
import com.sisgic.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only catalog of projects from {@code v_proyectos} (legacy project codes used by producto_proyecto).
 * Scientific product Projects live under {@code /api/projects}.
 */
@RestController
@RequestMapping("/api/project-catalog")
@CrossOrigin(origins = "*")
public class ProjectCatalogController {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @GetMapping
    public ResponseEntity<Page<ProyectoDTO>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "codigo") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(proyectoRepository.findAll(pageable).map(this::convertToDTO));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ProyectoDTO> getProject(@PathVariable String codigo) {
        return proyectoRepository.findById(codigo)
            .map(proyecto -> ResponseEntity.ok(convertToDTO(proyecto)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProyectoDTO>> getActiveProjects() {
        List<ProyectoDTO> proyectosDTO = proyectoRepository.findAll().stream()
            .filter(p -> p.getFechaTermino() == null || p.getFechaTermino().isAfter(java.time.LocalDate.now()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(proyectosDTO);
    }

    private ProyectoDTO convertToDTO(Proyecto proyecto) {
        ProyectoDTO dto = new ProyectoDTO();
        dto.setCodigo(proyecto.getCodigo());
        dto.setIdDescripcion(proyecto.getIdDescripcion());
        dto.setDescripcion(proyecto.getDescripcion());
        dto.setFechaInicio(proyecto.getFechaInicio());
        dto.setFechaTermino(proyecto.getFechaTermino());
        dto.setCodigoExterno(proyecto.getCodigoExterno());
        dto.setTipoFinanciamiento(proyecto.getTipoFinanciamiento());
        dto.setRealizaCon(proyecto.getRealizaCon());
        dto.setTotalProductos(proyecto.getTotalProductos());
        dto.setCreatedAt(proyecto.getCreatedAt() != null ? proyecto.getCreatedAt().toString() : null);
        dto.setUpdatedAt(proyecto.getUpdatedAt() != null ? proyecto.getUpdatedAt().toString() : null);
        return dto;
    }
}

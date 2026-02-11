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

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProyectoController {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @GetMapping
    public ResponseEntity<Page<ProyectoDTO>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "codigo") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Proyecto> proyectos = proyectoRepository.findAll(pageable);
        
        Page<ProyectoDTO> proyectosDTO = proyectos.map(this::convertToDTO);
        
        return ResponseEntity.ok(proyectosDTO);
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ProyectoDTO> getProject(@PathVariable String codigo) {
        return proyectoRepository.findById(codigo)
            .map(proyecto -> ResponseEntity.ok(convertToDTO(proyecto)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProyectoDTO>> searchProjects(@RequestParam String q) {
        List<Proyecto> proyectos = proyectoRepository.findAll().stream()
            .filter(p -> p.getDescripcion().toLowerCase().contains(q.toLowerCase()) ||
                        p.getCodigo().toLowerCase().contains(q.toLowerCase()) ||
                        (p.getCodigoExterno() != null && p.getCodigoExterno().toLowerCase().contains(q.toLowerCase())))
            .collect(Collectors.toList());
        
        List<ProyectoDTO> proyectosDTO = proyectos.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(proyectosDTO);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProyectoDTO>> getActiveProjects() {
        // Proyectos activos son aquellos sin fecha de término o con fecha de término futura
        List<Proyecto> proyectos = proyectoRepository.findAll().stream()
            .filter(p -> p.getFechaTermino() == null || 
                        p.getFechaTermino().isAfter(java.time.LocalDate.now()))
            .collect(Collectors.toList());
        
        List<ProyectoDTO> proyectosDTO = proyectos.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(proyectosDTO);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getProjectStats() {
        long totalProjects = proyectoRepository.count();
        long activeProjects = proyectoRepository.findAll().stream()
            .filter(p -> p.getFechaTermino() == null || 
                        p.getFechaTermino().isAfter(java.time.LocalDate.now()))
            .count();
        
        return ResponseEntity.ok(new ProjectStats(totalProjects, activeProjects, totalProjects - activeProjects));
    }
    
    // Clase interna para las estadísticas
    public static class ProjectStats {
        public final long totalProjects;
        public final long activeProjects;
        public final long completedProjects;
        
        public ProjectStats(long totalProjects, long activeProjects, long completedProjects) {
            this.totalProjects = totalProjects;
            this.activeProjects = activeProjects;
            this.completedProjects = completedProjects;
        }
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
        dto.setTotalProductos(proyecto.getTotalProductos() != null ? proyecto.getTotalProductos() : 0L);
        dto.setCreatedAt(proyecto.getCreatedAt() != null ? proyecto.getCreatedAt().toString() : null);
        dto.setUpdatedAt(proyecto.getUpdatedAt() != null ? proyecto.getUpdatedAt().toString() : null);
        return dto;
    }
}

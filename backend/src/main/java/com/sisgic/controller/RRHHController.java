package com.sisgic.controller;

import com.sisgic.dto.RRHHDTO;
import com.sisgic.dto.TipoRRHHDTO;
import com.sisgic.entity.RRHH;
import com.sisgic.entity.TipoRRHH;
import com.sisgic.repository.RRHHRepository;
import com.sisgic.repository.TipoRRHHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/researchers")
@CrossOrigin(origins = "*")
public class RRHHController {

    @Autowired
    private RRHHRepository rrhhRepository;
    
    @Autowired
    private TipoRRHHRepository tipoRRHHRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<RRHHDTO>> getResearchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RRHH> researchers = rrhhRepository.findAllWithTipoRRHH(pageable);
        // Convertir a DTO dentro de la transacción para evitar lazy loading durante serialización
        Page<RRHHDTO> researchersDTO = researchers.map(this::convertToDTO);

        return ResponseEntity.ok(researchersDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RRHHDTO> getResearcherById(@PathVariable Long id) {
        RRHH researcher = rrhhRepository.findByIdWithTipoRRHH(id);
        if (researcher != null) {
            return ResponseEntity.ok(convertToDTO(researcher));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<RRHHDTO>> searchResearchers(@RequestParam String q) {
        List<RRHH> researchers = rrhhRepository.findByQuery(q);
        List<RRHHDTO> researchersDTO = researchers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(researchersDTO);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getResearcherStats() {
        long totalResearchers = rrhhRepository.count();
        long maleResearchers = rrhhRepository.findAll().stream()
            .filter(r -> "M".equals(r.getCodigoGenero()))
            .count();
        long femaleResearchers = rrhhRepository.findAll().stream()
            .filter(r -> "F".equals(r.getCodigoGenero()))
            .count();

        return ResponseEntity.ok(new ResearcherStats(totalResearchers, maleResearchers, femaleResearchers));
    }

    @GetMapping("/types")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TipoRRHHDTO>> getResearcherTypes() {
        List<TipoRRHH> tipos = tipoRRHHRepository.findAll();
        List<TipoRRHHDTO> tiposDTO = tipos.stream()
            .map(this::convertTipoRRHHToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(tiposDTO);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createResearcher(@RequestBody CreateResearcherRequest request) {
        // Validar campos requeridos
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Full name is required");
        }
        
        // Validar formato de email si se proporciona
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!Pattern.matches(emailPattern, request.getEmail())) {
                return ResponseEntity.badRequest().body("Error: Invalid email format");
            }
        }
        
        // Validar formato de ORCID si se proporciona
        if (request.getOrcid() != null && !request.getOrcid().trim().isEmpty()) {
            String orcidPattern = "^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$";
            String normalizedOrcid = normalizeOrcid(request.getOrcid());
            if (!Pattern.matches(orcidPattern, normalizedOrcid)) {
                return ResponseEntity.badRequest().body("Error: Invalid ORCID format. Expected format: XXXX-XXXX-XXXX-XXXX");
            }
            
            // Verificar duplicados por ORCID
            List<RRHH> existingByOrcid = rrhhRepository.findAll().stream()
                .filter(r -> normalizedOrcid.equals(r.getOrcid()))
                .collect(Collectors.toList());
            
            if (!existingByOrcid.isEmpty()) {
                RRHH existing = existingByOrcid.get(0);
                return ResponseEntity.badRequest()
                    .body("Error: Researcher with ORCID " + normalizedOrcid + " already exists: " + 
                          (existing.getFullname() != null ? existing.getFullname() : "ID " + existing.getId()));
            }
        }
        
        // Parsear nombre completo en partes
        String[] nameParts = parseFullName(request.getFullName());
        String primerNombre = nameParts[0];
        String segundoNombre = nameParts[1];
        String primerApellido = nameParts[2];
        String segundoApellido = nameParts[3];
        
        // Obtener TipoRRHH
        TipoRRHH tipoRRHH = null;
        if (request.getRrhhTypeId() != null) {
            tipoRRHH = tipoRRHHRepository.findById(request.getRrhhTypeId())
                .orElse(null);
        }
        
        // Si no se proporciona tipo o no se encuentra, usar el tipo por defecto (ID 32 o el primero disponible)
        if (tipoRRHH == null) {
            tipoRRHH = tipoRRHHRepository.findById(32L).orElse(null);
            if (tipoRRHH == null) {
                List<TipoRRHH> tipos = tipoRRHHRepository.findAll();
                if (!tipos.isEmpty()) {
                    tipoRRHH = tipos.get(0);
                }
            }
        }
        
        // Crear nuevo RRHH
        RRHH nuevoRRHH = new RRHH();
        nuevoRRHH.setPrimerNombre(primerNombre);
        nuevoRRHH.setSegundoNombre(segundoNombre.isEmpty() ? null : segundoNombre);
        nuevoRRHH.setPrimerApellido(primerApellido.isEmpty() ? null : primerApellido);
        nuevoRRHH.setSegundoApellido(segundoApellido.isEmpty() ? null : segundoApellido);
        nuevoRRHH.setTipoRRHH(tipoRRHH);
        nuevoRRHH.setIdRecurso(request.getRut() != null && !request.getRut().trim().isEmpty() ? request.getRut().trim() : null);
        nuevoRRHH.setEmail(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null);
        nuevoRRHH.setOrcid(request.getOrcid() != null && !request.getOrcid().trim().isEmpty() ? normalizeOrcid(request.getOrcid()) : null);
        
        nuevoRRHH = rrhhRepository.save(nuevoRRHH);
        
        // Recargar el investigador para obtener el fullname calculado por la función MySQL
        RRHH reloadedRRHH = rrhhRepository.findByIdWithTipoRRHH(nuevoRRHH.getId());
        if (reloadedRRHH == null) {
            reloadedRRHH = nuevoRRHH;
        }
        
        // Convertir a DTO y retornar
        RRHHDTO dto = convertToDTO(reloadedRRHH);
        
        // Si el fullname en el DTO está vacío (campo calculado puede no estar disponible inmediatamente),
        // construir uno manualmente desde los campos individuales como fallback
        if (dto.getFullname() == null || dto.getFullname().trim().isEmpty()) {
            StringBuilder fullnameBuilder = new StringBuilder();
            if (reloadedRRHH.getPrimerNombre() != null && !reloadedRRHH.getPrimerNombre().trim().isEmpty()) {
                fullnameBuilder.append(reloadedRRHH.getPrimerNombre());
            }
            if (reloadedRRHH.getSegundoNombre() != null && !reloadedRRHH.getSegundoNombre().trim().isEmpty()) {
                if (fullnameBuilder.length() > 0) fullnameBuilder.append(" ");
                fullnameBuilder.append(reloadedRRHH.getSegundoNombre());
            }
            if (reloadedRRHH.getPrimerApellido() != null && !reloadedRRHH.getPrimerApellido().trim().isEmpty()) {
                if (fullnameBuilder.length() > 0) fullnameBuilder.append(" ");
                fullnameBuilder.append(reloadedRRHH.getPrimerApellido());
            }
            if (reloadedRRHH.getSegundoApellido() != null && !reloadedRRHH.getSegundoApellido().trim().isEmpty()) {
                if (fullnameBuilder.length() > 0) fullnameBuilder.append(" ");
                fullnameBuilder.append(reloadedRRHH.getSegundoApellido());
            }
            String constructedFullname = fullnameBuilder.toString().trim();
            if (!constructedFullname.isEmpty()) {
                dto.setFullname(constructedFullname);
            }
        }
        
        return ResponseEntity.ok(dto);
    }
    
    private String normalizeOrcid(String orcid) {
        if (orcid == null) return null;
        String normalized = orcid.trim();
        // Remover URL si viene con formato completo
        if (normalized.contains("/")) {
            normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        // Asegurar formato correcto (19 caracteres)
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        return normalized;
    }
    
    private String[] parseFullName(String fullName) {
        // Limpiar nombre (remover guiones y puntos)
        String name = fullName.replaceAll("-", " ").replaceAll("[.]", " ");
        String[] tokens = name.trim().split("[ ]+");
        
        String pn = capitalize(tokens.length > 0 ? tokens[0] : "");
        String sn = "";
        String pa = "";
        String sa = "";
        
        if (tokens.length == 2) {
            pa = capitalize(tokens[1]);
        } else if (tokens.length == 3) {
            if (tokens[1].length() == 1) {
                sn = capitalize(tokens[1]);
                pa = capitalize(tokens[2]);
            } else {
                pa = capitalize(tokens[1]);
                sa = capitalize(tokens[2]);
            }
        } else if (tokens.length == 4) {
            sn = capitalize(tokens[1]);
            pa = capitalize(tokens[2]);
            sa = capitalize(tokens[3]);
        } else if (tokens.length == 5) {
            sn = capitalize(tokens[1]);
            pa = capitalize(tokens[2]);
            sa = capitalize(tokens[3]) + " " + capitalize(tokens[4]);
        } else if (tokens.length > 5) {
            // Para más de 5 tokens, tomar los primeros 4 como antes y el resto como segundo apellido
            sn = capitalize(tokens[1]);
            pa = capitalize(tokens[2]);
            sa = capitalize(tokens[3]);
            for (int i = 4; i < tokens.length; i++) {
                sa += " " + capitalize(tokens[i]);
            }
        }
        
        return new String[]{pn, sn, pa, sa};
    }
    
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        if (word.length() == 1) {
            return word; // Si tiene 1 carácter, retorna el carácter tal cual (sin convertir a mayúscula)
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
    
    public static class CreateResearcherRequest {
        private String fullName;
        private String email;
        private String orcid;
        private String rut;
        private Long rrhhTypeId;
        
        // Getters and Setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOrcid() { return orcid; }
        public void setOrcid(String orcid) { this.orcid = orcid; }
        public String getRut() { return rut; }
        public void setRut(String rut) { this.rut = rut; }
        public Long getRrhhTypeId() { return rrhhTypeId; }
        public void setRrhhTypeId(Long rrhhTypeId) { this.rrhhTypeId = rrhhTypeId; }
    }

    // Clase interna para las estadísticas
    public static class ResearcherStats {
        public final long totalResearchers;
        public final long maleResearchers;
        public final long femaleResearchers;

        public ResearcherStats(long totalResearchers, long maleResearchers, long femaleResearchers) {
            this.totalResearchers = totalResearchers;
            this.maleResearchers = maleResearchers;
            this.femaleResearchers = femaleResearchers;
        }
    }

    private RRHHDTO convertToDTO(RRHH rrhh) {
        RRHHDTO dto = new RRHHDTO();
        dto.setId(rrhh.getId());
        dto.setIdRecurso(rrhh.getIdRecurso());
        dto.setFullname(rrhh.getFullname());
        dto.setIdTipoRRHH(rrhh.getTipoRRHH() != null ? rrhh.getTipoRRHH().getId() : null);
        dto.setTipoRRHH(convertTipoRRHHToDTO(rrhh.getTipoRRHH()));
        dto.setNumCelular(rrhh.getNumCelular());
        dto.setEmail(rrhh.getEmail());
        dto.setIniciales(rrhh.getIniciales());
        dto.setOrcid(rrhh.getOrcid());
        dto.setCodigoGenero(rrhh.getCodigoGenero());
        dto.setCreatedAt(rrhh.getCreatedAt() != null ? rrhh.getCreatedAt().toString() : null);
        dto.setUpdatedAt(rrhh.getUpdatedAt() != null ? rrhh.getUpdatedAt().toString() : null);
        return dto;
    }
    
    private TipoRRHHDTO convertTipoRRHHToDTO(TipoRRHH tipoRRHH) {
        if (tipoRRHH == null) return null;
        
        TipoRRHHDTO dto = new TipoRRHHDTO();
        dto.setId(tipoRRHH.getId());
        dto.setIdDescripcion(tipoRRHH.getCodigoDescripcion());
        dto.setDescripcion(tipoRRHH.getDescripcion()); // Usar el campo descripcion real
        dto.setCreatedAt(tipoRRHH.getCreatedAt() != null ? tipoRRHH.getCreatedAt().toString() : null);
        dto.setUpdatedAt(tipoRRHH.getUpdatedAt() != null ? tipoRRHH.getUpdatedAt().toString() : null);
        return dto;
    }
}
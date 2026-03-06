package com.sisgic.controller;

import com.sisgic.dto.TransferenciaTecnologicaDTO;
import com.sisgic.dto.EstadoProductoDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.InstitucionDTO;
import com.sisgic.dto.TipoTransferenciaDTO;
import com.sisgic.dto.PaisDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/technology-transfer")
@CrossOrigin(origins = "*")
public class TransferenciaTecnologicaController {

    @Autowired
    private TransferenciaTecnologicaRepository transferenciaTecnologicaRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private TipoTransferenciaRepository tipoTransferenciaRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private EstadoProductoRepository estadoProductoRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private TipoParticipacionRepository tipoParticipacionRepository;

    @Autowired
    private ParticipacionProductoRepository participacionProductoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private com.sisgic.service.PdfFileService pdfFileService;

    @Autowired
    private com.sisgic.service.UserService userService;

    /**
     * Obtiene todas las transferencias tecnológicas con paginación
     */
    @GetMapping
    public ResponseEntity<Page<TransferenciaTecnologicaDTO>> getTechnologyTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
            Sort.by(sortBy).descending() :
            Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Obtener idRRHH y userName del usuario conectado
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        Page<TransferenciaTecnologica> transfers = transferenciaTecnologicaRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        // Cargar todos los textos de una vez para optimizar consultas
        List<TransferenciaTecnologica> transfersList = transfers.getContent();
        List<String> codigosTexto = new ArrayList<>();
        for (TransferenciaTecnologica t : transfersList) {
            if (t.getDescripcion() != null && !t.getDescripcion().isEmpty()) {
                codigosTexto.add(t.getDescripcion());
            }
            if (t.getComentario() != null && !t.getComentario().isEmpty()) {
                codigosTexto.add(t.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");

        // Para listas, no cargar participantes para mejorar rendimiento
        Page<TransferenciaTecnologicaDTO> transfersDTO = transfers.map(t -> convertToDTOWithoutParticipants(t, textosMap));

        return ResponseEntity.ok(transfersDTO);
    }

    /**
     * Obtiene una transferencia tecnológica específica por ID
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<TransferenciaTecnologicaDTO> getTechnologyTransfer(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        
        // Primero verificar visibilidad con la consulta nativa
        Optional<TransferenciaTecnologica> transferVisible = transferenciaTecnologicaRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName);
        if (transferVisible.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Recargar la entidad con todas sus relaciones usando findByIdWithRelations
        return transferenciaTecnologicaRepository.findByIdWithRelations(id)
            .map(transfer -> ResponseEntity.ok(convertToDTO(transfer)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva transferencia tecnológica
     */
    @PostMapping
    @Transactional
    public ResponseEntity<TransferenciaTecnologicaDTO> createTechnologyTransfer(@RequestBody TransferenciaTecnologicaDTO dto) {
        try {
            TransferenciaTecnologica transfer = convertFromDTO(dto);
            // Establecer el username del usuario actual solo al crear
            userService.getCurrentUsername().ifPresent(transfer::setUsername);
            TransferenciaTecnologica saved = transferenciaTecnologicaRepository.save(transfer);

            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }

            // Recargar la entidad con todas sus relaciones antes de convertir a DTO
            TransferenciaTecnologica transferConRelaciones = transferenciaTecnologicaRepository.findByIdWithRelations(saved.getId())
                .orElse(saved);
            
            TransferenciaTecnologicaDTO resultDTO = convertToDTO(transferConRelaciones);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualiza una transferencia tecnológica existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<TransferenciaTecnologicaDTO> updateTechnologyTransfer(
            @PathVariable Long id,
            @RequestBody TransferenciaTecnologicaDTO dto) {

        return transferenciaTecnologicaRepository.findById(id)
            .map(existingTransfer -> {
                // Actualizar campos de ProductoCientifico
                // Actualizar descripción: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
                    if (existingTransfer.getDescripcion() != null && !existingTransfer.getDescripcion().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingTransfer.getDescripcion(), dto.getDescripcion(), 2);
                    } else {
                        String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
                        existingTransfer.setDescripcion(codigoDescripcion);
                    }
                }
                // Actualizar comentario: si ya existe código, actualizar; si no, crear nuevo
                if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
                    if (existingTransfer.getComentario() != null && !existingTransfer.getComentario().isEmpty()) {
                        textosService.updateTextInBothLanguages(existingTransfer.getComentario(), dto.getComentario(), 2);
                    } else {
                        String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
                        existingTransfer.setComentario(codigoComentario);
                    }
                }
                // fechaInicio es obligatorio
                if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
                    throw new IllegalArgumentException("fechaInicio is required");
                }
                existingTransfer.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
                if (dto.getFechaTermino() != null) {
                    existingTransfer.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
                }
                if (dto.getUrlDocumento() != null) {
                    existingTransfer.setUrlDocumento(dto.getUrlDocumento());
                }
                if (dto.getLinkVisualizacion() != null) {
                    existingTransfer.setLinkVisualizacion(dto.getLinkVisualizacion());
                }
                // Actualizar linkPDF siempre (puede ser null para limpiarlo)
                existingTransfer.setLinkPDF(dto.getLinkPDF());
                if (dto.getProgressReport() != null) {
                    existingTransfer.setProgressReport(dto.getProgressReport());
                }
                if (dto.getCodigoANID() != null) {
                    existingTransfer.setCodigoANID(dto.getCodigoANID());
                }
                // Manejar basal: debe ser "S" o "N"
                if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
                    char basalValue = dto.getBasal().charAt(0);
                    if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                        existingTransfer.setBasal(Character.toUpperCase(basalValue));
                    } else {
                        existingTransfer.setBasal('N');
                    }
                } else {
                    existingTransfer.setBasal('N');
                }
                if (dto.getLineasInvestigacion() != null) {
                    existingTransfer.setLineasInvestigacion(dto.getLineasInvestigacion());
                }

                // Actualizar relaciones de ProductoCientifico
                if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
                    tipoProductoRepository.findById(dto.getTipoProducto().getId())
                        .ifPresent(existingTransfer::setTipoProducto);
                }
                if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
                    estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                        .ifPresent(existingTransfer::setEstadoProducto);
                }

                // Actualizar campos específicos de TransferenciaTecnologica
                if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
                    institucionRepository.findById(dto.getInstitucion().getId())
                        .ifPresent(existingTransfer::setInstitucion);
                }
                if (dto.getTipoTransferencia() != null && dto.getTipoTransferencia().getId() != null) {
                    tipoTransferenciaRepository.findById(dto.getTipoTransferencia().getId())
                        .ifPresent(existingTransfer::setTipoTransferencia);
                }
                if (dto.getCategoriaTransferencia() != null) {
                    existingTransfer.setCategoriaTransferencia(dto.getCategoriaTransferencia());
                }
                if (dto.getCiudad() != null) {
                    existingTransfer.setCiudad(dto.getCiudad());
                }
                if (dto.getRegion() != null) {
                    existingTransfer.setRegion(dto.getRegion());
                }
                if (dto.getAgno() != null) {
                    existingTransfer.setAgno(dto.getAgno());
                }
                if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
                    paisRepository.findById(dto.getPais().getCodigo())
                        .ifPresent(existingTransfer::setPais);
                }

                // Actualizar participantes
                if (dto.getParticipantes() != null) {
                    participacionProductoRepository.deleteByProductoId(existingTransfer.getId());
                    if (!dto.getParticipantes().isEmpty()) {
                        saveParticipantes(existingTransfer, dto.getParticipantes());
                    }
                }

                TransferenciaTecnologica updated = transferenciaTecnologicaRepository.save(existingTransfer);
                
                // Recargar la entidad con todas sus relaciones antes de convertir a DTO
                TransferenciaTecnologica transferConRelaciones = transferenciaTecnologicaRepository.findByIdWithRelations(updated.getId())
                    .orElse(updated);
                
                return ResponseEntity.ok(convertToDTO(transferConRelaciones));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una transferencia tecnológica
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTechnologyTransfer(@PathVariable Long id) {
        return transferenciaTecnologicaRepository.findById(id)
            .map(transfer -> {
                // Eliminar archivo PDF asociado si existe
                if (transfer.getLinkPDF() != null && !transfer.getLinkPDF().trim().isEmpty()) {
                    pdfFileService.deletePdfFile(transfer.getLinkPDF());
                }
                // Eliminar participantes asociados
                participacionProductoRepository.deleteByProductoId(id);
                // Eliminar el registro
                transferenciaTecnologicaRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convierte una entidad TransferenciaTecnologica a DTO (sin participantes - para listas)
     */
    private TransferenciaTecnologicaDTO convertToDTOWithoutParticipants(TransferenciaTecnologica transfer) {
        return convertToDTOWithoutParticipants(transfer, null);
    }
    
    /**
     * Convierte una entidad TransferenciaTecnologica a DTO (sin participantes - para listas) con textos precargados
     */
    private TransferenciaTecnologicaDTO convertToDTOWithoutParticipants(TransferenciaTecnologica transfer, Map<String, String> textosMap) {
        TransferenciaTecnologicaDTO dto = convertToDTOBase(transfer, textosMap);
        // No cargar participantes para mejorar rendimiento en listas
        dto.setParticipantes(null);
        return dto;
    }

    /**
     * Convierte una entidad TransferenciaTecnologica a DTO (con participantes - para detalles)
     */
    private TransferenciaTecnologicaDTO convertToDTO(TransferenciaTecnologica transfer) {
        TransferenciaTecnologicaDTO dto = convertToDTOBase(transfer);
        
        // Cargar participantes solo cuando se necesita (detalles)
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(transfer.getId());
        List<ParticipanteDTO> participantesDTO = participaciones.stream()
            .map(pp -> {
                ParticipanteDTO pDTO = new ParticipanteDTO();
                pDTO.setRrhhId(pp.getRrhh() != null ? pp.getRrhh().getId() : null);
                pDTO.setTipoParticipacionId(pp.getTipoParticipacion() != null ? pp.getTipoParticipacion().getId() : null);
                pDTO.setOrden(pp.getOrden());
                pDTO.setCorresponding(pp.isCorresponding());
                return pDTO;
            })
            .collect(Collectors.toList());
        dto.setParticipantes(participantesDTO);

        return dto;
    }

    /**
     * Método base para convertir TransferenciaTecnologica a DTO (sin participantes)
     */
    private TransferenciaTecnologicaDTO convertToDTOBase(TransferenciaTecnologica transfer) {
        return convertToDTOBase(transfer, null);
    }
    
    /**
     * Método base para convertir TransferenciaTecnologica a DTO (sin participantes) con textos precargados
     */
    private TransferenciaTecnologicaDTO convertToDTOBase(TransferenciaTecnologica transfer, Map<String, String> textosMap) {
        TransferenciaTecnologicaDTO dto = new TransferenciaTecnologicaDTO();

        // Campos de ProductoCientifico
        dto.setId(transfer.getId());
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idDescripcion
        if (transfer.getDescripcion() != null && !transfer.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(transfer.getDescripcion())
                ? textosMap.get(transfer.getDescripcion())
                : textosService.getTextValue(transfer.getDescripcion(), 2, "us").orElse(transfer.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        // Obtener el valor del texto desde la tabla textos usando el código guardado en idComentario
        if (transfer.getComentario() != null && !transfer.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(transfer.getComentario())
                ? textosMap.get(transfer.getComentario())
                : textosService.getTextValue(transfer.getComentario(), 2, "us").orElse(transfer.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(transfer.getFechaInicio() != null ? transfer.getFechaInicio().toString() : null);
        dto.setFechaTermino(transfer.getFechaTermino() != null ? transfer.getFechaTermino().toString() : null);
        dto.setUrlDocumento(transfer.getUrlDocumento());
        dto.setLinkVisualizacion(transfer.getLinkVisualizacion());
        dto.setLinkPDF(transfer.getLinkPDF());
        dto.setProgressReport(transfer.getProgressReport());
        dto.setCodigoANID(transfer.getCodigoANID());
        // Normalizar basal: convertir '1' a 'S' y '0' a 'N', mantener 'S'/'N' como están
        if (transfer.getBasal() != null) {
            char basalChar = transfer.getBasal();
            if (basalChar == '1') {
                dto.setBasal("S");
            } else if (basalChar == '0') {
                dto.setBasal("N");
            } else {
                dto.setBasal(String.valueOf(basalChar));
            }
        } else {
            dto.setBasal(null);
        }
        dto.setLineasInvestigacion(transfer.getLineasInvestigacion());
        dto.setParticipantesNombres(transfer.getParticipantesNombres());
        dto.setCreatedAt(transfer.getCreatedAt() != null ? transfer.getCreatedAt().toString() : null);
        dto.setUpdatedAt(transfer.getUpdatedAt() != null ? transfer.getUpdatedAt().toString() : null);

        // Relaciones de ProductoCientifico
        if (transfer.getTipoProducto() != null) {
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(transfer.getTipoProducto().getId());
            tipoDTO.setIdDescripcion(transfer.getTipoProducto().getIdDescripcion());
            tipoDTO.setDescripcion(transfer.getTipoProducto().getDescripcion());
            dto.setTipoProducto(tipoDTO);
        }

        if (transfer.getEstadoProducto() != null) {
            EstadoProductoDTO estadoDTO = new EstadoProductoDTO();
            estadoDTO.setId(transfer.getEstadoProducto().getId());
            estadoDTO.setCodigoDescripcion(transfer.getEstadoProducto().getCodigoDescripcion());
            estadoDTO.setCreatedAt(transfer.getEstadoProducto().getCreatedAt() != null ?
                transfer.getEstadoProducto().getCreatedAt().toString() : null);
            estadoDTO.setUpdatedAt(transfer.getEstadoProducto().getUpdatedAt() != null ?
                transfer.getEstadoProducto().getUpdatedAt().toString() : null);
            dto.setEstadoProducto(estadoDTO);
        }

        // Campos específicos de TransferenciaTecnologica
        if (transfer.getInstitucion() != null) {
            InstitucionDTO institucionDTO = new InstitucionDTO();
            institucionDTO.setId(transfer.getInstitucion().getId());
            institucionDTO.setIdDescripcion(transfer.getInstitucion().getIdDescripcion());
            institucionDTO.setDescripcion(transfer.getInstitucion().getDescripcion());
            dto.setInstitucion(institucionDTO);
        }

        if (transfer.getTipoTransferencia() != null) {
            TipoTransferenciaDTO tipoTransferenciaDTO = new TipoTransferenciaDTO();
            tipoTransferenciaDTO.setId(transfer.getTipoTransferencia().getId());
            tipoTransferenciaDTO.setIdDescripcion(transfer.getTipoTransferencia().getIdDescripcion());
            dto.setTipoTransferencia(tipoTransferenciaDTO);
        }

        dto.setCategoriaTransferencia(transfer.getCategoriaTransferencia());
        dto.setCiudad(transfer.getCiudad());
        dto.setRegion(transfer.getRegion());
        dto.setAgno(transfer.getAgno());

        if (transfer.getPais() != null) {
            PaisDTO paisDTO = new PaisDTO();
            paisDTO.setCodigo(transfer.getPais().getCodigo());
            paisDTO.setIdDescripcion(transfer.getPais().getIdDescripcion());
            dto.setPais(paisDTO);
        }

        return dto;
    }

    /**
     * Convierte un DTO a entidad TransferenciaTecnologica
     */
    private TransferenciaTecnologica convertFromDTO(TransferenciaTecnologicaDTO dto) {
        TransferenciaTecnologica transfer = new TransferenciaTecnologica();

        // Campos de ProductoCientifico
        // Generar código de texto para descripción si se proporciona
        if (dto.getDescripcion() != null && !dto.getDescripcion().isEmpty()) {
            String codigoDescripcion = textosService.createTextInBothLanguages(dto.getDescripcion(), 2);
            transfer.setDescripcion(codigoDescripcion); // Guardar el código en idDescripcion
        }
        // Generar código de texto para comentario si se proporciona
        if (dto.getComentario() != null && !dto.getComentario().isEmpty()) {
            String codigoComentario = textosService.createTextInBothLanguages(dto.getComentario(), 2);
            transfer.setComentario(codigoComentario); // Guardar el código en idComentario
        }
        // fechaInicio es obligatorio
        if (dto.getFechaInicio() == null || dto.getFechaInicio().isEmpty()) {
            throw new IllegalArgumentException("fechaInicio is required");
        }
        transfer.setFechaInicio(parseLocalDate(dto.getFechaInicio()));
        if (dto.getFechaTermino() != null) {
            transfer.setFechaTermino(parseLocalDate(dto.getFechaTermino()));
        }
        transfer.setUrlDocumento(dto.getUrlDocumento());
        transfer.setLinkVisualizacion(dto.getLinkVisualizacion());
        transfer.setLinkPDF(dto.getLinkPDF());
        transfer.setProgressReport(dto.getProgressReport());
        transfer.setCodigoANID(dto.getCodigoANID());
        // Manejar basal: debe ser "S" o "N"
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = dto.getBasal().charAt(0);
            if (basalValue == 'S' || basalValue == 's' || basalValue == 'N' || basalValue == 'n') {
                transfer.setBasal(Character.toUpperCase(basalValue));
            } else {
                transfer.setBasal('S');
            }
        } else {
            transfer.setBasal('S');
        }
        transfer.setLineasInvestigacion(dto.getLineasInvestigacion());

        // Relaciones de ProductoCientifico
        if (dto.getTipoProducto() != null && dto.getTipoProducto().getId() != null) {
            tipoProductoRepository.findById(dto.getTipoProducto().getId())
                .ifPresent(transfer::setTipoProducto);
        }

        if (dto.getEstadoProducto() != null && dto.getEstadoProducto().getId() != null) {
            estadoProductoRepository.findById(dto.getEstadoProducto().getId())
                .ifPresent(transfer::setEstadoProducto);
        }

        // Campos específicos de TransferenciaTecnologica
        if (dto.getInstitucion() != null && dto.getInstitucion().getId() != null) {
            institucionRepository.findById(dto.getInstitucion().getId())
                .ifPresent(transfer::setInstitucion);
        }
        if (dto.getTipoTransferencia() != null && dto.getTipoTransferencia().getId() != null) {
            tipoTransferenciaRepository.findById(dto.getTipoTransferencia().getId())
                .ifPresent(transfer::setTipoTransferencia);
        }
        transfer.setCategoriaTransferencia(dto.getCategoriaTransferencia());
        transfer.setCiudad(dto.getCiudad());
        transfer.setRegion(dto.getRegion());
        transfer.setAgno(dto.getAgno());
        if (dto.getPais() != null && dto.getPais().getCodigo() != null) {
            paisRepository.findById(dto.getPais().getCodigo())
                .ifPresent(transfer::setPais);
        }

        return transfer;
    }

    /**
     * Convierte un string a LocalDate
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                // Intentar con formato ISO
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Guarda los participantes de una transferencia tecnológica
     */
    private void saveParticipantes(TransferenciaTecnologica transfer, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue; // Saltar si faltan datos requeridos
            }

            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);

            if (rrhh != null && tipoParticipacion != null) {
                // Obtener el siguiente ID correlativo para esta combinación (idProducto, idRRHH)
                Long nextId = participacionProductoRepository.getNextIdForParticipacion(
                    transfer.getId(), 
                    rrhh.getId()
                );
                
                ParticipacionProducto participacion = new ParticipacionProducto();
                participacion.setRrhh(rrhh);
                participacion.setProducto(transfer);
                participacion.setTipoParticipacion(tipoParticipacion);
                participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
                participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());

                // Establecer el ID compuesto con el correlativo
                ParticipacionProductoId id = new ParticipacionProductoId(
                    rrhh.getId(), 
                    transfer.getId(), 
                    nextId
                );
                participacion.setId(id);

                participacionProductoRepository.save(participacion);
            }
        }
    }
}


package com.sisgic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisgic.dto.*;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AgendaService {

    private static final long READY_TO_PUBLISH_VIRTUAL_ID = 4L;

    private static final Map<Long, String> ESTADO_CODES = Map.of(
        EstadoNoticia.PUBLISHED, "PUBLISHED",
        EstadoNoticia.DRAFT, "DRAFT",
        EstadoNoticia.UNPUBLISHED, "UNPUBLISHED"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private EstadoNoticiaRepository estadoNoticiaRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private UserService userService;

    @PostConstruct
    void ensureEstadoNoticiaCatalog() {
        ensureEstadoNoticiaExists(EstadoNoticia.PUBLISHED);
        ensureEstadoNoticiaExists(EstadoNoticia.DRAFT);
        ensureEstadoNoticiaExists(EstadoNoticia.UNPUBLISHED);
    }

    @Transactional(readOnly = true)
    public Page<AgendaDTO> findAll(
            Long estadoId,
            LocalDate fromDate,
            LocalDate toDate,
            String location,
            String title,
            Pageable pageable) {
        return agendaRepository.findByFilters(estadoId, fromDate, toDate, location, title, pageable)
            .map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public Optional<AgendaDTO> findById(Long id) {
        return agendaRepository.findByIdWithRelations(id).map(this::toDetailDto);
    }

    @Transactional(readOnly = true)
    public List<EstadoNoticiaDTO> listEstados() {
        return estadoNoticiaRepository.findAll().stream()
            .map(this::toEstadoDto)
            .collect(Collectors.toList());
    }

    public AgendaDTO create(AgendaDTO dto) {
        Agenda agenda = new Agenda();
        applyDtoToEntity(agenda, dto, true);
        userService.getCurrentUsername().ifPresent(agenda::setUsername);
        tipoProductoRepository.findById((long) Agenda.ID_TIPO_PRODUCTO)
            .ifPresent(agenda::setTipoProducto);
        Agenda saved = agendaRepository.save(agenda);
        return agendaRepository.findByIdWithRelations(saved.getId())
            .map(this::toDetailDto)
            .orElseThrow(() -> new IllegalStateException("Agenda event could not be loaded after create"));
    }

    public Optional<AgendaDTO> update(Long id, AgendaDTO dto) {
        return agendaRepository.findByIdWithRelations(id).map(existing -> {
            applyDtoToEntity(existing, dto, false);
            Agenda saved = agendaRepository.save(existing);
            return toDetailDto(saved);
        });
    }

    public boolean delete(Long id) {
        return agendaRepository.findById(id).map(agenda -> {
            if (agenda.getDescripcion() != null) {
                textosService.deleteLocalizedText(agenda.getDescripcion(), Agenda.ID_TIPO_TEXTO);
            }
            if (agenda.getComentario() != null) {
                textosService.deleteLocalizedText(agenda.getComentario(), Agenda.ID_TIPO_TEXTO);
            }
            agendaRepository.delete(agenda);
            return true;
        }).orElse(false);
    }

    public Optional<AgendaDTO> publish(Long id) {
        return agendaRepository.findByIdWithRelations(id).map(agenda -> {
            AgendaExtraMetadata extra = readExtraMetadata(agenda.getProgressReport());
            extra.readyToPublish = false;
            agenda.setProgressReport(writeExtraMetadata(extra));
            agenda.setEstado(ensureEstadoNoticiaExists(EstadoNoticia.PUBLISHED));
            if (agenda.getFechaInicio() == null) {
                agenda.setFechaInicio(LocalDate.now());
            }
            return toDetailDto(agendaRepository.save(agenda));
        });
    }

    public Optional<AgendaDTO> unpublish(Long id) {
        return agendaRepository.findByIdWithRelations(id).map(agenda -> {
            AgendaExtraMetadata extra = readExtraMetadata(agenda.getProgressReport());
            extra.readyToPublish = false;
            agenda.setProgressReport(writeExtraMetadata(extra));
            agenda.setEstado(ensureEstadoNoticiaExists(EstadoNoticia.UNPUBLISHED));
            return toDetailDto(agendaRepository.save(agenda));
        });
    }

    public Optional<AgendaDTO> duplicate(Long id) {
        return findById(id).map(source -> {
            AgendaDTO copy = new AgendaDTO();
            copy.setTitle(copyLocalized(source.getTitle(), " (copy)"));
            copy.setSummary(source.getSummary());
            copy.setDescription(source.getDescription());
            copy.setImage(source.getImage());
            copy.setEventDate(source.getEventDate());
            copy.setStartTime(source.getStartTime());
            copy.setEndTime(source.getEndTime());
            copy.setLocation(source.getLocation());
            copy.setEventMode(source.getEventMode());
            copy.setOnlineUrl(source.getOnlineUrl());
            copy.setFeature("N");
            copy.setCategories(source.getCategories());
            EstadoNoticiaDTO draft = new EstadoNoticiaDTO();
            draft.setId(EstadoNoticia.DRAFT);
            copy.setEstado(draft);
            return create(copy);
        });
    }

    private LocalizedTextDTO copyLocalized(LocalizedTextDTO source, String suffix) {
        LocalizedTextDTO copy = new LocalizedTextDTO();
        if (source != null) {
            copy.setEs(source.getEs() != null ? source.getEs() + suffix : null);
            copy.setUs(source.getUs());
        }
        return copy;
    }

    private void applyDtoToEntity(Agenda agenda, AgendaDTO dto, boolean isCreate) {
        if (dto.getTitle() != null && dto.getTitle().hasAnyValue()) {
            if (isCreate || agenda.getDescripcion() == null || agenda.getDescripcion().isEmpty()) {
                agenda.setDescripcion(textosService.createLocalizedText(dto.getTitle(), Agenda.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(agenda.getDescripcion(), dto.getTitle(), Agenda.ID_TIPO_TEXTO);
            }
        }

        if (dto.getDescription() != null && dto.getDescription().hasAnyValue()) {
            if (isCreate || agenda.getComentario() == null || agenda.getComentario().isEmpty()) {
                agenda.setComentario(textosService.createLocalizedText(dto.getDescription(), Agenda.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(agenda.getComentario(), dto.getDescription(), Agenda.ID_TIPO_TEXTO);
            }
        }

        AgendaExtraMetadata extra = readExtraMetadata(agenda.getProgressReport());
        applyEstado(agenda, dto, extra);

        if (dto.getImage() != null) {
            agenda.setImage(dto.getImage());
        }
        if (dto.getStartTime() != null) {
            agenda.setHora(normalizeTime(dto.getStartTime()));
        }
        if (dto.getLocation() != null) {
            agenda.setLugar(dto.getLocation());
        }
        if (dto.getEventDate() != null && !dto.getEventDate().isBlank()) {
            agenda.setFechaInicio(parseDate(dto.getEventDate()));
        } else if (isCreate && agenda.getFechaInicio() == null) {
            agenda.setFechaInicio(LocalDate.now());
        }
        if (dto.getOnlineUrl() != null) {
            agenda.setLinkVisualizacion(dto.getOnlineUrl());
        }

        if (dto.getSummary() != null) {
            extra.summary = dto.getSummary();
        }
        if (dto.getEndTime() != null) {
            extra.endTime = normalizeTime(dto.getEndTime());
        }
        if (dto.getEventMode() != null) {
            extra.eventMode = dto.getEventMode();
        }
        if (dto.getFeature() != null) {
            extra.feature = dto.getFeature();
        }
        if (dto.getCategories() != null) {
            extra.categories = dto.getCategories();
        }
        if (dto.getOrganizer() != null) {
            extra.organizer = dto.getOrganizer();
        }
        if (dto.getSpeaker() != null) {
            extra.speaker = dto.getSpeaker();
        }
        if (dto.getAudience() != null) {
            extra.audience = dto.getAudience();
        }
        if (dto.getCtaLabel() != null) {
            extra.ctaLabel = dto.getCtaLabel();
        }
        if (dto.getCtaUrl() != null) {
            extra.ctaUrl = dto.getCtaUrl();
        }
        agenda.setProgressReport(writeExtraMetadata(extra));
    }

    private void applyEstado(Agenda agenda, AgendaDTO dto, AgendaExtraMetadata extra) {
        long estadoId = EstadoNoticia.DRAFT;
        if (dto.getEstado() != null && dto.getEstado().getId() != null) {
            estadoId = dto.getEstado().getId();
        }
        if (estadoId == READY_TO_PUBLISH_VIRTUAL_ID) {
            extra.readyToPublish = true;
            estadoId = EstadoNoticia.DRAFT;
        } else {
            extra.readyToPublish = false;
        }
        agenda.setEstado(ensureEstadoNoticiaExists(estadoId));
    }

    private EstadoNoticia ensureEstadoNoticiaExists(long id) {
        return estadoNoticiaRepository.findById(id).orElseGet(() -> {
            EstadoNoticia estado = new EstadoNoticia();
            estado.setId(id);
            estado.setIdDescripcion(ESTADO_CODES.getOrDefault(id, "UNKNOWN"));
            return estadoNoticiaRepository.save(estado);
        });
    }

    private AgendaDTO toListDto(Agenda agenda) {
        AgendaDTO dto = new AgendaDTO();
        AgendaExtraMetadata extra = readExtraMetadata(agenda.getProgressReport());
        dto.setId(agenda.getId());
        dto.setTitle(textosService.getLocalizedText(agenda.getDescripcion(), Agenda.ID_TIPO_TEXTO));
        dto.setDescription(textosService.getLocalizedText(agenda.getComentario(), Agenda.ID_TIPO_TEXTO));
        dto.setEstado(toEstadoDto(agenda.getEstado(), extra));
        dto.setImage(agenda.getImage());
        dto.setEventDate(formatDate(agenda.getFechaInicio()));
        dto.setStartTime(agenda.getHora());
        dto.setLocation(agenda.getLugar());
        dto.setOnlineUrl(agenda.getLinkVisualizacion());
        dto.setCreatedAt(agenda.getCreatedAt() != null ? agenda.getCreatedAt().toString() : null);
        dto.setUpdatedAt(agenda.getUpdatedAt() != null ? agenda.getUpdatedAt().toString() : null);
        dto.setUsername(agenda.getUsername());
        if (agenda.getBasal() != null) {
            dto.setBasal(String.valueOf(agenda.getBasal()));
        }
        applyExtraMetadata(dto, extra);
        return dto;
    }

    private AgendaDTO toDetailDto(Agenda agenda) {
        return toListDto(agenda);
    }

    private void applyExtraMetadata(AgendaDTO dto, AgendaExtraMetadata extra) {
        if (extra.summary != null) {
            dto.setSummary(extra.summary);
        }
        dto.setEndTime(extra.endTime);
        dto.setEventMode(extra.eventMode);
        if (extra.feature != null) {
            dto.setFeature(extra.feature);
        }
        if (extra.categories != null) {
            dto.setCategories(extra.categories);
        }
        if (extra.organizer != null) {
            dto.setOrganizer(extra.organizer);
        }
        if (extra.speaker != null) {
            dto.setSpeaker(extra.speaker);
        }
        if (extra.audience != null) {
            dto.setAudience(extra.audience);
        }
        if (extra.ctaLabel != null) {
            dto.setCtaLabel(extra.ctaLabel);
        }
        dto.setCtaUrl(extra.ctaUrl);
    }

    private AgendaExtraMetadata readExtraMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new AgendaExtraMetadata();
        }
        try {
            return objectMapper.readValue(json, AgendaExtraMetadata.class);
        } catch (JsonProcessingException e) {
            return new AgendaExtraMetadata();
        }
    }

    private String writeExtraMetadata(AgendaExtraMetadata extra) {
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private EstadoNoticiaDTO toEstadoDto(EstadoNoticia estado, AgendaExtraMetadata extra) {
        if (estado == null) {
            return null;
        }
        EstadoNoticiaDTO dto = new EstadoNoticiaDTO();
        if (extra != null && extra.readyToPublish && estado.getId() == EstadoNoticia.DRAFT) {
            dto.setId(READY_TO_PUBLISH_VIRTUAL_ID);
            dto.setCode("READY_TO_PUBLISH");
        } else {
            dto.setId(estado.getId());
            dto.setCode(ESTADO_CODES.getOrDefault(estado.getId(), "UNKNOWN"));
        }
        dto.setLabel(dto.getCode());
        return dto;
    }

    private EstadoNoticiaDTO toEstadoDto(EstadoNoticia estado) {
        return toEstadoDto(estado, null);
    }

    private String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String t = time.trim();
        if (t.length() == 5) {
            return t + ":00";
        }
        return t;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format, expected ISO-8601 (YYYY-MM-DD): " + value);
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    /** JSON stored in producto.progressReport for agenda-specific fields. */
    static class AgendaExtraMetadata {
        public LocalizedTextDTO summary;
        public String endTime;
        public String eventMode;
        public String feature;
        public List<CategoryDTO> categories = new ArrayList<>();
        public LocalizedTextDTO organizer;
        public LocalizedTextDTO speaker;
        public LocalizedTextDTO audience;
        public LocalizedTextDTO ctaLabel;
        public String ctaUrl;
        public boolean readyToPublish;
    }
}

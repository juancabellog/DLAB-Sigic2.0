package com.sisgic.service;

import com.sisgic.dto.*;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.util.TaxonomyNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AgendaService {

    private static final long READY_TO_PUBLISH_VIRTUAL_ID = 4L;
    private static final long DEFAULT_TIPO_EVENTO_ID = 1L;

    private static final Map<Long, String> ESTADO_CODES = Map.of(
        EstadoNoticia.PUBLISHED, "PUBLISHED",
        EstadoNoticia.DRAFT, "DRAFT",
        EstadoNoticia.UNPUBLISHED, "UNPUBLISHED"
    );

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private EstadoNoticiaRepository estadoNoticiaRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private TipoEventoAgendaRepository tipoEventoAgendaRepository;

    @Autowired
    private CategoryEventRepository categoryEventRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private UserService userService;

    @PostConstruct
    void ensureCatalogs() {
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

    @Transactional(readOnly = true)
    public List<TipoEventoAgendaDTO> listEventTypes() {
        return tipoEventoAgendaRepository.findAllByOrderByIdAsc().stream()
            .map(this::toTipoEventoDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listCategories() {
        return categoryEventRepository.findAllByOrderByLabelAsc().stream()
            .map(this::toCategoryDto)
            .collect(Collectors.toList());
    }

    public CategoryDTO createCategory(CreateCategoryRequest request) {
        if (request == null || request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("Category label is required");
        }
        String label = request.getLabel().trim();

        Optional<CategoryEvent> existing = categoryEventRepository.findAll().stream()
            .filter(c -> TaxonomyNormalizer.termsMatch(c.getLabel(), label))
            .findFirst();
        if (existing.isPresent()) {
            return toCategoryDto(existing.get());
        }

        CategoryEvent category = new CategoryEvent();
        category.setId(UUID.randomUUID().toString());
        category.setLabel(label);
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedDate(now);
        category.setUpdatedDate(now);
        return toCategoryDto(categoryEventRepository.save(category));
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
            agenda.setEstado(ensureEstadoNoticiaExists(EstadoNoticia.PUBLISHED));
            if (agenda.getFechaInicio() == null) {
                agenda.setFechaInicio(LocalDate.now());
            }
            return toDetailDto(agendaRepository.save(agenda));
        });
    }

    public Optional<AgendaDTO> unpublish(Long id) {
        return agendaRepository.findByIdWithRelations(id).map(agenda -> {
            agenda.setEstado(ensureEstadoNoticiaExists(EstadoNoticia.UNPUBLISHED));
            return toDetailDto(agendaRepository.save(agenda));
        });
    }

    public Optional<AgendaDTO> duplicate(Long id) {
        return findById(id).map(source -> {
            AgendaDTO copy = new AgendaDTO();
            copy.setTitle(copyLocalized(source.getTitle(), " (copy)"));
            copy.setDescription(source.getDescription());
            copy.setImage(source.getImage());
            copy.setEventDate(source.getEventDate());
            copy.setStartTime(source.getStartTime());
            copy.setEndTime(source.getEndTime());
            copy.setLocation(source.getLocation());
            copy.setEventMode(source.getEventMode());
            copy.setOnlineUrl(source.getOnlineUrl());
            copy.setFeature("N");
            copy.setBasal(source.getBasal() != null ? source.getBasal() : "N");
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

        applyEstado(agenda, dto);

        if (dto.getImage() != null) {
            agenda.setImage(dto.getImage());
        }
        if (dto.getStartTime() != null) {
            agenda.setStartTime(normalizeTime(dto.getStartTime()));
        } else if (isCreate && agenda.getStartTime() == null) {
            agenda.setStartTime("00:00");
        }
        if (dto.getEndTime() != null) {
            agenda.setEndTime(normalizeTime(dto.getEndTime()));
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

        if (dto.getEventMode() != null) {
            agenda.setTipoEvento(resolveTipoEvento(dto.getEventMode()));
        } else if (isCreate && agenda.getTipoEvento() == null) {
            agenda.setTipoEvento(defaultTipoEvento());
        }

        if (dto.getCategories() != null) {
            Set<CategoryEvent> categories = resolveCategories(dto.getCategories());
            agenda.getCategories().clear();
            agenda.getCategories().addAll(categories);
        }

        applyFeature(agenda, dto.getFeature(), isCreate);
        applyBasal(agenda, dto.getBasal(), isCreate);
        agenda.setProgressReport(null);
    }

    private void applyEstado(Agenda agenda, AgendaDTO dto) {
        long estadoId = EstadoNoticia.DRAFT;
        if (dto.getEstado() != null && dto.getEstado().getId() != null) {
            estadoId = dto.getEstado().getId();
        }
        if (estadoId == READY_TO_PUBLISH_VIRTUAL_ID) {
            estadoId = EstadoNoticia.DRAFT;
        }
        agenda.setEstado(ensureEstadoNoticiaExists(estadoId));
    }

    private void applyFeature(Agenda agenda, String featureDto, boolean isCreate) {
        Character normalized = normalizeAgendaFeature(featureDto);
        if (normalized != null) {
            agenda.setFeature(normalized);
        } else if (isCreate && agenda.getFeature() == null) {
            agenda.setFeature('N');
        }
    }

    private void applyBasal(Agenda agenda, String basalDto, boolean isCreate) {
        Character normalized = normalizeBasal(basalDto);
        if (normalized != null) {
            agenda.setBasal(normalized);
        } else if (isCreate) {
            agenda.setBasal('N');
        }
    }

    private Character normalizeBasal(String basal) {
        if (basal == null || basal.isBlank()) {
            return null;
        }
        char value = Character.toUpperCase(basal.charAt(0));
        if (value == 'S' || value == 'N') {
            return value;
        }
        if (value == '1') {
            return 'S';
        }
        if (value == '0') {
            return 'N';
        }
        return null;
    }

    private EstadoNoticia ensureEstadoNoticiaExists(long id) {
        return estadoNoticiaRepository.findById(id).orElseGet(() -> {
            EstadoNoticia estado = new EstadoNoticia();
            estado.setId(id);
            estado.setIdDescripcion(ESTADO_CODES.getOrDefault(id, "UNKNOWN"));
            return estadoNoticiaRepository.save(estado);
        });
    }

    private TipoEventoAgenda defaultTipoEvento() {
        return tipoEventoAgendaRepository.findById(DEFAULT_TIPO_EVENTO_ID)
            .orElseThrow(() -> new IllegalStateException(
                "TipoEventoAgenda id=" + DEFAULT_TIPO_EVENTO_ID + " not found in database"));
    }

    private TipoEventoAgenda resolveTipoEvento(String eventMode) {
        if (eventMode == null || eventMode.isBlank()) {
            return defaultTipoEvento();
        }
        String normalized = eventMode.trim().toLowerCase(Locale.ROOT);
        return tipoEventoAgendaRepository.findByIdDescripcionIgnoreCase(normalized)
            .orElseGet(() -> tipoEventoAgendaRepository.findAll().stream()
                .filter(t -> t.getIdDescripcion() != null
                    && normalized.equals(t.getIdDescripcion().trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(defaultTipoEvento()));
    }

    private Set<CategoryEvent> resolveCategories(List<CategoryDTO> categoryDtos) {
        if (categoryDtos == null || categoryDtos.isEmpty()) {
            return Set.of();
        }
        Set<String> ids = categoryDtos.stream()
            .map(CategoryDTO::getId)
            .filter(Objects::nonNull)
            .filter(id -> !id.isBlank())
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Set.of();
        }
        List<CategoryEvent> found = categoryEventRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException("One or more agenda categories were not found");
        }
        return new HashSet<>(found);
    }

    private AgendaDTO toListDto(Agenda agenda) {
        AgendaDTO dto = new AgendaDTO();
        dto.setId(agenda.getId());
        dto.setTitle(textosService.getLocalizedText(agenda.getDescripcion(), Agenda.ID_TIPO_TEXTO));
        dto.setDescription(textosService.getLocalizedText(agenda.getComentario(), Agenda.ID_TIPO_TEXTO));
        dto.setEstado(toEstadoDto(agenda.getEstado()));
        dto.setImage(agenda.getImage());
        dto.setEventDate(formatDate(agenda.getFechaInicio()));
        dto.setStartTime(agenda.getStartTime());
        dto.setEndTime(agenda.getEndTime());
        dto.setLocation(agenda.getLugar());
        dto.setOnlineUrl(agenda.getLinkVisualizacion());
        dto.setEventMode(eventModeFromEntity(agenda.getTipoEvento()));
        dto.setFeature(featureToDto(agenda.getFeature()));
        if (agenda.getCategories() != null && !agenda.getCategories().isEmpty()) {
            dto.setCategories(agenda.getCategories().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList()));
        }
        dto.setCreatedAt(agenda.getCreatedAt() != null ? agenda.getCreatedAt().toString() : null);
        dto.setUpdatedAt(agenda.getUpdatedAt() != null ? agenda.getUpdatedAt().toString() : null);
        dto.setUsername(agenda.getUsername());
        if (agenda.getBasal() != null) {
            dto.setBasal(String.valueOf(agenda.getBasal()));
        }
        return dto;
    }

    private AgendaDTO toDetailDto(Agenda agenda) {
        return toListDto(agenda);
    }

    private EstadoNoticiaDTO toEstadoDto(EstadoNoticia estado) {
        if (estado == null) {
            return null;
        }
        EstadoNoticiaDTO dto = new EstadoNoticiaDTO();
        dto.setId(estado.getId());
        dto.setCode(ESTADO_CODES.getOrDefault(estado.getId(), "UNKNOWN"));
        dto.setLabel(dto.getCode());
        return dto;
    }

    private TipoEventoAgendaDTO toTipoEventoDto(TipoEventoAgenda tipo) {
        TipoEventoAgendaDTO dto = new TipoEventoAgendaDTO();
        dto.setId(tipo.getId());
        dto.setCode(tipo.getIdDescripcion());
        dto.setLabel(formatEventModeLabel(tipo.getIdDescripcion()));
        return dto;
    }

    private CategoryDTO toCategoryDto(CategoryEvent category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setLabel(category.getLabel());
        return dto;
    }

    private String eventModeFromEntity(TipoEventoAgenda tipo) {
        if (tipo == null || tipo.getIdDescripcion() == null) {
            return null;
        }
        return tipo.getIdDescripcion().trim().toLowerCase(Locale.ROOT);
    }

    private String formatEventModeLabel(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "in_person" -> "In person";
            case "online" -> "Online";
            case "hybrid" -> "Hybrid";
            default -> code;
        };
    }

    private Character normalizeAgendaFeature(String feature) {
        if (feature == null || feature.isBlank()) {
            return null;
        }
        char value = Character.toUpperCase(feature.charAt(0));
        if (value == 'S' || value == 'Y' || value == '1') {
            return 'S';
        }
        if (value == 'N' || value == '0') {
            return 'N';
        }
        return null;
    }

    private String featureToDto(Character feature) {
        if (feature == null) {
            return "N";
        }
        return String.valueOf(Character.toUpperCase(feature));
    }

    /** Normalizes to HH:mm for agenda.startTime / agenda.endTime (CHAR(5)). */
    private String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String t = time.trim();
        if (t.length() >= 5) {
            return t.substring(0, 5);
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
}

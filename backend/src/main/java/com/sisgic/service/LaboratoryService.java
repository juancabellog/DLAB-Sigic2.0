package com.sisgic.service;

import com.sisgic.dto.*;
import com.sisgic.entity.*;
import com.sisgic.repository.LaboratorioRepository;
import com.sisgic.repository.LaboratorioRrhhRepository;
import com.sisgic.repository.RRHHRepository;
import com.sisgic.repository.VClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LaboratoryService {

    private static final Set<String> VALID_STATUSES = Set.of("active", "inactive");
    private static final String MEMBERSHIP_DIRECTOR = "director";
    private static final String MEMBERSHIP_LAB_MANAGER = "lab_manager";
    private static final String MEMBERSHIP_MEMBER = "member";

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Autowired
    private LaboratorioRrhhRepository laboratorioRrhhRepository;

    @Autowired
    private RRHHRepository rrhhRepository;

    @Autowired
    private VClusterRepository vClusterRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private GeminiTranslationService geminiTranslationService;

    @Autowired
    private RrhhTipoHistoryService rrhhTipoHistoryService;

    @Transactional(readOnly = true)
    public Page<LaboratoryDTO> findAll(
            String status,
            Integer clusterId,
            Long directorId,
            String search,
            Boolean hasActiveMembers,
            Pageable pageable) {

        String activo = mapStatusToActivo(status);
        Long idArea = clusterId != null ? clusterId.longValue() : null;

        List<LaboratoryDTO> dtos = laboratorioRepository
            .findFiltered(Laboratorio.CODIGO_CENTRO, activo, idArea)
            .stream()
            .map(this::toListDto)
            .filter(dto -> directorId == null || directorId.equals(dto.getDirectorId()))
            .filter(dto -> matchesSearch(dto, search))
            .collect(Collectors.toList());

        if (hasActiveMembers != null) {
            dtos = dtos.stream()
                .filter(dto -> {
                    int count = dto.getActiveMemberCount() != null ? dto.getActiveMemberCount() : 0;
                    return hasActiveMembers ? count > 0 : count == 0;
                })
                .collect(Collectors.toList());
        }

        sortDtos(dtos, pageable);
        return paginate(dtos, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<LaboratoryDTO> findById(Long id, Integer clusterId) {
        return resolveLaboratorio(id, clusterId).map(this::toDetailDto);
    }

    public LaboratoryDTO create(LaboratoryDTO dto) {
        validateLaboratory(dto, true);
        if (dto.getClusterId() == null) {
            throw new IllegalArgumentException("Cluster is required");
        }

        Long nextId = laboratorioRepository.findMaxIdByCodigoCentro(Laboratorio.CODIGO_CENTRO) + 1;
        Laboratorio lab = new Laboratorio();
        lab.setCodigoCentro(Laboratorio.CODIGO_CENTRO);
        lab.setId(nextId);
        lab.setIdArea(dto.getClusterId().longValue());
        lab.setActivo(mapStatusToActivo(dto.getStatus() != null ? dto.getStatus() : "active"));

        persistTexts(lab, dto, true);
        lab.setUrlImagen(dto.getImageUrl());
        lab = laboratorioRepository.save(lab);

        if (dto.getDirectorId() != null) {
            assignDirectorInternal(lab, dto.getDirectorId(), LocalDate.now(), contactFromDto(dto));
        }

        return toDetailDto(lab);
    }

    public Optional<LaboratoryDTO> update(Long id, Integer clusterId, LaboratoryDTO dto) {
        validateLaboratory(dto, false);
        return resolveLaboratorio(id, clusterId).map(lab -> {
            persistTexts(lab, dto, false);
            if (dto.getStatus() != null) {
                lab.setActivo(mapStatusToActivo(dto.getStatus()));
            }
            if (dto.getImageUrl() != null) {
                lab.setUrlImagen(dto.getImageUrl());
            }
            laboratorioRepository.save(lab);
            return toDetailDto(lab);
        });
    }

    public Optional<LaboratoryDTO> activate(Long id, Integer clusterId) {
        return setStatus(id, clusterId, "active");
    }

    public Optional<LaboratoryDTO> deactivate(Long id, Integer clusterId) {
        return setStatus(id, clusterId, "inactive");
    }

    @Transactional
    public boolean delete(Long id, Integer clusterId) {
        Optional<Laboratorio> optional = resolveLaboratorio(id, clusterId);
        if (optional.isEmpty()) {
            return false;
        }
        Laboratorio lab = optional.get();
        // Remove memberships (director, manager, members) before deleting the laboratory.
        laboratorioRrhhRepository.deleteByCodigoCentroAndIdAreaAndIdLaboratorio(
            lab.getCodigoCentro(), lab.getIdArea(), lab.getId());
        laboratorioRepository.delete(lab);
        return true;
    }

    public TranslateLaboratoryResponse translate(TranslateLaboratoryRequest request) {
        try {
            String direction = request.getDirection() != null
                ? request.getDirection().trim().toLowerCase()
                : "es_to_en";

            if ("en_to_es".equals(direction)) {
                if (request.getNameEn() == null || request.getNameEn().isBlank()) {
                    throw new IllegalArgumentException("English name is required for translation to Spanish");
                }
                GeminiTranslationService.TranslationResult result = geminiTranslationService.translate(
                    request.getNameEn(),
                    null,
                    request.getDescriptionEn(),
                    GeminiTranslationService.TranslationDirection.EN_TO_ES);
                return TranslateLaboratoryResponse.fromSpanish(result.titleEn(), result.bodyEn());
            }

            if (request.getNameEs() == null || request.getNameEs().isBlank()) {
                throw new IllegalArgumentException("Spanish name is required for translation to English");
            }
            GeminiTranslationService.TranslationResult result = geminiTranslationService.translate(
                request.getNameEs(),
                null,
                request.getDescriptionEs(),
                GeminiTranslationService.TranslationDirection.ES_TO_EN);
            return TranslateLaboratoryResponse.fromEnglish(result.titleEn(), result.bodyEn());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Translation failed: " + e.getMessage(), e);
        }
    }

    public Optional<LaboratoryDTO> validateTranslation(Long id, Integer clusterId) {
        return findById(id, clusterId).map(dto -> {
            dto.setTranslationStatus("validated");
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<LaboratoryMembershipDTO> getMemberships(Long id, Integer clusterId, String membershipType) {
        return resolveLaboratorio(id, clusterId)
            .map(lab -> loadMembershipDtos(lab, membershipType))
            .orElse(List.of());
    }

    public LaboratoryMembershipDTO addMembership(Long id, Integer clusterId, LaboratoryMembershipDTO dto) {
        Laboratorio lab = resolveLaboratorio(id, clusterId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found"));
        validateMembership(dto, true);

        String tipo = mapMembershipTypeToTipo(dto.getMembershipType());
        if (LaboratorioRrhh.TIPO_LAB_MANAGER.equals(tipo) && dto.getEndDate() == null) {
            ensureNoActiveByTipo(lab, tipo);
        }
        if (LaboratorioRrhh.TIPO_MEMBER.equals(tipo) && dto.getEndDate() == null) {
            ensureNoDuplicateActiveMember(lab, dto.getPersonId());
        }
        if (LaboratorioRrhh.TIPO_DIRECTOR.equals(tipo) && dto.getEndDate() == null) {
            ensureNoActiveByTipo(lab, tipo);
            endActiveByTipo(lab, tipo, dto.getStartDate());
        }

        RRHH person = rrhhRepository.findById(dto.getPersonId())
            .orElseThrow(() -> new IllegalArgumentException("Person not found"));
        applyPersonUpdates(person, dto);

        LaboratorioRrhh row = new LaboratorioRrhh();
        row.setCodigoCentro(lab.getCodigoCentro());
        row.setIdArea(lab.getIdArea());
        row.setIdLaboratorio(lab.getId());
        row.setIdRRHH(dto.getPersonId());
        row.setFechaInicio(dto.getStartDate());
        row.setTipoRecurso(tipo);
        row.setFechaTermino(dto.getEndDate());
        row.setRrhh(person);

        return toMembershipDto(laboratorioRrhhRepository.save(row));
    }

    public Optional<LaboratoryMembershipDTO> updateMembership(
            Long id, Integer clusterId, String membershipKey, LaboratoryMembershipDTO dto) {
        return resolveLaboratorio(id, clusterId).flatMap(lab ->
            findMembership(lab, membershipKey).map(row -> {
                validateMembership(dto, false);
                applyPersonUpdates(row.getRrhh(), dto);

                if (dto.getEndDate() != null) {
                    row.setFechaTermino(dto.getEndDate());
                } else if (dto.getStatus() != null && "active".equals(dto.getStatus())) {
                    String tipo = row.getTipoRecurso();
                    if (LaboratorioRrhh.TIPO_LAB_MANAGER.equals(tipo)) {
                        ensureNoActiveByTipo(lab, tipo, row.getLaboratorioRrhhId());
                    }
                    if (LaboratorioRrhh.TIPO_DIRECTOR.equals(tipo)) {
                        ensureNoActiveByTipo(lab, tipo, row.getLaboratorioRrhhId());
                    }
                    row.setFechaTermino(null);
                }

                if (dto.getStartDate() != null && !dto.getStartDate().equals(row.getFechaInicio())) {
                    throw new IllegalArgumentException(
                        "Start date cannot be changed for an existing membership. End and create a new record instead.");
                }

                row.setRrhh(rrhhRepository.findById(row.getIdRRHH()).orElse(row.getRrhh()));
                return toMembershipDto(laboratorioRrhhRepository.save(row));
            })
        );
    }

    public Optional<LaboratoryMembershipDTO> endMembership(
            Long id, Integer clusterId, String membershipKey, LocalDate endDate) {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        final LocalDate effectiveEndDate = endDate;
        return resolveLaboratorio(id, clusterId).flatMap(lab ->
            findMembership(lab, membershipKey).map(row -> {
                if (effectiveEndDate.isBefore(row.getFechaInicio())) {
                    throw new IllegalArgumentException("End date must be on or after start date");
                }
                row.setFechaTermino(effectiveEndDate);
                return toMembershipDto(laboratorioRrhhRepository.save(row));
            })
        );
    }

    public boolean deleteMembershipPermanently(Long id, Integer clusterId, String membershipKey) {
        return resolveLaboratorio(id, clusterId).flatMap(lab ->
            findMembership(lab, membershipKey).map(row -> {
                if (row.isActive()) {
                    throw new IllegalArgumentException("Only ended memberships can be permanently deleted");
                }
                laboratorioRrhhRepository.delete(row);
                return true;
            })
        ).orElse(false);
    }

    public Optional<LaboratoryDTO> assignDirector(Long id, Integer clusterId, AssignLabDirectorRequest request) {
        if (request.getPersonId() == null) {
            throw new IllegalArgumentException("Person is required");
        }
        return resolveLaboratorio(id, clusterId).map(lab -> {
            endActiveByTipo(lab, LaboratorioRrhh.TIPO_DIRECTOR, LocalDate.now());
            assignDirectorInternal(lab, request.getPersonId(), LocalDate.now(), contactFromRequest(request));
            return toDetailDto(lab);
        });
    }

    public Optional<LaboratoryDTO> clearDirector(Long id, Integer clusterId) {
        return resolveLaboratorio(id, clusterId).map(lab -> {
            endActiveByTipo(lab, LaboratorioRrhh.TIPO_DIRECTOR, LocalDate.now());
            return toDetailDto(lab);
        });
    }

    public Optional<LaboratoryDTO> updateDirectorContact(Long id, Integer clusterId, AssignLabDirectorRequest request) {
        return resolveLaboratorio(id, clusterId).map(lab -> {
            LaboratorioRrhh director = findActiveByTipo(lab, LaboratorioRrhh.TIPO_DIRECTOR)
                .orElseThrow(() -> new IllegalArgumentException("No director assigned"));
            applyPersonContactUpdates(director.getRrhh(), contactFromRequest(request));
            return toDetailDto(lab);
        });
    }

    public LaboratoryMembershipDTO changeLabManager(Long id, Integer clusterId, ChangeLabManagerRequest request) {
        if (request.getPersonId() == null) {
            throw new IllegalArgumentException("Person is required");
        }
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate endDateForCurrent = request.getEndDateForCurrent() != null
            ? request.getEndDateForCurrent() : LocalDate.now();

        Laboratorio lab = resolveLaboratorio(id, clusterId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found"));
        endActiveByTipo(lab, LaboratorioRrhh.TIPO_LAB_MANAGER, endDateForCurrent);

        LaboratoryMembershipDTO dto = new LaboratoryMembershipDTO();
        dto.setPersonId(request.getPersonId());
        dto.setMembershipType(MEMBERSHIP_LAB_MANAGER);
        dto.setResourceType(
            request.getResourceType() != null && !request.getResourceType().isBlank()
                ? request.getResourceType()
                : resolveResourceTypeFromPerson(request.getPersonId())
        );
        dto.setStartDate(startDate);
        dto.setEmail(request.getEmail());
        dto.setOrcid(request.getOrcid());
        dto.setMobilePhone(request.getMobilePhone());
        return addMembership(id, clusterId, dto);
    }

    public Optional<LaboratoryMembershipDTO> restoreMembership(Long id, Integer clusterId, String membershipKey) {
        return resolveLaboratorio(id, clusterId).flatMap(lab ->
            findMembership(lab, membershipKey).map(row -> {
                if (LaboratorioRrhh.TIPO_LAB_MANAGER.equals(row.getTipoRecurso())) {
                    ensureNoActiveByTipo(lab, row.getTipoRecurso(), row.getLaboratorioRrhhId());
                }
                if (LaboratorioRrhh.TIPO_DIRECTOR.equals(row.getTipoRecurso())) {
                    ensureNoActiveByTipo(lab, row.getTipoRecurso(), row.getLaboratorioRrhhId());
                }
                if (LaboratorioRrhh.TIPO_MEMBER.equals(row.getTipoRecurso())) {
                    ensureNoDuplicateActiveMember(lab, row.getIdRRHH(), row.getLaboratorioRrhhId());
                }
                row.setFechaTermino(null);
                return toMembershipDto(laboratorioRrhhRepository.save(row));
            })
        );
    }

    // —— Internal helpers ——

    private void assignDirectorInternal(Laboratorio lab, Long personId, LocalDate startDate,
                                        LaboratoryMembershipDTO contact) {
        RRHH person = rrhhRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("Person not found"));
        applyPersonUpdates(person, contact);

        LaboratorioRrhh row = new LaboratorioRrhh();
        row.setCodigoCentro(lab.getCodigoCentro());
        row.setIdArea(lab.getIdArea());
        row.setIdLaboratorio(lab.getId());
        row.setIdRRHH(personId);
        row.setFechaInicio(startDate);
        row.setTipoRecurso(LaboratorioRrhh.TIPO_DIRECTOR);
        row.setRrhh(person);
        laboratorioRrhhRepository.save(row);
    }

    private void endActiveByTipo(Laboratorio lab, String tipo, LocalDate endDate) {
        findActiveByTipo(lab, tipo).ifPresent(row -> {
            if (!endDate.isBefore(row.getFechaInicio())) {
                row.setFechaTermino(endDate);
                laboratorioRrhhRepository.save(row);
            }
        });
    }

    private Optional<LaboratorioRrhh> findActiveByTipo(Laboratorio lab, String tipo) {
        return laboratorioRrhhRepository
            .findByLaboratorioAndTipo(lab.getCodigoCentro(), lab.getIdArea(), lab.getId(), tipo)
            .stream()
            .filter(LaboratorioRrhh::isActive)
            .findFirst();
    }

    private void ensureNoActiveByTipo(Laboratorio lab, String tipo) {
        ensureNoActiveByTipo(lab, tipo, null);
    }

    private void ensureNoActiveByTipo(Laboratorio lab, String tipo, LaboratorioRrhhId exclude) {
        boolean hasOther = laboratorioRrhhRepository
            .findByLaboratorioAndTipo(lab.getCodigoCentro(), lab.getIdArea(), lab.getId(), tipo)
            .stream()
            .filter(LaboratorioRrhh::isActive)
            .anyMatch(row -> exclude == null || !exclude.equals(row.getLaboratorioRrhhId()));
        if (hasOther) {
            String label = LaboratorioRrhh.TIPO_DIRECTOR.equals(tipo) ? "director" : "lab manager";
            throw new IllegalArgumentException("Laboratory already has a current " + label);
        }
    }

    private void ensureNoDuplicateActiveMember(Laboratorio lab, Long personId) {
        ensureNoDuplicateActiveMember(lab, personId, null);
    }

    private void ensureNoDuplicateActiveMember(Laboratorio lab, Long personId, LaboratorioRrhhId exclude) {
        boolean hasActive = laboratorioRrhhRepository
            .findByLaboratorioAndTipo(lab.getCodigoCentro(), lab.getIdArea(), lab.getId(), LaboratorioRrhh.TIPO_MEMBER)
            .stream()
            .filter(row -> personId.equals(row.getIdRRHH()))
            .filter(LaboratorioRrhh::isActive)
            .anyMatch(row -> exclude == null || !exclude.equals(row.getLaboratorioRrhhId()));
        if (hasActive) {
            throw new IllegalArgumentException("Person already has an active membership in this laboratory");
        }
    }

    private Optional<Laboratorio> resolveLaboratorio(Long id, Integer clusterId) {
        if (id == null) {
            return Optional.empty();
        }
        if (clusterId != null) {
            return laboratorioRepository.findByCodigoCentroAndIdAndIdArea(
                Laboratorio.CODIGO_CENTRO, id, clusterId.longValue());
        }
        List<Laboratorio> matches = laboratorioRepository.findByCodigoCentroAndId(Laboratorio.CODIGO_CENTRO, id);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("clusterId is required because multiple laboratories share this id");
        }
        return Optional.of(matches.get(0));
    }

    private Optional<LaboratorioRrhh> findMembership(Laboratorio lab, String membershipKey) {
        ParsedMembershipKey parsed = parseMembershipKey(membershipKey);
        LaboratorioRrhhId pk = new LaboratorioRrhhId(
            lab.getCodigoCentro(), lab.getIdArea(), lab.getId(),
            parsed.personId(), parsed.startDate());
        return laboratorioRrhhRepository.findById(pk)
            .filter(row -> parsed.tipoRecurso().equals(row.getTipoRecurso()));
    }

    private List<LaboratoryMembershipDTO> loadMembershipDtos(Laboratorio lab, String membershipType) {
        String tipoFilter = membershipType != null && !membershipType.isBlank()
            ? mapMembershipTypeToTipo(membershipType) : null;
        return laboratorioRrhhRepository
            .findByLaboratorio(lab.getCodigoCentro(), lab.getIdArea(), lab.getId())
            .stream()
            .filter(row -> tipoFilter == null || tipoFilter.equals(row.getTipoRecurso()))
            .map(this::toMembershipDto)
            .collect(Collectors.toList());
    }

    private void persistTexts(Laboratorio lab, LaboratoryDTO dto, boolean isCreate) {
        LocalizedTextDTO name = new LocalizedTextDTO();
        name.setEs(dto.getNameEs());
        name.setUs(dto.getNameEn());

        LocalizedTextDTO description = new LocalizedTextDTO();
        description.setEs(dto.getDescriptionEs());
        description.setUs(dto.getDescriptionEn());

        if (isCreate || lab.getIdDescripcion() == null || lab.getIdDescripcion().isBlank()) {
            lab.setIdDescripcion(textosService.createLocalizedText(name, Laboratorio.ID_TIPO_TEXTO));
        } else {
            textosService.updateLocalizedText(lab.getIdDescripcion(), name, Laboratorio.ID_TIPO_TEXTO);
        }

        if (description.hasAnyValue()) {
            if (isCreate || lab.getIdComentario() == null || lab.getIdComentario().isBlank()) {
                lab.setIdComentario(textosService.createLocalizedText(description, Laboratorio.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(lab.getIdComentario(), description, Laboratorio.ID_TIPO_TEXTO);
            }
        }
    }

    private LaboratoryMembershipDTO contactFromDto(LaboratoryDTO dto) {
        LaboratoryMembershipDTO contact = new LaboratoryMembershipDTO();
        contact.setEmail(dto.getDirectorEmail());
        contact.setOrcid(dto.getDirectorOrcid());
        contact.setMobilePhone(dto.getDirectorMobilePhone());
        return contact;
    }

    private LaboratoryMembershipDTO contactFromRequest(AssignLabDirectorRequest request) {
        LaboratoryMembershipDTO contact = new LaboratoryMembershipDTO();
        contact.setEmail(request.getEmail());
        contact.setOrcid(request.getOrcid());
        contact.setMobilePhone(request.getMobilePhone());
        return contact;
    }

    private void applyPersonUpdates(RRHH person, LaboratoryMembershipDTO dto) {
        applyPersonContactUpdates(person, dto);
        if (dto != null) {
            applyPersonTipoRrhh(person, dto.getResourceType());
        }
    }

    private void applyPersonContactUpdates(RRHH person, LaboratoryMembershipDTO dto) {
        if (dto == null || person == null) return;
        if (dto.getEmail() != null) {
            person.setEmail(dto.getEmail().isBlank() ? null : dto.getEmail().trim());
        }
        if (dto.getOrcid() != null) {
            person.setOrcid(dto.getOrcid().isBlank() ? null : dto.getOrcid().trim());
        }
        if (dto.getMobilePhone() != null) {
            person.setNumCelular(dto.getMobilePhone().isBlank() ? null : dto.getMobilePhone().trim());
        }
        rrhhRepository.save(person);
    }

    private void applyPersonTipoRrhh(RRHH person, String resourceType) {
        if (person == null || person.getId() == null || resourceType == null || resourceType.isBlank()) {
            return;
        }
        try {
            Long tipoId = Long.parseLong(resourceType.trim());
            rrhhTipoHistoryService.changeTipoRrhh(person.getId(), tipoId);
            RRHH refreshed = rrhhRepository.findById(person.getId()).orElse(person);
            person.setTipoRRHH(refreshed.getTipoRRHH());
        } catch (NumberFormatException ignored) {
            // Keep existing tipo when resourceType is not a numeric id.
        }
    }

    private Optional<LaboratoryDTO> setStatus(Long id, Integer clusterId, String status) {
        return resolveLaboratorio(id, clusterId).map(lab -> {
            lab.setActivo(mapStatusToActivo(status));
            return toListDto(laboratorioRepository.save(lab));
        });
    }

    private void validateLaboratory(LaboratoryDTO dto, boolean isCreate) {
        if (dto.getNameEs() == null || dto.getNameEs().isBlank()) {
            throw new IllegalArgumentException("Spanish name is required");
        }
        if (isCreate && dto.getClusterId() == null) {
            throw new IllegalArgumentException("Cluster is required");
        }
        if (dto.getStatus() != null && !VALID_STATUSES.contains(dto.getStatus())) {
            throw new IllegalArgumentException("Invalid status");
        }
    }

    private void validateMembership(LaboratoryMembershipDTO dto, boolean isCreate) {
        if (isCreate && dto.getPersonId() == null) {
            throw new IllegalArgumentException("Person is required");
        }
        if (isCreate && (dto.getMembershipType() == null || mapMembershipTypeToTipo(dto.getMembershipType()) == null)) {
            throw new IllegalArgumentException("Invalid membership type");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    private LaboratoryDTO toListDto(Laboratorio lab) {
        LaboratoryDTO dto = toBaseDto(lab);
        dto.setActiveMemberCount((int) laboratorioRrhhRepository.countActiveMembers(
            lab.getCodigoCentro(), lab.getIdArea(), lab.getId()));

        findActiveByTipo(lab, LaboratorioRrhh.TIPO_LAB_MANAGER)
            .ifPresent(m -> dto.setLabManagerName(m.getRrhh().getFullname()));

        return dto;
    }

    private LaboratoryDTO toDetailDto(Laboratorio lab) {
        LaboratoryDTO dto = toListDto(lab);
        dto.setMemberships(loadMembershipDtos(lab, null));
        return dto;
    }

    private LaboratoryDTO toBaseDto(Laboratorio lab) {
        LaboratoryDTO dto = new LaboratoryDTO();
        dto.setId(lab.getId());
        dto.setClusterId(lab.getIdArea().intValue());
        dto.setClusterLabel(resolveClusterLabel(lab.getIdArea()));
        dto.setStatus(mapActivoToStatus(lab.getActivo()));
        dto.setImageUrl(lab.getUrlImagen());

        LocalizedTextDTO name = textosService.getLocalizedText(lab.getIdDescripcion(), Laboratorio.ID_TIPO_TEXTO);
        dto.setNameEs(name.getEs());
        dto.setNameEn(name.getUs());

        if (lab.getIdComentario() != null) {
            LocalizedTextDTO description = textosService.getLocalizedText(
                lab.getIdComentario(), Laboratorio.ID_TIPO_TEXTO);
            dto.setDescriptionEs(description.getEs());
            dto.setDescriptionEn(description.getUs());
        }

        dto.setTranslationStatus(resolveTranslationStatus(name, lab.getIdComentario()));

        findActiveByTipo(lab, LaboratorioRrhh.TIPO_DIRECTOR).ifPresent(director -> {
            RRHH person = director.getRrhh();
            dto.setDirectorId(person.getId());
            dto.setDirectorName(person.getFullname());
            dto.setDirectorEmail(person.getEmail());
            dto.setDirectorOrcid(person.getOrcid());
            dto.setDirectorMobilePhone(person.getNumCelular());
            dto.setDirectorIniciales(person.getIniciales());
            dto.setDirectorProfileImageUrl(person.getUrlImagen());
            dto.setDirectorResourceType(resolveResourceTypeFromPerson(person));
            dto.setDirectorResourceTypeLabel(resolveResourceTypeLabel(person));
        });

        return dto;
    }

    private LaboratoryMembershipDTO toMembershipDto(LaboratorioRrhh row) {
        RRHH person = row.getRrhh();
        LaboratoryMembershipDTO dto = new LaboratoryMembershipDTO();
        dto.setId(buildMembershipKey(row));
        dto.setLaboratoryId(row.getIdLaboratorio());
        dto.setPersonId(row.getIdRRHH());
        dto.setMembershipType(mapTipoToMembershipType(row.getTipoRecurso()));
        dto.setResourceType(resolveResourceTypeFromPerson(person));
        dto.setResourceTypeLabel(resolveResourceTypeLabel(person));
        dto.setStartDate(row.getFechaInicio());
        dto.setEndDate(row.getFechaTermino());
        dto.setStatus(row.isActive() ? "active" : "ended");
        if (person != null) {
            dto.setPersonName(person.getFullname());
            dto.setPersonEmail(person.getEmail());
            dto.setEmail(person.getEmail());
            dto.setOrcid(person.getOrcid());
            dto.setMobilePhone(person.getNumCelular());
            dto.setPersonIniciales(person.getIniciales());
            dto.setProfileImageUrl(person.getUrlImagen());
        }
        return dto;
    }

    private String resolveResourceTypeFromPerson(Long personId) {
        return rrhhRepository.findById(personId)
            .map(this::resolveResourceTypeFromPerson)
            .orElse("");
    }

    private String resolveResourceTypeFromPerson(RRHH person) {
        if (person == null || person.getTipoRRHH() == null || person.getTipoRRHH().getId() == null) {
            return "";
        }
        return String.valueOf(person.getTipoRRHH().getId());
    }

    private String resolveResourceTypeLabel(RRHH person) {
        if (person == null || person.getTipoRRHH() == null) {
            return null;
        }
        if (person.getTipoRRHH().getDescripcion() != null && !person.getTipoRRHH().getDescripcion().isBlank()) {
            return person.getTipoRRHH().getDescripcion().trim();
        }
        if (person.getTipoRRHH().getCodigoDescripcion() != null && !person.getTipoRRHH().getCodigoDescripcion().isBlank()) {
            return person.getTipoRRHH().getCodigoDescripcion().trim();
        }
        return null;
    }

    private String resolveTranslationStatus(LocalizedTextDTO name, String idComentario) {
        boolean hasEnName = name.getUs() != null && !name.getUs().isBlank();
        boolean hasEsName = name.getEs() != null && !name.getEs().isBlank();
        if (!hasEnName || !hasEsName) {
            return "no_translation";
        }
        if (idComentario == null) {
            return "auto_generated";
        }
        LocalizedTextDTO description = textosService.getLocalizedText(idComentario, Laboratorio.ID_TIPO_TEXTO);
        boolean hasEnDesc = description.getUs() != null && !description.getUs().isBlank();
        return hasEnDesc ? "manually_edited" : "auto_generated";
    }

    private String mapStatusToActivo(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        return "active".equalsIgnoreCase(status) ? "S" : "N";
    }

    private String mapActivoToStatus(String activo) {
        return "S".equalsIgnoreCase(activo) ? "active" : "inactive";
    }

    private String mapMembershipTypeToTipo(String membershipType) {
        if (membershipType == null) return null;
        return switch (membershipType) {
            case MEMBERSHIP_DIRECTOR -> LaboratorioRrhh.TIPO_DIRECTOR;
            case MEMBERSHIP_LAB_MANAGER -> LaboratorioRrhh.TIPO_LAB_MANAGER;
            case MEMBERSHIP_MEMBER -> LaboratorioRrhh.TIPO_MEMBER;
            default -> null;
        };
    }

    private String mapTipoToMembershipType(String tipo) {
        return switch (tipo) {
            case LaboratorioRrhh.TIPO_DIRECTOR -> MEMBERSHIP_DIRECTOR;
            case LaboratorioRrhh.TIPO_LAB_MANAGER -> MEMBERSHIP_LAB_MANAGER;
            case LaboratorioRrhh.TIPO_MEMBER -> MEMBERSHIP_MEMBER;
            default -> MEMBERSHIP_MEMBER;
        };
    }

    public static String buildMembershipKey(LaboratorioRrhh row) {
        return row.getIdRRHH() + "|" + row.getFechaInicio() + "|" + mapTipoToMembershipTypeStatic(row.getTipoRecurso());
    }

    private static String mapTipoToMembershipTypeStatic(String tipo) {
        return switch (tipo) {
            case LaboratorioRrhh.TIPO_DIRECTOR -> MEMBERSHIP_DIRECTOR;
            case LaboratorioRrhh.TIPO_LAB_MANAGER -> MEMBERSHIP_LAB_MANAGER;
            default -> MEMBERSHIP_MEMBER;
        };
    }

    private ParsedMembershipKey parseMembershipKey(String membershipKey) {
        String[] parts = membershipKey.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid membership key");
        }
        return new ParsedMembershipKey(
            Long.parseLong(parts[0]),
            LocalDate.parse(parts[1]),
            mapMembershipTypeToTipo(parts[2])
        );
    }

    private boolean matchesSearch(LaboratoryDTO dto, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.toLowerCase(Locale.ROOT);
        return contains(dto.getNameEs(), q) || contains(dto.getNameEn(), q);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void sortDtos(List<LaboratoryDTO> dtos, Pageable pageable) {
        Comparator<LaboratoryDTO> comparator = Comparator.comparing(
            dto -> Optional.ofNullable(dto.getNameEs()).orElse(""),
            String.CASE_INSENSITIVE_ORDER);
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            comparator = switch (property) {
                case "nameEn" -> Comparator.comparing(
                    dto -> Optional.ofNullable(dto.getNameEn()).orElse(""), String.CASE_INSENSITIVE_ORDER);
                case "clusterId" -> Comparator.comparing(
                    dto -> Optional.ofNullable(dto.getClusterId()).orElse(0));
                case "status" -> Comparator.comparing(
                    dto -> Optional.ofNullable(dto.getStatus()).orElse(""));
                default -> comparator;
            };
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
        }
        dtos.sort(comparator);
    }

    private Page<LaboratoryDTO> paginate(List<LaboratoryDTO> dtos, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<LaboratoryDTO> pageContent = start >= dtos.size() ? List.of() : dtos.subList(start, end);
        return new PageImpl<>(pageContent, pageable, dtos.size());
    }

    private String resolveClusterLabel(Long clusterId) {
        if (clusterId == null) return null;
        return vClusterRepository.findById(clusterId)
            .map(VCluster::getDescripcion)
            .orElse("Cluster " + clusterId);
    }

    private record ParsedMembershipKey(Long personId, LocalDate startDate, String tipoRecurso) {}
}

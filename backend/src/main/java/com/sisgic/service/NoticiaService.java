package com.sisgic.service;

import com.sisgic.dto.*;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.util.TaxonomyNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class NoticiaService {

    private static final Map<Long, String> ESTADO_CODES = Map.of(
        EstadoNoticia.PUBLISHED, "PUBLISHED",
        EstadoNoticia.DRAFT, "DRAFT",
        EstadoNoticia.UNPUBLISHED, "UNPUBLISHED"
    );

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private EstadoNoticiaRepository estadoNoticiaRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TipoProductoRepository tipoProductoRepository;

    @Autowired
    private TextosService textosService;

    @Autowired
    private UserService userService;

    @Autowired
    private RelatedNewsService relatedNewsService;

    @PostConstruct
    void ensureEstadoNoticiaCatalog() {
        ensureEstadoNoticiaExists(EstadoNoticia.PUBLISHED);
        ensureEstadoNoticiaExists(EstadoNoticia.DRAFT);
        ensureEstadoNoticiaExists(EstadoNoticia.UNPUBLISHED);
    }

    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findAll(
            Long estadoId,
            String tagId,
            String categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            String title,
            Pageable pageable) {
        return noticiaRepository.findByFilters(estadoId, tagId, categoryId, fromDate, toDate, title, pageable)
            .map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findPublished(
            String tagId,
            String categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            String title,
            Pageable pageable) {
        return findAll(EstadoNoticia.PUBLISHED, tagId, categoryId, fromDate, toDate, title, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<NoticiaDTO> findById(Long id) {
        return noticiaRepository.findByIdWithRelations(id).map(this::toDetailDto);
    }

    @Transactional(readOnly = true)
    public Optional<NoticiaDTO> findPublishedById(Long id) {
        return noticiaRepository.findByIdWithRelations(id)
            .filter(n -> n.getEstado() != null && n.getEstado().getId() == EstadoNoticia.PUBLISHED)
            .map(this::toDetailDto);
    }

    @Transactional(readOnly = true)
    public List<EstadoNoticiaDTO> listEstados() {
        return estadoNoticiaRepository.findAll().stream()
            .map(this::toEstadoDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TagDTO> listTags() {
        return tagRepository.findAllByOrderByLabelAsc().stream()
            .map(this::toTagDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listCategories() {
        return categoryRepository.findAllByOrderByLabelAsc().stream()
            .map(this::toCategoryDto)
            .collect(Collectors.toList());
    }

    public TagDTO createTag(CreateTagRequest request) {
        if (request == null || request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("Tag label is required");
        }
        String label = request.getLabel().trim();
        String slug = TaxonomyNormalizer.slugify(label);

        Optional<Tag> existing = tagRepository.findAll().stream()
            .filter(t -> TaxonomyNormalizer.termsMatch(t.getLabel(), label)
                || TaxonomyNormalizer.termsMatch(t.getSlug(), slug))
            .findFirst();
        if (existing.isPresent()) {
            return toTagDto(existing.get());
        }

        Tag tag = new Tag();
        tag.setId(UUID.randomUUID().toString());
        tag.setLabel(label);
        tag.setSlug(slug);
        tag.setLanguage(null);
        LocalDateTime now = LocalDateTime.now();
        tag.setCreatedDate(now);
        tag.setUpdatedDate(now);
        return toTagDto(tagRepository.save(tag));
    }

    public CategoryDTO createCategory(CreateCategoryRequest request) {
        if (request == null || request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("Category label is required");
        }
        String label = request.getLabel().trim();
        String slug = TaxonomyNormalizer.slugify(label);

        Optional<Category> existing = categoryRepository.findAll().stream()
            .filter(c -> TaxonomyNormalizer.termsMatch(c.getLabel(), label)
                || TaxonomyNormalizer.termsMatch(c.getSlug(), slug))
            .findFirst();
        if (existing.isPresent()) {
            return toCategoryDto(existing.get());
        }

        Category category = new Category();
        category.setId(UUID.randomUUID().toString());
        category.setLabel(label);
        category.setSlug(slug);
        category.setLanguage(null);
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedDate(now);
        category.setUpdatedDate(now);
        return toCategoryDto(categoryRepository.save(category));
    }

    public NoticiaDTO create(NoticiaDTO dto) {
        List<Long> relatedPostIds = dto.getRelatedPostIds();
        Noticia noticia = new Noticia();
        applyDtoToEntity(noticia, dto, true);
        userService.getCurrentUsername().ifPresent(noticia::setUsername);
        if (noticia.getNumVisitas() == null) {
            noticia.setNumVisitas(0);
        }
        if (noticia.getNumLikes() == null) {
            noticia.setNumLikes(0);
        }
        tipoProductoRepository.findById((long) Noticia.ID_TIPO_PRODUCTO)
            .ifPresent(noticia::setTipoProducto);
        Noticia saved = noticiaRepository.save(noticia);
        applyPublishDates(saved);
        noticiaRepository.save(saved);
        if (relatedPostIds != null) {
            relatedNewsService.syncRelatedPosts(saved.getId(), relatedPostIds);
        }
        return noticiaRepository.findByIdWithRelations(saved.getId())
            .map(this::toDetailDto)
            .orElseThrow(() -> new IllegalStateException("News item could not be loaded after create"));
    }

    public Optional<NoticiaDTO> update(Long id, NoticiaDTO dto) {
        return noticiaRepository.findByIdWithRelations(id).map(existing -> {
            applyDtoToEntity(existing, dto, false);
            applyPublishDates(existing);
            Noticia saved = noticiaRepository.save(existing);
            if (dto.getRelatedPostIds() != null) {
                relatedNewsService.syncRelatedPosts(id, dto.getRelatedPostIds());
            }
            return toDetailDto(saved);
        });
    }

    public boolean delete(Long id) {
        return noticiaRepository.findById(id).map(noticia -> {
            if (noticia.getIdTitulo() != null) {
                textosService.deleteLocalizedText(noticia.getIdTitulo(), Noticia.ID_TIPO_TEXTO);
            }
            if (noticia.getDescripcion() != null) {
                textosService.deleteLocalizedText(noticia.getDescripcion(), Noticia.ID_TIPO_TEXTO);
            }
            if (noticia.getComentario() != null) {
                textosService.deleteLocalizedText(noticia.getComentario(), Noticia.ID_TIPO_TEXTO);
            }
            relatedNewsService.deleteAllReferences(id);
            noticiaRepository.delete(noticia);
            return true;
        }).orElse(false);
    }

    public boolean incrementVisitas(Long id) {
        return noticiaRepository.incrementVisitas(id) > 0;
    }

    public boolean incrementLikes(Long id) {
        return noticiaRepository.incrementLikes(id) > 0;
    }

    private void applyDtoToEntity(Noticia noticia, NoticiaDTO dto, boolean isCreate) {
        if (dto.getTitle() != null && dto.getTitle().hasAnyValue()) {
            if (isCreate || noticia.getIdTitulo() == null || noticia.getIdTitulo().isEmpty()) {
                noticia.setIdTitulo(textosService.createLocalizedText(dto.getTitle(), Noticia.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(noticia.getIdTitulo(), dto.getTitle(), Noticia.ID_TIPO_TEXTO);
            }
        }

        if (dto.getExcerpt() != null && dto.getExcerpt().hasAnyValue()) {
            if (isCreate || noticia.getDescripcion() == null || noticia.getDescripcion().isEmpty()) {
                noticia.setDescripcion(textosService.createLocalizedText(dto.getExcerpt(), Noticia.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(noticia.getDescripcion(), dto.getExcerpt(), Noticia.ID_TIPO_TEXTO);
            }
        }

        if (dto.getBody() != null && dto.getBody().hasAnyValue()) {
            if (isCreate || noticia.getComentario() == null || noticia.getComentario().isEmpty()) {
                noticia.setComentario(textosService.createLocalizedText(dto.getBody(), Noticia.ID_TIPO_TEXTO));
            } else {
                textosService.updateLocalizedText(noticia.getComentario(), dto.getBody(), Noticia.ID_TIPO_TEXTO);
            }
        }

        applyEstado(noticia, dto, isCreate);

        if (dto.getImage() != null) {
            noticia.setImage(dto.getImage());
        }

        if (dto.getLinkVisualizacion() != null) {
            noticia.setLinkVisualizacion(dto.getLinkVisualizacion().isBlank() ? null : dto.getLinkVisualizacion().trim());
        }

        applyPublicationDates(noticia, dto, isCreate);

        if (dto.getTags() != null) {
            Set<Tag> tags = resolveTags(dto.getTags());
            noticia.getTags().clear();
            noticia.getTags().addAll(tags);
        }

        if (dto.getCategories() != null) {
            Set<Category> categories = resolveCategories(dto.getCategories());
            noticia.getCategories().clear();
            noticia.getCategories().addAll(categories);
        }

        applyFeature(noticia, dto.getFeature(), isCreate);
        applyBasal(noticia, dto.getBasal(), isCreate);
    }

    private void applyEstado(Noticia noticia, NoticiaDTO dto, boolean isCreate) {
        long estadoId = EstadoNoticia.DRAFT;
        if (dto.getEstado() != null && dto.getEstado().getId() != null) {
            estadoId = dto.getEstado().getId();
        }
        noticia.setEstado(ensureEstadoNoticiaExists(estadoId));
    }

    private EstadoNoticia ensureEstadoNoticiaExists(long id) {
        return estadoNoticiaRepository.findById(id).orElseGet(() -> {
            EstadoNoticia estado = new EstadoNoticia();
            estado.setId(id);
            estado.setIdDescripcion(ESTADO_CODES.getOrDefault(id, "UNKNOWN"));
            return estadoNoticiaRepository.save(estado);
        });
    }

    private void applyBasal(Noticia noticia, String basalDto, boolean isCreate) {
        Character normalized = normalizeBasal(basalDto);
        if (normalized != null) {
            noticia.setBasal(normalized);
        } else if (isCreate) {
            noticia.setBasal('N');
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

    private void applyPublicationDates(Noticia noticia, NoticiaDTO dto, boolean isCreate) {
        if (dto.getFirstPublishedDate() != null && !dto.getFirstPublishedDate().isBlank()) {
            noticia.setFechaInicio(parseDate(dto.getFirstPublishedDate()));
        } else if (isCreate && noticia.getFechaInicio() == null) {
            noticia.setFechaInicio(LocalDate.now());
        }

        if (dto.getLastPublishedDate() != null && !dto.getLastPublishedDate().isBlank()) {
            noticia.setFechaTermino(parseDate(dto.getLastPublishedDate()));
        }
    }

    private void applyPublishDates(Noticia noticia) {
        if (noticia.getEstado() != null && noticia.getEstado().getId() == EstadoNoticia.PUBLISHED) {
            LocalDate today = LocalDate.now();
            if (noticia.getFechaInicio() == null) {
                noticia.setFechaInicio(today);
            }
            if (noticia.getFechaTermino() == null) {
                noticia.setFechaTermino(today);
            }
        }
    }

    private void applyFeature(Noticia noticia, String featureDto, boolean isCreate) {
        Character normalized = normalizeFeature(featureDto);
        if (normalized != null) {
            noticia.setFeature(normalized);
        } else if (isCreate && noticia.getFeature() == null) {
            noticia.setFeature('N');
        }
    }

    private Character normalizeFeature(String feature) {
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

    private Set<Category> resolveCategories(List<CategoryDTO> categoryDtos) {
        if (categoryDtos == null || categoryDtos.isEmpty()) {
            return new HashSet<>();
        }
        List<String> ids = categoryDtos.stream()
            .map(CategoryDTO::getId)
            .filter(Objects::nonNull)
            .filter(id -> !id.isBlank())
            .distinct()
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(categoryRepository.findAllById(ids));
    }

    private Set<Tag> resolveTags(List<TagDTO> tagDtos) {
        if (tagDtos == null || tagDtos.isEmpty()) {
            return new HashSet<>();
        }
        List<String> ids = tagDtos.stream()
            .map(TagDTO::getId)
            .filter(Objects::nonNull)
            .filter(id -> !id.isBlank())
            .distinct()
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findAllById(ids));
    }

    private NoticiaDTO toListDto(Noticia noticia) {
        NoticiaDTO dto = new NoticiaDTO();
        dto.setId(noticia.getId());
        dto.setTitle(textosService.getLocalizedText(noticia.getIdTitulo(), Noticia.ID_TIPO_TEXTO));
        dto.setExcerpt(textosService.getLocalizedText(noticia.getDescripcion(), Noticia.ID_TIPO_TEXTO));
        dto.setEstado(toEstadoDto(noticia.getEstado()));
        dto.setNumVisitas(noticia.getNumVisitas());
        dto.setNumLikes(noticia.getNumLikes());
        dto.setImage(noticia.getImage());
        dto.setLinkVisualizacion(noticia.getLinkVisualizacion());
        dto.setFirstPublishedDate(formatDate(noticia.getFechaInicio()));
        dto.setLastPublishedDate(formatDate(noticia.getFechaTermino()));
        dto.setCreatedAt(noticia.getCreatedAt() != null ? noticia.getCreatedAt().toString() : null);
        dto.setUpdatedAt(noticia.getUpdatedAt() != null ? noticia.getUpdatedAt().toString() : null);
        dto.setUsername(noticia.getUsername());
        if (noticia.getBasal() != null) {
            dto.setBasal(String.valueOf(noticia.getBasal()));
        }
        if (noticia.getTags() != null) {
            dto.setTags(noticia.getTags().stream().map(this::toTagDto).collect(Collectors.toList()));
        }
        if (noticia.getCategories() != null) {
            dto.setCategories(noticia.getCategories().stream().map(this::toCategoryDto).collect(Collectors.toList()));
        }
        if (noticia.getFeature() != null) {
            dto.setFeature(String.valueOf(noticia.getFeature()));
        }
        if (noticia.getId() != null) {
            List<Noticia> related = relatedNewsService.getRelatedNoticias(noticia.getId());
            if (!related.isEmpty()) {
                dto.setRelatedPosts(related.stream()
                    .map(this::toRelatedPostSummary)
                    .collect(Collectors.toList()));
                dto.setRelatedPostIds(related.stream()
                    .map(Noticia::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            }
        }
        return dto;
    }

    private RelatedPostSummaryDTO toRelatedPostSummary(Noticia related) {
        RelatedPostSummaryDTO dto = new RelatedPostSummaryDTO();
        dto.setId(related.getId());
        LocalizedTextDTO title = textosService.getLocalizedText(related.getIdTitulo(), Noticia.ID_TIPO_TEXTO);
        dto.setTitle(title != null && title.getEs() != null ? title.getEs() : "");
        dto.setTitleEn(title != null && title.getUs() != null ? title.getUs() : "");
        dto.setImage(related.getImage());
        dto.setAuthor(related.getUsername());
        dto.setPublicationDate(formatDate(related.getFechaInicio()));
        if (related.getEstado() != null) {
            dto.setPublicationStatus(ESTADO_CODES.getOrDefault(related.getEstado().getId(), "UNKNOWN"));
        }
        return dto;
    }

    private NoticiaDTO toDetailDto(Noticia noticia) {
        NoticiaDTO dto = toListDto(noticia);
        dto.setBody(textosService.getLocalizedText(noticia.getComentario(), Noticia.ID_TIPO_TEXTO));
        return dto;
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

    private TagDTO toTagDto(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setLabel(tag.getLabel());
        dto.setSlug(tag.getSlug());
        dto.setLanguage(tag.getLanguage());
        dto.setPostCount(tag.getPostCount());
        dto.setPublishedPostCount(tag.getPublishedPostCount());
        return dto;
    }

    private CategoryDTO toCategoryDto(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setLabel(category.getLabel());
        dto.setSlug(category.getSlug());
        return dto;
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

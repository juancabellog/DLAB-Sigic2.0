package com.sisgic.controller;

import com.sisgic.dto.BookProductDTO;
import com.sisgic.dto.BookTypeDTO;
import com.sisgic.dto.ParticipanteDTO;
import com.sisgic.dto.TipoProductoDTO;
import com.sisgic.entity.*;
import com.sisgic.repository.*;
import com.sisgic.service.TextosService;
import com.sisgic.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);
    private static final long BOOK_TIPO_PRODUCTO_ID = 20L;
    private static final long BOOK_TYPE_CHAPTER_ID = 2L;
    /** Chapter Author — only valid when Work Type is Chapter. */
    private static final long CHAPTER_AUTHOR_PARTICIPATION_ID = 32L;

    @Autowired private BookRepository bookRepository;
    @Autowired private BookTypeRepository bookTypeRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private RRHHRepository rrhhRepository;
    @Autowired private TipoParticipacionRepository tipoParticipacionRepository;
    @Autowired private ParticipacionProductoRepository participacionProductoRepository;
    @Autowired private TextosService textosService;
    @Autowired private UserService userService;

    @GetMapping
    public ResponseEntity<Page<BookProductDTO>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        Page<Book> books = bookRepository.findVisibleByUserIdRRHH(idRRHH, userName, pageable);

        List<String> codigosTexto = new ArrayList<>();
        for (Book b : books.getContent()) {
            if (b.getDescripcion() != null && !b.getDescripcion().isEmpty()) {
                codigosTexto.add(b.getDescripcion());
            }
            if (b.getComentario() != null && !b.getComentario().isEmpty()) {
                codigosTexto.add(b.getComentario());
            }
        }
        Map<String, String> textosMap = textosService.getTextValuesBatch(codigosTexto, 2, "us");
        return ResponseEntity.ok(books.map(b -> convertToDTOWithoutParticipants(b, textosMap)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookProductDTO> getBook(@PathVariable Long id) {
        Long idRRHH = userService.getCurrentUserIdRRHH().orElse(null);
        String userName = userService.getCurrentUsername().orElse(null);
        return bookRepository.findVisibleByIdAndUserIdRRHH(id, idRRHH, userName)
            .map(entity -> ResponseEntity.ok(convertToDTO(entity)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createBook(@RequestBody BookProductDTO dto) {
        try {
            validateDto(dto);
            Book entity = convertFromDTO(dto);
            userService.getCurrentUsername().ifPresent(entity::setUsername);
            entity.setTipoProducto(resolveBookProductType());
            Book saved = bookRepository.save(entity);
            if (dto.getParticipantes() != null && !dto.getParticipantes().isEmpty()) {
                saveParticipantes(saved, dto.getParticipantes());
            }
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (IllegalArgumentException ex) {
            log.warn("Create book validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error creating book", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not create book"));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookProductDTO dto) {
        try {
            validateDto(dto);
            return bookRepository.findById(id)
                .map(existing -> {
                    applyProductFields(existing, dto, true);
                    applyBookFields(existing, dto);
                    existing.setTipoProducto(resolveBookProductType());
                    if (dto.getParticipantes() != null) {
                        participacionProductoRepository.deleteByProductoId(existing.getId());
                        if (!dto.getParticipantes().isEmpty()) {
                            saveParticipantes(existing, dto.getParticipantes());
                        }
                    }
                    Book updated = bookRepository.save(existing);
                    return ResponseEntity.ok(convertToDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            log.warn("Update book validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            log.error("Error updating book id={}", id, e);
            return ResponseEntity.badRequest().body(Map.of("message", "Could not update book"));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        return bookRepository.findById(id)
            .map(entity -> {
                participacionProductoRepository.deleteByProductoId(id);
                bookRepository.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private void validateDto(BookProductDTO dto) {
        if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("Book Title is required");
        }
        Long bookTypeId = resolveBookTypeId(dto);
        bookTypeRepository.findById(bookTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Work Type is required"));

        if (BOOK_TYPE_CHAPTER_ID == bookTypeId
            && (dto.getChapterTitle() == null || dto.getChapterTitle().isBlank())) {
            throw new IllegalArgumentException("Book Chapter is required when Work Type is Chapter");
        }
        if (dto.getFirstPage() == null || dto.getFirstPage() < 1) {
            throw new IllegalArgumentException("First Page must be a positive integer");
        }
        if (dto.getLastPage() == null || dto.getLastPage() < 1) {
            throw new IllegalArgumentException("Last Page must be a positive integer");
        }
        if (dto.getLastPage() < dto.getFirstPage()) {
            throw new IllegalArgumentException("Last Page must be greater than or equal to First Page");
        }
        int maxYear = Year.now().getValue() + 1;
        if (dto.getYear() == null || dto.getYear() < 1900 || dto.getYear() > maxYear) {
            throw new IllegalArgumentException("Year must be a valid year between 1900 and " + maxYear);
        }
        if (BOOK_TYPE_CHAPTER_ID != bookTypeId
            && dto.getParticipantes() != null) {
            boolean hasChapterAuthor = dto.getParticipantes().stream()
                .anyMatch(p -> p.getTipoParticipacionId() != null
                    && p.getTipoParticipacionId().equals(CHAPTER_AUTHOR_PARTICIPATION_ID));
            if (hasChapterAuthor) {
                throw new IllegalArgumentException(
                    "Chapter Author participation is only allowed when Work Type is Chapter");
            }
        }
    }

    private Long resolveBookTypeId(BookProductDTO dto) {
        if (dto.getIdBookType() != null) {
            return dto.getIdBookType();
        }
        if (dto.getBookType() != null && dto.getBookType().getId() != null) {
            return dto.getBookType().getId();
        }
        throw new IllegalArgumentException("Work Type is required");
    }

    private TipoProducto resolveBookProductType() {
        return tipoProductoRepository.findById(BOOK_TIPO_PRODUCTO_ID)
            .orElseThrow(() -> new IllegalStateException(
                "TipoProducto with id=" + BOOK_TIPO_PRODUCTO_ID + " (BOOK) not found"));
    }

    private Book convertFromDTO(BookProductDTO dto) {
        Book entity = new Book();
        applyProductFields(entity, dto, false);
        applyBookFields(entity, dto);
        return entity;
    }

    private void applyProductFields(Book entity, BookProductDTO dto, boolean isUpdate) {
        if (dto.getDescripcion() != null && !dto.getDescripcion().isBlank()) {
            if (isUpdate && entity.getDescripcion() != null && !entity.getDescripcion().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getDescripcion(), dto.getDescripcion().trim(), 2);
            } else {
                entity.setDescripcion(textosService.createTextInBothLanguages(dto.getDescripcion().trim(), 2));
            }
        }
        if (dto.getComentario() != null && !dto.getComentario().isBlank()) {
            if (isUpdate && entity.getComentario() != null && !entity.getComentario().isEmpty()) {
                textosService.updateTextInBothLanguages(entity.getComentario(), dto.getComentario().trim(), 2);
            } else {
                entity.setComentario(textosService.createTextInBothLanguages(dto.getComentario().trim(), 2));
            }
        } else if (isUpdate && dto.getComentario() != null && dto.getComentario().isBlank()) {
            entity.setComentario(null);
        }

        // Optional start date; default to Jan 1 of book year when year is set (helps progress report)
        if (dto.getFechaInicio() != null && !dto.getFechaInicio().isBlank()) {
            entity.setFechaInicio(LocalDate.parse(dto.getFechaInicio().trim()));
        } else if (dto.getYear() != null) {
            entity.setFechaInicio(LocalDate.of(dto.getYear(), 1, 1));
        } else {
            entity.setFechaInicio(null);
        }
        entity.setFechaTermino(null);
        entity.setProgressReport(dto.getProgressReport());
        entity.setCodigoANID(dto.getCodigoANID());
        if (dto.getBasal() != null && !dto.getBasal().isEmpty()) {
            char basalValue = Character.toUpperCase(dto.getBasal().charAt(0));
            entity.setBasal((basalValue == 'S' || basalValue == 'N') ? basalValue : 'N');
        } else {
            entity.setBasal(isUpdate ? 'N' : 'S');
        }
        entity.setCluster(dto.getCluster());
    }

    private void applyBookFields(Book entity, BookProductDTO dto) {
        Long bookTypeId = resolveBookTypeId(dto);
        BookType bookType = bookTypeRepository.findById(bookTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Work Type is required"));
        entity.setBookType(bookType);

        if (BOOK_TYPE_CHAPTER_ID == bookTypeId) {
            entity.setChapterTitle(trimToNull(dto.getChapterTitle()));
        } else {
            entity.setChapterTitle(null);
        }
        entity.setFirstPage(dto.getFirstPage());
        entity.setLastPage(dto.getLastPage());
        entity.setEditorialCityCountry(trimToNull(dto.getEditorialCityCountry()));
        entity.setYear(dto.getYear());
        entity.setIsbn(trimToNull(dto.getIsbn()));
    }

    private BookProductDTO convertToDTOWithoutParticipants(Book entity, Map<String, String> textosMap) {
        BookProductDTO dto = convertToDTOBase(entity, textosMap);
        dto.setParticipantes(null);
        return dto;
    }

    private BookProductDTO convertToDTO(Book entity) {
        BookProductDTO dto = convertToDTOBase(entity, null);
        List<ParticipacionProducto> participaciones = participacionProductoRepository.findByProductoId(entity.getId());
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

    private BookProductDTO convertToDTOBase(Book entity, Map<String, String> textosMap) {
        BookProductDTO dto = new BookProductDTO();
        dto.setId(entity.getId());

        if (entity.getDescripcion() != null && !entity.getDescripcion().isEmpty()) {
            String descripcion = textosMap != null && textosMap.containsKey(entity.getDescripcion())
                ? textosMap.get(entity.getDescripcion())
                : textosService.getTextValue(entity.getDescripcion(), 2, "us").orElse(entity.getDescripcion());
            dto.setDescripcion(descripcion);
        }
        if (entity.getComentario() != null && !entity.getComentario().isEmpty()) {
            String comentario = textosMap != null && textosMap.containsKey(entity.getComentario())
                ? textosMap.get(entity.getComentario())
                : textosService.getTextValue(entity.getComentario(), 2, "us").orElse(entity.getComentario());
            dto.setComentario(comentario);
        }
        dto.setFechaInicio(entity.getFechaInicio() != null ? entity.getFechaInicio().toString() : null);
        dto.setFechaTermino(entity.getFechaTermino() != null ? entity.getFechaTermino().toString() : null);
        dto.setProgressReport(entity.getProgressReport());
        dto.setCodigoANID(entity.getCodigoANID());
        if (entity.getBasal() != null) {
            char basalChar = entity.getBasal();
            if (basalChar == '1') {
                dto.setBasal("S");
            } else if (basalChar == '0') {
                dto.setBasal("N");
            } else {
                dto.setBasal(String.valueOf(basalChar));
            }
        }
        dto.setCluster(entity.getCluster());
        dto.setParticipantesNombres(entity.getParticipantesNombres());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);

        if (entity.getTipoProducto() != null) {
            Long tipoId = entity.getTipoProducto().getId();
            TipoProducto tipoProducto = tipoId != null
                ? tipoProductoRepository.findById(tipoId).orElse(null)
                : null;
            TipoProductoDTO tipoDTO = new TipoProductoDTO();
            tipoDTO.setId(tipoId);
            if (tipoProducto != null) {
                tipoDTO.setIdDescripcion(tipoProducto.getIdDescripcion());
                tipoDTO.setDescripcion(tipoProducto.getDescripcion());
            }
            dto.setTipoProducto(tipoDTO);
        }

        dto.setChapterTitle(entity.getChapterTitle());
        dto.setFirstPage(entity.getFirstPage());
        dto.setLastPage(entity.getLastPage());
        dto.setEditorialCityCountry(entity.getEditorialCityCountry());
        dto.setYear(entity.getYear());
        dto.setIsbn(entity.getIsbn());

        if (entity.getBookType() != null) {
            Long bookTypeId = entity.getBookType().getId();
            BookType bookType = bookTypeId != null
                ? bookTypeRepository.findById(bookTypeId).orElse(null)
                : null;
            BookTypeDTO bookTypeDTO = new BookTypeDTO();
            bookTypeDTO.setId(bookTypeId);
            if (bookType != null) {
                bookTypeDTO.setIdDescripcion(bookType.getIdDescripcion());
                dto.setBookTypeLabel(bookType.getIdDescripcion());
            }
            dto.setBookType(bookTypeDTO);
            dto.setIdBookType(bookTypeId);
        }
        return dto;
    }

    private void saveParticipantes(Book book, List<ParticipanteDTO> participantesDTO) {
        for (ParticipanteDTO pDTO : participantesDTO) {
            if (pDTO.getRrhhId() == null || pDTO.getTipoParticipacionId() == null) {
                continue;
            }
            RRHH rrhh = rrhhRepository.findById(pDTO.getRrhhId()).orElse(null);
            TipoParticipacion tipoParticipacion = tipoParticipacionRepository.findById(pDTO.getTipoParticipacionId()).orElse(null);
            if (rrhh == null || tipoParticipacion == null) {
                continue;
            }
            Long nextId = participacionProductoRepository.getNextIdForParticipacion(book.getId(), rrhh.getId());
            ParticipacionProducto participacion = new ParticipacionProducto();
            participacion.setRrhh(rrhh);
            participacion.setProducto(book);
            participacion.setTipoParticipacion(tipoParticipacion);
            participacion.setOrden(pDTO.getOrden() != null ? pDTO.getOrden() : 0);
            participacion.setCorresponding(pDTO.getCorresponding() != null && pDTO.getCorresponding());
            participacion.setId(new ParticipacionProductoId(rrhh.getId(), book.getId(), nextId));
            participacionProductoRepository.save(participacion);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

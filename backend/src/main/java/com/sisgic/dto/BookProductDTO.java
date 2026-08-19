package com.sisgic.dto;

import java.util.List;

/**
 * DTO for scientific product Books (producto + book). idTipoProducto = 20.
 */
public class BookProductDTO {

    private Long id;
    private String descripcion;
    private String comentario;
    private String fechaInicio;
    private String fechaTermino;
    private TipoProductoDTO tipoProducto;
    private String progressReport;
    private String codigoANID;
    private String basal;
    private String cluster;
    private String participantesNombres;
    private String createdAt;
    private String updatedAt;
    private List<ParticipanteDTO> participantes;

    private Long idBookType;
    private BookTypeDTO bookType;
    private String bookTypeLabel;
    private String chapterTitle;
    private Integer firstPage;
    private Integer lastPage;
    private String editorialCityCountry;
    private Integer year;
    private String isbn;

    public BookProductDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaTermino() { return fechaTermino; }
    public void setFechaTermino(String fechaTermino) { this.fechaTermino = fechaTermino; }
    public TipoProductoDTO getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(TipoProductoDTO tipoProducto) { this.tipoProducto = tipoProducto; }
    public String getProgressReport() { return progressReport; }
    public void setProgressReport(String progressReport) { this.progressReport = progressReport; }
    public String getCodigoANID() { return codigoANID; }
    public void setCodigoANID(String codigoANID) { this.codigoANID = codigoANID; }
    public String getBasal() { return basal; }
    public void setBasal(String basal) { this.basal = basal; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getParticipantesNombres() { return participantesNombres; }
    public void setParticipantesNombres(String participantesNombres) { this.participantesNombres = participantesNombres; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<ParticipanteDTO> getParticipantes() { return participantes; }
    public void setParticipantes(List<ParticipanteDTO> participantes) { this.participantes = participantes; }

    public Long getIdBookType() { return idBookType; }
    public void setIdBookType(Long idBookType) { this.idBookType = idBookType; }
    public BookTypeDTO getBookType() { return bookType; }
    public void setBookType(BookTypeDTO bookType) { this.bookType = bookType; }
    public String getBookTypeLabel() { return bookTypeLabel; }
    public void setBookTypeLabel(String bookTypeLabel) { this.bookTypeLabel = bookTypeLabel; }
    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public Integer getFirstPage() { return firstPage; }
    public void setFirstPage(Integer firstPage) { this.firstPage = firstPage; }
    public Integer getLastPage() { return lastPage; }
    public void setLastPage(Integer lastPage) { this.lastPage = lastPage; }
    public String getEditorialCityCountry() { return editorialCityCountry; }
    public void setEditorialCityCountry(String editorialCityCountry) { this.editorialCityCountry = editorialCityCountry; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
}

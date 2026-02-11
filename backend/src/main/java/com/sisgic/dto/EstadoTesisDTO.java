package com.sisgic.dto;

public class EstadoTesisDTO {
    private Long id;
    private String idDescripcion;
    private String descripcion;

    public EstadoTesisDTO() {}

    public EstadoTesisDTO(Long id, String idDescripcion, String descripcion) {
        this.id = id;
        this.idDescripcion = idDescripcion;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdDescripcion() {
        return idDescripcion;
    }

    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}











package com.sisgic.dto;

public class CategoriaTransferenciaDTO {
    private Long id;
    private String idDescripcion;

    public CategoriaTransferenciaDTO() {}

    public CategoriaTransferenciaDTO(Long id, String idDescripcion) {
        this.id = id;
        this.idDescripcion = idDescripcion;
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
}











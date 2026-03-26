package com.sisgic.dto;

/**
 * DTO for index types (v_index_type).
 */
public class IndexTypeDTO {

    private Long id;
    private String descripcion;

    public IndexTypeDTO() {}

    public IndexTypeDTO(Long id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}


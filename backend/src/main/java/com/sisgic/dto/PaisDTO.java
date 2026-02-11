package com.sisgic.dto;

public class PaisDTO {
    private String codigo;
    private String idDescripcion;

    public PaisDTO() {}

    public PaisDTO(String codigo, String idDescripcion) {
        this.codigo = codigo;
        this.idDescripcion = idDescripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getIdDescripcion() {
        return idDescripcion;
    }

    public void setIdDescripcion(String idDescripcion) {
        this.idDescripcion = idDescripcion;
    }
}











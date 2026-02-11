package com.sisgic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TextosId implements Serializable {
    
    @Column(name = "lenguaje", length = 5, columnDefinition = "CHAR(5)")
    private String lenguaje;
    
    @Column(name = "codigotexto", length = 200)
    private String codigoTexto;
    
    @Column(name = "idTipoTexto")
    private Integer idTipoTexto;
    
    // Constructors
    public TextosId() {}
    
    public TextosId(String lenguaje, String codigoTexto, Integer idTipoTexto) {
        this.lenguaje = lenguaje;
        this.codigoTexto = codigoTexto;
        this.idTipoTexto = idTipoTexto;
    }
    
    // Getters and Setters
    public String getLenguaje() {
        return lenguaje;
    }
    
    public void setLenguaje(String lenguaje) {
        this.lenguaje = lenguaje;
    }
    
    public String getCodigoTexto() {
        return codigoTexto;
    }
    
    public void setCodigoTexto(String codigoTexto) {
        this.codigoTexto = codigoTexto;
    }
    
    public Integer getIdTipoTexto() {
        return idTipoTexto;
    }
    
    public void setIdTipoTexto(Integer idTipoTexto) {
        this.idTipoTexto = idTipoTexto;
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextosId textosId = (TextosId) o;
        return Objects.equals(lenguaje, textosId.lenguaje) &&
               Objects.equals(codigoTexto, textosId.codigoTexto) &&
               Objects.equals(idTipoTexto, textosId.idTipoTexto);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(lenguaje, codigoTexto, idTipoTexto);
    }
}












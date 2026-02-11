package com.sisgic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transferenciatecnologica")
@PrimaryKeyJoinColumn(name = "id")
public class TransferenciaTecnologica extends ProductoCientifico {
    
    @ManyToOne
    @JoinColumn(name = "idInstitucion")
    private Institucion institucion;
    
    @ManyToOne
    @JoinColumn(name = "idTipoTransferencia")
    private TipoTransferencia tipoTransferencia;
    
    @Lob
    @Column(name = "categoriaTransferencia")
    private String categoriaTransferencia; // JSON string o lista separada por comas
    
    @Column(name = "ciudad", length = 200)
    private String ciudad;
    
    @Column(name = "region", length = 200)
    private String region;
    
    @Column(name = "year")
    private Integer agno;
    
    @ManyToOne
    @JoinColumn(name = "codigoPais")
    private Pais pais;
    
    // Constructors
    public TransferenciaTecnologica() {}
    
    // Getters and Setters
    public Institucion getInstitucion() {
        return institucion;
    }
    
    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }
    
    public TipoTransferencia getTipoTransferencia() {
        return tipoTransferencia;
    }
    
    public void setTipoTransferencia(TipoTransferencia tipoTransferencia) {
        this.tipoTransferencia = tipoTransferencia;
    }
    
    public String getCategoriaTransferencia() {
        return categoriaTransferencia;
    }
    
    public void setCategoriaTransferencia(String categoriaTransferencia) {
        this.categoriaTransferencia = categoriaTransferencia;
    }
    
    public String getCiudad() {
        return ciudad;
    }
    
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public Integer getAgno() {
        return agno;
    }
    
    public void setAgno(Integer agno) {
        this.agno = agno;
    }
    
    public Pais getPais() {
        return pais;
    }
    
    public void setPais(Pais pais) {
        this.pais = pais;
    }
}


package com.sisgic.dto;

import java.util.List;

/**
 * DTO para Participante en un producto
 */
public class ParticipanteDTO {
    
    private Long rrhhId;
    private Long tipoParticipacionId;
    private Integer orden;
    private Boolean corresponding; // Para publicaciones
    private Long idRRHHProducto; // ID de la participación en rrhh_producto
    private List<AfiliacionDTO> afiliaciones; // Afiliaciones asociadas a este participante
    
    // Constructors
    public ParticipanteDTO() {}
    
    public ParticipanteDTO(Long rrhhId, Long tipoParticipacionId, Integer orden, Boolean corresponding) {
        this.rrhhId = rrhhId;
        this.tipoParticipacionId = tipoParticipacionId;
        this.orden = orden;
        this.corresponding = corresponding;
    }
    
    // Getters and Setters
    public Long getRrhhId() {
        return rrhhId;
    }
    
    public void setRrhhId(Long rrhhId) {
        this.rrhhId = rrhhId;
    }
    
    public Long getTipoParticipacionId() {
        return tipoParticipacionId;
    }
    
    public void setTipoParticipacionId(Long tipoParticipacionId) {
        this.tipoParticipacionId = tipoParticipacionId;
    }
    
    public Integer getOrden() {
        return orden;
    }
    
    public void setOrden(Integer orden) {
        this.orden = orden;
    }
    
    public Boolean getCorresponding() {
        return corresponding;
    }
    
    public void setCorresponding(Boolean corresponding) {
        this.corresponding = corresponding;
    }
    
    public Long getIdRRHHProducto() {
        return idRRHHProducto;
    }
    
    public void setIdRRHHProducto(Long idRRHHProducto) {
        this.idRRHHProducto = idRRHHProducto;
    }
    
    public List<AfiliacionDTO> getAfiliaciones() {
        return afiliaciones;
    }
    
    public void setAfiliaciones(List<AfiliacionDTO> afiliaciones) {
        this.afiliaciones = afiliaciones;
    }
}




package com.sisgic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "participacioneventocientifico")
@PrimaryKeyJoinColumn(name = "id")
public class ParticipacionEventoCientifico extends ProductoCientifico {

    @ManyToOne
    @JoinColumn(name = "idModalidadPresentacion")
    private ModalidadPresentacion modalidadPresentacion;

    @ManyToOne
    @JoinColumn(name = "idTipoParticipacionEvento")
    private TipoParticipacionEvento tipoParticipacionEvento;

    @ManyToOne
    @JoinColumn(name = "codigoPais")
    private Pais pais;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "nameResearchLine", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String nameResearchLine;

    @Column(name = "eventName", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String eventName;

    public ModalidadPresentacion getModalidadPresentacion() {
        return modalidadPresentacion;
    }

    public void setModalidadPresentacion(ModalidadPresentacion modalidadPresentacion) {
        this.modalidadPresentacion = modalidadPresentacion;
    }

    public TipoParticipacionEvento getTipoParticipacionEvento() {
        return tipoParticipacionEvento;
    }

    public void setTipoParticipacionEvento(TipoParticipacionEvento tipoParticipacionEvento) {
        this.tipoParticipacionEvento = tipoParticipacionEvento;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getNameResearchLine() {
        return nameResearchLine;
    }

    public void setNameResearchLine(String nameResearchLine) {
        this.nameResearchLine = nameResearchLine;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}

package com.sisgic.dto;

public class TipoEventoAgendaDTO {

    private Long id;
    /** Event mode code, e.g. in_person, online, hybrid */
    private String code;
    private String label;

    public TipoEventoAgendaDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

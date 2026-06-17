package com.sisgic.dto;

/**
 * Texto multilingüe (us / es) para noticias y otros módulos.
 */
public class LocalizedTextDTO {

    private String us;
    private String es;

    public LocalizedTextDTO() {}

    public LocalizedTextDTO(String us, String es) {
        this.us = us;
        this.es = es;
    }

    public String getUs() {
        return us;
    }

    public void setUs(String us) {
        this.us = us;
    }

    public String getEs() {
        return es;
    }

    public void setEs(String es) {
        this.es = es;
    }

    public boolean hasAnyValue() {
        return (us != null && !us.isBlank()) || (es != null && !es.isBlank());
    }
}

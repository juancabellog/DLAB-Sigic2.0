package com.sisgic.dto;

public class TranslateLaboratoryRequest {
    /** "es_to_en" (default) or "en_to_es". */
    private String direction;
    private String nameEs;
    private String descriptionEs;
    private String nameEn;
    private String descriptionEn;

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getNameEs() { return nameEs; }
    public void setNameEs(String nameEs) { this.nameEs = nameEs; }

    public String getDescriptionEs() { return descriptionEs; }
    public void setDescriptionEs(String descriptionEs) { this.descriptionEs = descriptionEs; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
}

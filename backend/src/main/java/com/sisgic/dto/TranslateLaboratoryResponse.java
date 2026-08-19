package com.sisgic.dto;

public class TranslateLaboratoryResponse {
    private String nameEn;
    private String descriptionEn;
    private String nameEs;
    private String descriptionEs;

    public TranslateLaboratoryResponse() {}

    public static TranslateLaboratoryResponse fromEnglish(String nameEn, String descriptionEn) {
        TranslateLaboratoryResponse r = new TranslateLaboratoryResponse();
        r.nameEn = nameEn;
        r.descriptionEn = descriptionEn;
        return r;
    }

    public static TranslateLaboratoryResponse fromSpanish(String nameEs, String descriptionEs) {
        TranslateLaboratoryResponse r = new TranslateLaboratoryResponse();
        r.nameEs = nameEs;
        r.descriptionEs = descriptionEs;
        return r;
    }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public String getNameEs() { return nameEs; }
    public void setNameEs(String nameEs) { this.nameEs = nameEs; }

    public String getDescriptionEs() { return descriptionEs; }
    public void setDescriptionEs(String descriptionEs) { this.descriptionEs = descriptionEs; }
}

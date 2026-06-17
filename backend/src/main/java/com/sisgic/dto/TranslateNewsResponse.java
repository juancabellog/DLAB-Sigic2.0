package com.sisgic.dto;

public class TranslateNewsResponse {
    private String titleEn;
    private String summaryEn;
    private String bodyEn;

    public TranslateNewsResponse() {}

    public TranslateNewsResponse(String titleEn, String summaryEn, String bodyEn) {
        this.titleEn = titleEn;
        this.summaryEn = summaryEn;
        this.bodyEn = bodyEn;
    }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getSummaryEn() { return summaryEn; }
    public void setSummaryEn(String summaryEn) { this.summaryEn = summaryEn; }

    public String getBodyEn() { return bodyEn; }
    public void setBodyEn(String bodyEn) { this.bodyEn = bodyEn; }
}

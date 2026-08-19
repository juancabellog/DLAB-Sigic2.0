package com.sisgic.dto;

public class TranslateNewsRequest {
    /** "es_to_en" (default) or "en_to_es". */
    private String direction;
    private String titleEs;
    private String summaryEs;
    private String bodyEs;
    private String titleEn;
    private String summaryEn;
    private String bodyEn;

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getTitleEs() { return titleEs; }
    public void setTitleEs(String titleEs) { this.titleEs = titleEs; }

    public String getSummaryEs() { return summaryEs; }
    public void setSummaryEs(String summaryEs) { this.summaryEs = summaryEs; }

    public String getBodyEs() { return bodyEs; }
    public void setBodyEs(String bodyEs) { this.bodyEs = bodyEs; }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getSummaryEn() { return summaryEn; }
    public void setSummaryEn(String summaryEn) { this.summaryEn = summaryEn; }

    public String getBodyEn() { return bodyEn; }
    public void setBodyEn(String bodyEn) { this.bodyEn = bodyEn; }
}

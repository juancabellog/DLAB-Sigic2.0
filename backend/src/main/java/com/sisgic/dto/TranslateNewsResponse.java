package com.sisgic.dto;

public class TranslateNewsResponse {
    private String titleEn;
    private String summaryEn;
    private String bodyEn;
    private String titleEs;
    private String summaryEs;
    private String bodyEs;

    public TranslateNewsResponse() {}

    public TranslateNewsResponse(String titleEn, String summaryEn, String bodyEn) {
        this.titleEn = titleEn;
        this.summaryEn = summaryEn;
        this.bodyEn = bodyEn;
    }

    public static TranslateNewsResponse fromEnglish(String titleEn, String summaryEn, String bodyEn) {
        return new TranslateNewsResponse(titleEn, summaryEn, bodyEn);
    }

    public static TranslateNewsResponse fromSpanish(String titleEs, String summaryEs, String bodyEs) {
        TranslateNewsResponse r = new TranslateNewsResponse();
        r.titleEs = titleEs;
        r.summaryEs = summaryEs;
        r.bodyEs = bodyEs;
        return r;
    }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getSummaryEn() { return summaryEn; }
    public void setSummaryEn(String summaryEn) { this.summaryEn = summaryEn; }

    public String getBodyEn() { return bodyEn; }
    public void setBodyEn(String bodyEn) { this.bodyEn = bodyEn; }

    public String getTitleEs() { return titleEs; }
    public void setTitleEs(String titleEs) { this.titleEs = titleEs; }

    public String getSummaryEs() { return summaryEs; }
    public void setSummaryEs(String summaryEs) { this.summaryEs = summaryEs; }

    public String getBodyEs() { return bodyEs; }
    public void setBodyEs(String bodyEs) { this.bodyEs = bodyEs; }
}

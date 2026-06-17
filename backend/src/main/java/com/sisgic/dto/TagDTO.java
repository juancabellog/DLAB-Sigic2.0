package com.sisgic.dto;

public class TagDTO {

    private String id;
    private String label;
    private String slug;
    private String language;
    private Integer postCount;
    private Integer publishedPostCount;

    public TagDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public Integer getPublishedPostCount() {
        return publishedPostCount;
    }

    public void setPublishedPostCount(Integer publishedPostCount) {
        this.publishedPostCount = publishedPostCount;
    }
}

package com.sisgic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String slug;

    @Column(length = 10)
    private String language;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "post_count")
    private Integer postCount;

    @Column(name = "published_post_count")
    private Integer publishedPostCount;

    public Tag() {}

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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
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

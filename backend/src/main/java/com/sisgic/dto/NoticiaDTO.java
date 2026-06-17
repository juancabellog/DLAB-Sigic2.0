package com.sisgic.dto;

import java.util.ArrayList;
import java.util.List;

public class NoticiaDTO {

    private Long id;
    private LocalizedTextDTO title;
    private LocalizedTextDTO excerpt;
    private LocalizedTextDTO body;
    private EstadoNoticiaDTO estado;
    private Integer numVisitas;
    private Integer numLikes;
    private String image;
    /** firstPublishedDate (producto.fechaInicio) */
    private String firstPublishedDate;
    /** lastPublishedDate (producto.fechaTermino) */
    private String lastPublishedDate;
    private List<TagDTO> tags = new ArrayList<>();
    private List<CategoryDTO> categories = new ArrayList<>();
    private List<Long> relatedPostIds = new ArrayList<>();
    private List<RelatedPostSummaryDTO> relatedPosts = new ArrayList<>();
    /** S / N — noticias.feature */
    private String feature;
    private String createdAt;
    private String updatedAt;
    private String username;
    /** S / N — producto.basal */
    private String basal;

    public NoticiaDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalizedTextDTO getTitle() {
        return title;
    }

    public void setTitle(LocalizedTextDTO title) {
        this.title = title;
    }

    public LocalizedTextDTO getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(LocalizedTextDTO excerpt) {
        this.excerpt = excerpt;
    }

    public LocalizedTextDTO getBody() {
        return body;
    }

    public void setBody(LocalizedTextDTO body) {
        this.body = body;
    }

    public EstadoNoticiaDTO getEstado() {
        return estado;
    }

    public void setEstado(EstadoNoticiaDTO estado) {
        this.estado = estado;
    }

    public Integer getNumVisitas() {
        return numVisitas;
    }

    public void setNumVisitas(Integer numVisitas) {
        this.numVisitas = numVisitas;
    }

    public Integer getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(Integer numLikes) {
        this.numLikes = numLikes;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getFirstPublishedDate() {
        return firstPublishedDate;
    }

    public void setFirstPublishedDate(String firstPublishedDate) {
        this.firstPublishedDate = firstPublishedDate;
    }

    public String getLastPublishedDate() {
        return lastPublishedDate;
    }

    public void setLastPublishedDate(String lastPublishedDate) {
        this.lastPublishedDate = lastPublishedDate;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public List<Long> getRelatedPostIds() {
        return relatedPostIds;
    }

    public void setRelatedPostIds(List<Long> relatedPostIds) {
        this.relatedPostIds = relatedPostIds != null ? relatedPostIds : new ArrayList<>();
    }

    public List<RelatedPostSummaryDTO> getRelatedPosts() {
        return relatedPosts;
    }

    public void setRelatedPosts(List<RelatedPostSummaryDTO> relatedPosts) {
        this.relatedPosts = relatedPosts != null ? relatedPosts : new ArrayList<>();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBasal() {
        return basal;
    }

    public void setBasal(String basal) {
        this.basal = basal;
    }
}

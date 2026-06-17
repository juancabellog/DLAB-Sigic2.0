package com.sisgic.dto;

import java.util.ArrayList;
import java.util.List;

public class AgendaDTO {

    private Long id;
    private LocalizedTextDTO title;
    private LocalizedTextDTO summary;
    private LocalizedTextDTO description;
    private EstadoNoticiaDTO estado;
    private String image;
    private String eventDate;
    private String startTime;
    private String endTime;
    private String location;
    /** in_person | online | hybrid */
    private String eventMode;
    private String onlineUrl;
    private LocalizedTextDTO organizer;
    private LocalizedTextDTO speaker;
    private LocalizedTextDTO audience;
    private LocalizedTextDTO ctaLabel;
    private String ctaUrl;
    /** S / N */
    private String feature;
    private List<CategoryDTO> categories = new ArrayList<>();
    private String createdAt;
    private String updatedAt;
    private String username;
    private String basal;

    public AgendaDTO() {}

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

    public LocalizedTextDTO getSummary() {
        return summary;
    }

    public void setSummary(LocalizedTextDTO summary) {
        this.summary = summary;
    }

    public LocalizedTextDTO getDescription() {
        return description;
    }

    public void setDescription(LocalizedTextDTO description) {
        this.description = description;
    }

    public EstadoNoticiaDTO getEstado() {
        return estado;
    }

    public void setEstado(EstadoNoticiaDTO estado) {
        this.estado = estado;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEventMode() {
        return eventMode;
    }

    public void setEventMode(String eventMode) {
        this.eventMode = eventMode;
    }

    public String getOnlineUrl() {
        return onlineUrl;
    }

    public void setOnlineUrl(String onlineUrl) {
        this.onlineUrl = onlineUrl;
    }

    public LocalizedTextDTO getOrganizer() {
        return organizer;
    }

    public void setOrganizer(LocalizedTextDTO organizer) {
        this.organizer = organizer;
    }

    public LocalizedTextDTO getSpeaker() {
        return speaker;
    }

    public void setSpeaker(LocalizedTextDTO speaker) {
        this.speaker = speaker;
    }

    public LocalizedTextDTO getAudience() {
        return audience;
    }

    public void setAudience(LocalizedTextDTO audience) {
        this.audience = audience;
    }

    public LocalizedTextDTO getCtaLabel() {
        return ctaLabel;
    }

    public void setCtaLabel(LocalizedTextDTO ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    public String getCtaUrl() {
        return ctaUrl;
    }

    public void setCtaUrl(String ctaUrl) {
        this.ctaUrl = ctaUrl;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
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

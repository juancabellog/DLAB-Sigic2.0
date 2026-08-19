package com.sisgic.dto;

import java.util.ArrayList;
import java.util.List;

public class AgendaDTO {

    private Long id;
    private LocalizedTextDTO title;
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

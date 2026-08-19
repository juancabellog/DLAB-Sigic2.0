package com.sisgic.entity;

import jakarta.persistence.*;

/**
 * Scientific product subtype mapped to table {@code book}.
 * Shares primary key with {@code producto}. idTipoProducto = 20.
 */
@Entity
@Table(name = "book")
@PrimaryKeyJoinColumn(name = "id")
public class Book extends ProductoCientifico {

    /** Work type: 1 = Whole, 2 = Chapter. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idBookType", nullable = false)
    private BookType bookType;

    @Column(name = "chapterTitle", columnDefinition = "TINYTEXT")
    private String chapterTitle;

    @Column(name = "firstPage", nullable = false)
    private Integer firstPage;

    @Column(name = "lastPage", nullable = false)
    private Integer lastPage;

    @Column(name = "editorialCityCountry", columnDefinition = "TINYTEXT")
    private String editorialCityCountry;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "ISBN", columnDefinition = "TINYTEXT")
    private String isbn;

    public Book() {}

    public BookType getBookType() {
        return bookType;
    }

    public void setBookType(BookType bookType) {
        this.bookType = bookType;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Integer getFirstPage() {
        return firstPage;
    }

    public void setFirstPage(Integer firstPage) {
        this.firstPage = firstPage;
    }

    public Integer getLastPage() {
        return lastPage;
    }

    public void setLastPage(Integer lastPage) {
        this.lastPage = lastPage;
    }

    public String getEditorialCityCountry() {
        return editorialCityCountry;
    }

    public void setEditorialCityCountry(String editorialCityCountry) {
        this.editorialCityCountry = editorialCityCountry;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
}

package com.sisgic.repository;

import com.sisgic.entity.BookType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookTypeRepository extends JpaRepository<BookType, Long> {
    List<BookType> findAllByOrderByIdAsc();
}

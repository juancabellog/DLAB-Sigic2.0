package com.sisgic.repository;

import com.sisgic.entity.CategoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryEventRepository extends JpaRepository<CategoryEvent, String> {

    List<CategoryEvent> findAllByOrderByLabelAsc();
}

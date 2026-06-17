package com.sisgic.repository;

import com.sisgic.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    List<Tag> findAllByOrderByLabelAsc();
}

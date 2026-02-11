package com.sisgic.repository;

import com.sisgic.entity.TipoDifusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoDifusionRepository extends JpaRepository<TipoDifusion, Long> {
    List<TipoDifusion> findAllByOrderByIdAsc();
}











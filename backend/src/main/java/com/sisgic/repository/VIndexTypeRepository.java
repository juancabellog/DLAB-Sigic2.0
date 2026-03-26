package com.sisgic.repository;

import com.sisgic.entity.VIndexType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VIndexTypeRepository extends JpaRepository<VIndexType, Long> {
    List<VIndexType> findAllByOrderByIdAsc();
}


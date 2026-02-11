package com.sisgic.repository;

import com.sisgic.entity.GradoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradoAcademicoRepository extends JpaRepository<GradoAcademico, Long> {
    List<GradoAcademico> findAllByOrderByIdAsc();
}











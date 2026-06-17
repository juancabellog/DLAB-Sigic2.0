package com.sisgic.repository;

import com.sisgic.entity.ModalidadPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModalidadPresentacionRepository extends JpaRepository<ModalidadPresentacion, Long> {
    List<ModalidadPresentacion> findAllByOrderByIdAsc();
}

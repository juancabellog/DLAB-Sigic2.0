package com.sisgic.repository;

import com.sisgic.entity.ProductoProyecto;
import com.sisgic.entity.ProductoProyectoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoProyectoRepository extends JpaRepository<ProductoProyecto, ProductoProyectoId> {
}








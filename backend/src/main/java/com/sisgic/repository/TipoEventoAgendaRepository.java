package com.sisgic.repository;

import com.sisgic.entity.TipoEventoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoEventoAgendaRepository extends JpaRepository<TipoEventoAgenda, Long> {

    List<TipoEventoAgenda> findAllByOrderByIdAsc();

    Optional<TipoEventoAgenda> findByIdDescripcionIgnoreCase(String idDescripcion);
}

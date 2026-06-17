package com.sisgic.repository;

import com.sisgic.entity.TipoParticipacionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoParticipacionEventoRepository extends JpaRepository<TipoParticipacionEvento, Long> {
    List<TipoParticipacionEvento> findAllByOrderByIdAsc();
}

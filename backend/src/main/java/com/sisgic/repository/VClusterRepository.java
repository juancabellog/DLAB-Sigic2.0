package com.sisgic.repository;

import com.sisgic.entity.VCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VClusterRepository extends JpaRepository<VCluster, Long> {
    List<VCluster> findAllByOrderByIdAsc();
}


package com.sisgic.repository;

import com.sisgic.entity.FundingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundingTypeRepository extends JpaRepository<FundingType, Long> {
    Optional<FundingType> findByIdDescripcionIgnoreCase(String idDescripcion);

    /**
     * Lists funding types with "Other" (id=7) last.
     * Equivalent to: ORDER BY (id = 7) ASC, id ASC
     */
    @Query(value = "SELECT * FROM fundingtype ORDER BY (id = 7) ASC, id ASC", nativeQuery = true)
    List<FundingType> findAllWithOtherLast();
}

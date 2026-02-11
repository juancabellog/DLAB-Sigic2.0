package com.sisgic.repository;

import com.sisgic.entity.FundingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FundingTypeRepository extends JpaRepository<FundingType, Long> {
}











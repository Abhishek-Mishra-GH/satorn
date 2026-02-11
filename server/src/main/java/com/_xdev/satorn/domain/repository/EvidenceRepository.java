package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
}

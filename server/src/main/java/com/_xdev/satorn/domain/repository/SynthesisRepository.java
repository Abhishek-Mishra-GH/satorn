package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.Synthesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SynthesisRepository extends JpaRepository<Synthesis, Long> {
}

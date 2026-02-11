package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
  Optional<Category> findByName(String name);
}

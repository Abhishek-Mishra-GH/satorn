package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.LearnerProfile;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

  Optional<LearnerProfile> findByUser(User user);

  Optional<LearnerProfile> findByUser_Id(Long userId);
}

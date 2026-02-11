package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.RssFeedConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RssFeedConfigRepository extends JpaRepository<RssFeedConfig, Long> {

  List<RssFeedConfig> findByEnabled(Boolean enabled);

  List<RssFeedConfig> findByCategory(String category);

  Optional<RssFeedConfig> findByFeedUrl(String feedUrl);

  List<RssFeedConfig> findByConsecutiveFailuresLessThan(Integer threshold);
}

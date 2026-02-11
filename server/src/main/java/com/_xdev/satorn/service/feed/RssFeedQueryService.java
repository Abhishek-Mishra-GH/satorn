package com._xdev.satorn.service.feed;

import com._xdev.satorn.domain.entity.RssFeedConfig;
import com._xdev.satorn.domain.repository.RssFeedConfigRepository;
import com._xdev.satorn.service.feed.dto.RssFeedConfigDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RssFeedQueryService {

  private final RssFeedConfigRepository rssFeedConfigRepository;

  public List<RssFeedConfigDto> listFeeds() {
    return rssFeedConfigRepository.findAll()
        .stream()
        .map(this::mapToDto)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public RssFeedConfigDto getFeed(Long id) {
    RssFeedConfig feed = rssFeedConfigRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));
    return mapToDto(feed);
  }

  public Map<String, Object> getStatistics() {
    List<RssFeedConfig> allFeeds = rssFeedConfigRepository.findAll();
    List<RssFeedConfig> enabledFeeds = rssFeedConfigRepository.findByEnabled(true);
    long totalArticles = allFeeds.stream()
        .mapToLong(f -> f.getArticlesProcessed() != null ? f.getArticlesProcessed() : 0)
        .sum();
    long failedFeeds = allFeeds.stream()
        .filter(f -> f.getConsecutiveFailures() != null && f.getConsecutiveFailures() > 0)
        .count();

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalFeeds", allFeeds.size());
    stats.put("enabledFeeds", enabledFeeds.size());
    stats.put("disabledFeeds", allFeeds.size() - enabledFeeds.size());
    stats.put("totalArticlesProcessed", totalArticles);
    stats.put("feedsWithErrors", failedFeeds);
    return stats;
  }

  private RssFeedConfigDto mapToDto(RssFeedConfig feed) {
    return new RssFeedConfigDto(
        feed.getId(),
        feed.getName(),
        feed.getFeedUrl(),
        feed.getDescription(),
        feed.getCategory(),
        feed.getUpdateFrequencyMinutes(),
        formatDate(feed.getLastChecked()),
        feed.getEnabled(),
        feed.getArticlesProcessed(),
        feed.getLastError(),
        feed.getConsecutiveFailures(),
        formatDate(feed.getCreatedAt()),
        formatDate(feed.getUpdatedAt()));
  }

  private String formatDate(LocalDateTime value) {
    if (value == null) {
      return null;
    }
    return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}

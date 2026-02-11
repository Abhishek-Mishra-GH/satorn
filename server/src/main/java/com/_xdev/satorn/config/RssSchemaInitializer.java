package com._xdev.satorn.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures critical RSS schema exists even when Flyway is disabled.
 * This prevents runtime failures for admin RSS endpoints on partially initialized databases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssSchemaInitializer {

  private final JdbcTemplate jdbcTemplate;

  @Value("${rss.monitoring.bootstrap-schema:true}")
  private boolean bootstrapSchema;

  @PostConstruct
  public void ensureRssSchema() {
    if (!bootstrapSchema) {
      return;
    }
    try {
      jdbcTemplate.execute("""
          CREATE TABLE IF NOT EXISTS rss_feed_configs (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            feed_url VARCHAR(500) UNIQUE NOT NULL,
            description VARCHAR(1000),
            category VARCHAR(100) NOT NULL,
            update_frequency_minutes INTEGER DEFAULT 60,
            last_checked TIMESTAMP,
            enabled BOOLEAN DEFAULT true,
            articles_processed BIGINT DEFAULT 0,
            last_error TEXT,
            consecutive_failures INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rss_enabled ON rss_feed_configs(enabled)");
      jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rss_category ON rss_feed_configs(category)");
      jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_rss_feed_url_unique ON rss_feed_configs(feed_url)");

      seedDefaultFeeds();
      log.info("RSS schema bootstrap check completed");
    } catch (DataAccessException e) {
      log.warn("Could not bootstrap RSS schema (continuing without crash): {}", e.getMostSpecificCause().getMessage());
    }
  }

  private void seedDefaultFeeds() {
    insertFeed(
        "The Verge",
        "https://www.theverge.com/rss/index.xml",
        "The Verge technology and product news",
        "Technology",
        60);
    insertFeed(
        "NASA Breaking News",
        "https://www.nasa.gov/rss/dyn/breaking_news.rss",
        "Latest breaking news from NASA",
        "Science",
        60);
    insertFeed(
        "BBC News",
        "https://feeds.bbci.co.uk/news/rss.xml",
        "BBC top news RSS feed",
        "Politics",
        60);
    insertFeed(
        "The Hindu",
        "https://www.thehindu.com/feeder/default.rss",
        "The Hindu latest stories",
        "Politics",
        60);
    insertFeed(
        "Economic Times",
        "https://economictimes.indiatimes.com/rssfeedsdefault.cms",
        "Economic Times headlines",
        "Business",
        60);
    disableLegacyFeeds();
  }

  private void insertFeed(String name, String feedUrl, String description, String category, int frequencyMinutes) {
    jdbcTemplate.update("""
        INSERT INTO rss_feed_configs(name, feed_url, description, category, update_frequency_minutes, enabled)
        VALUES (?, ?, ?, ?, ?, true)
        ON CONFLICT (feed_url) DO UPDATE
        SET name = EXCLUDED.name,
            description = EXCLUDED.description,
            category = EXCLUDED.category,
            update_frequency_minutes = EXCLUDED.update_frequency_minutes,
            enabled = true,
            last_error = NULL,
            consecutive_failures = 0,
            updated_at = CURRENT_TIMESTAMP
        """, name, feedUrl, description, category, frequencyMinutes);
  }

  private void disableLegacyFeeds() {
    jdbcTemplate.update("""
        UPDATE rss_feed_configs
        SET enabled = false,
            last_error = 'Disabled by default feed refresh',
            updated_at = CURRENT_TIMESTAMP
        WHERE feed_url IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        "https://feeds.bbc.co.uk/news/world/rss.xml",
        "https://feeds.reuters.com/reuters/topNews",
        "http://rss.cnn.com/rss/edition_world.rss",
        "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
        "https://www.theguardian.com/world/rss",
        "http://feeds.techcrunch.com/TechCrunch/",
        "https://www.technologyreview.com/feed.rss",
        "https://feeds.healthline.com/healthline",
        "https://www.sciencedaily.com/rss/all.xml",
        "https://feeds.reuters.com/reuters/sportsNews");
  }
}

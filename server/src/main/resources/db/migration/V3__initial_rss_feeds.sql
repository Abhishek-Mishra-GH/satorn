-- SATORN v3.0 RSS Feed Initial Configuration

-- Insert initial RSS feeds for monitoring
INSERT INTO rss_feed_configs (name, feed_url, description, category, update_frequency_minutes, enabled) VALUES 
    ('The Verge', 'https://www.theverge.com/rss/index.xml', 'The Verge technology and product news', 'Technology', 60, true),
    ('NASA Breaking News', 'https://www.nasa.gov/rss/dyn/breaking_news.rss', 'Latest breaking news from NASA', 'Science', 60, true),
    ('BBC News', 'https://feeds.bbci.co.uk/news/rss.xml', 'BBC top news RSS feed', 'Politics', 60, true),
    ('The Hindu', 'https://www.thehindu.com/feeder/default.rss', 'The Hindu latest stories', 'Politics', 60, true),
    ('Economic Times', 'https://economictimes.indiatimes.com/rssfeedsdefault.cms', 'Economic Times headlines', 'Business', 60, true)
ON CONFLICT DO NOTHING;

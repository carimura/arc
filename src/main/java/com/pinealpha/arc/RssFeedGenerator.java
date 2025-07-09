package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates RSS 2.0 feed for blog posts.
 */
public class RssFeedGenerator {
    
    private static final DateTimeFormatter RFC_822_FORMATTER = 
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    
    private final FileProcessor fileProcessor;
    
    public RssFeedGenerator(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }
    
    /**
     * Generate RSS feed from posts and write to output directory
     * @param posts List of post metadata maps (already sorted by date)
     * @param siteDir Output site directory
     * @param siteConfig Site configuration (may be null for defaults)
     */
    public void generateFeed(List<Map<String, String>> posts, Path siteDir, 
                           Map<String, String> siteConfig) throws IOException {
        
        // Use config values or defaults
        String siteTitle = getConfigValue(siteConfig, "title", Constants.DEFAULT_SITE_TITLE);
        String siteDescription = getConfigValue(siteConfig, "description", Constants.DEFAULT_SITE_DESCRIPTION);
        String siteUrl = getConfigValue(siteConfig, "url", Constants.DEFAULT_SITE_URL);
        String siteLanguage = getConfigValue(siteConfig, "language", Constants.DEFAULT_SITE_LANGUAGE);
        int maxItems = getConfigIntValue(siteConfig, "rss_max_items", Constants.DEFAULT_RSS_MAX_ITEMS);
        
        // Build RSS XML
        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        rss.append("  <channel>\n");
        
        // Channel metadata
        rss.append("    <title>").append(escapeXml(siteTitle)).append("</title>\n");
        rss.append("    <link>").append(escapeXml(siteUrl)).append("</link>\n");
        rss.append("    <description>").append(escapeXml(siteDescription)).append("</description>\n");
        rss.append("    <language>").append(escapeXml(siteLanguage)).append("</language>\n");
        rss.append("    <lastBuildDate>").append(getCurrentRFC822Date()).append("</lastBuildDate>\n");
        rss.append("    <generator>Arc Static Site Generator</generator>\n");
        
        // Add posts (limited by maxItems)
        int itemCount = 0;
        for (Map<String, String> post : posts) {
            if (itemCount >= maxItems) break;
            
            String title = post.get("title");
            String date = post.get(Constants.DATE_VAR);
            String url = post.get(Constants.URL_VAR);
            String excerpt = post.get("excerpt");
            String content = post.get("rendered_content");
            
            if (title == null || date == null || url == null) {
                continue; // Skip posts missing required fields
            }
            
            // Convert relative URL to absolute
            String absoluteUrl = siteUrl + (url.startsWith("/") ? url : "/" + url);
            
            rss.append("\n    <item>\n");
            rss.append("      <title>").append(escapeXml(title)).append("</title>\n");
            rss.append("      <link>").append(escapeXml(absoluteUrl)).append("</link>\n");
            
            // Use excerpt if available, otherwise use a truncated version of content
            if (excerpt != null && !excerpt.isEmpty()) {
                rss.append("      <description>").append(escapeXml(excerpt)).append("</description>\n");
            } else if (content != null) {
                String description = truncateHtml(content, 200);
                rss.append("      <description>").append(escapeXml(description)).append("</description>\n");
            }
            
            rss.append("      <pubDate>").append(convertToRFC822(date)).append("</pubDate>\n");
            rss.append("      <guid isPermaLink=\"true\">").append(escapeXml(absoluteUrl)).append("</guid>\n");
            
            // Include full content in CDATA section
            if (content != null) {
                rss.append("      <content:encoded><![CDATA[").append(content).append("]]></content:encoded>\n");
            }
            
            rss.append("    </item>\n");
            itemCount++;
        }
        
        rss.append("  </channel>\n");
        rss.append("</rss>\n");
        
        // Write feed to file
        Path feedPath = siteDir.resolve(Constants.RSS_FEED_FILE);
        fileProcessor.writeFile(feedPath, rss.toString());
        
        System.out.println("Generated RSS feed: " + siteDir.relativize(feedPath));
        System.out.println("  - Included " + itemCount + " posts");
        
        // Show config recommendation if using defaults
        if (siteConfig == null) {
            System.out.println("\n📝 TIP: Create an app/" + Constants.SITE_CONFIG_FILE + " file to customize your RSS feed:");
            System.out.println("---");
            System.out.println("title: " + siteTitle);
            System.out.println("description: " + siteDescription);
            System.out.println("url: " + siteUrl);
            System.out.println("language: " + siteLanguage);
            System.out.println("rss_max_items: " + maxItems);
            System.out.println("---\n");
        }
    }
    
    /**
     * Get configuration value or return default
     */
    private String getConfigValue(Map<String, String> config, String key, String defaultValue) {
        if (config != null && config.containsKey(key)) {
            return config.get(key);
        }
        return defaultValue;
    }
    
    /**
     * Get integer configuration value or return default
     */
    private int getConfigIntValue(Map<String, String> config, String key, int defaultValue) {
        if (config != null && config.containsKey(key)) {
            try {
                return Integer.parseInt(config.get(key));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
    
    /**
     * Convert ISO date to RFC 822 format for RSS
     */
    private String convertToRFC822(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            ZonedDateTime zdt = date.atStartOfDay(ZoneOffset.UTC);
            return zdt.format(RFC_822_FORMATTER);
        } catch (Exception e) {
            // If parsing fails, return current date
            return getCurrentRFC822Date();
        }
    }
    
    /**
     * Get current date in RFC 822 format
     */
    private String getCurrentRFC822Date() {
        return ZonedDateTime.now().format(RFC_822_FORMATTER);
    }
    
    /**
     * Escape special XML characters
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    /**
     * Truncate HTML content to approximately maxLength characters
     * Strips HTML tags and truncates at word boundary
     */
    private String truncateHtml(String html, int maxLength) {
        // Simple HTML tag removal
        String text = html.replaceAll("<[^>]+>", "").trim();
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        // Truncate at word boundary
        int lastSpace = text.lastIndexOf(' ', maxLength);
        if (lastSpace > 0) {
            return text.substring(0, lastSpace) + "...";
        }
        
        return text.substring(0, maxLength) + "...";
    }
} 
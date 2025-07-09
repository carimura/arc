package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Processes all content (pages and posts) with unified logic.
 * Handles metadata extraction, URL generation, and content-type specific processing.
 */
public class PageProcessor {
    
    private final FrontmatterParser frontmatterParser;
    private final FileProcessor fileProcessor;
    private final TemplateEngine templateEngine;
    private final RssGenerator rssGenerator;
    
    public PageProcessor(FrontmatterParser frontmatterParser, FileProcessor fileProcessor, 
                        TemplateEngine templateEngine, RssGenerator rssGenerator) {
        this.frontmatterParser = frontmatterParser;
        this.fileProcessor = fileProcessor;
        this.templateEngine = templateEngine;
        this.rssGenerator = rssGenerator;
    }
    
    /**
     * Process all markdown files from both posts and pages directories
     * @param appDir Application directory
     * @param siteDir Output site directory
     */
    public void processAllContent(Path appDir, Path siteDir) throws IOException {
        List<ContentItem>           allContent = new ArrayList<>();
        List<Map<String, String>>   posts = new ArrayList<>();
        
        // Load site configuration if it exists
        Map<String, String> siteConfig = loadSiteConfig(appDir);
        
        // Process posts and pages
        Map<String, String> contentDirs = Map.of(
            Constants.POSTS_DIR, "posts",
            Constants.PAGES_DIR, "pages"
        );
        for (Map.Entry<String, String> entry : contentDirs.entrySet()) {
            Path dir = appDir.resolve(entry.getKey());
            if (Files.exists(dir)) {
                List<Path> files = fileProcessor.findMarkdownFiles(dir);
                System.out.println("Found " + files.size() + " markdown " + entry.getValue() + " to process");
                for (Path file : files) {
                    allContent.add(processFile(file, appDir));
                }
            }
        }
        // Identify and sort posts
        for (ContentItem item : allContent) {
            if (item.metadata.get("type").equals("post")) {
                posts.add(item.metadata);
            }
        }
        posts.sort(new PostDateComparator());
        
        // Register global template variables
        templateEngine.registerGlobalVariable(Constants.POSTS_VAR, posts);
        if (!posts.isEmpty()) {
            templateEngine.registerGlobalVariable(Constants.LATEST_POST_VAR, posts.get(0));
        } else {
            templateEngine.registerGlobalVariable(Constants.LATEST_POST_VAR, null);
        }
        
        // Generate HTML for all content
        for (ContentItem item : allContent) {
            generateHtml(item, appDir, siteDir);
        }
        
        // Generate RSS feed for posts
        if (!posts.isEmpty()) {
            rssGenerator.generateFeed(posts, siteDir, siteConfig);
        }
    }
    
    /**
     * Load site configuration from site.config file if it exists
     */
    private Map<String, String> loadSiteConfig(Path appDir) throws IOException {
        Path configPath = appDir.resolve(Constants.SITE_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return null;
        }
        
        String configContent = Files.readString(configPath);
        String frontmatter = frontmatterParser.extractFrontmatter(configContent);
        Map<String, String> config = frontmatterParser.parse(frontmatter);
        
        System.out.println("Loaded site configuration from: " + Constants.SITE_CONFIG_FILE);
        return config;
    }
    
    private ContentItem processFile(Path file, Path appDir) throws IOException {
        String content = Files.readString(file);
        String frontmatter = frontmatterParser.extractFrontmatter(content);
        String markdownContent = frontmatterParser.extractContent(content);
        
        Map<String, String> metadata = new HashMap<>(frontmatterParser.parse(frontmatter));
        
        // Add computed fields
        metadata.put(Constants.URL_VAR, generateUrl(file, appDir));
        metadata.put("content", markdownContent);
        metadata.put("rendered_content", convertMarkdownToHtml(markdownContent));
        
        // Format date if present
        String date = metadata.get(Constants.DATE_VAR);
        if (date != null) {
            metadata.put("formatted_date", formatDateForDisplay(date));
        }
        
        return new ContentItem(file, metadata, markdownContent);
    }
    
    private void generateHtml(ContentItem item, Path appDir, Path siteDir) throws IOException {
        String htmlContent = item.metadata.get("rendered_content");
        
        // Get template name from frontmatter
        String templateName = item.metadata.get(Constants.TEMPLATE_VAR);
        if (templateName == null) {
            throw new IllegalArgumentException("No template specified in frontmatter for: " + item.file);
        }
        
        // Load and process template
        Path templatesDir = appDir.resolve(Constants.TEMPLATES_DIR);
        Path templatePath = templatesDir.resolve(templateName);
        
        if (!Files.exists(templatePath)) {
            throw new IOException("Template not found: " + templatePath);
        }
        
        String template = Files.readString(templatePath);
        String finalHtml = templateEngine.processTemplate(template, item.metadata, htmlContent, templatesDir);
        
        // Write output
        Path outputPath = fileProcessor.determineOutputPath(item.file, appDir, siteDir);
        fileProcessor.writeFile(outputPath, finalHtml);
        
        System.out.println("Generated: " + siteDir.relativize(outputPath));
    }
    
    private String generateUrl(Path file, Path appDir) {
        return "/" + appDir.relativize(file)
            .toString()
            .replace("\\", "/")
            .replace(".md", ".html");
    }
    
    /**
     * Format date from ISO format (2025-05-28) to readable format (May 28th, 2025)
     */
    private String formatDateForDisplay(String isoDate) {
        LocalDate date = LocalDate.parse(isoDate);
        String month = date.format(DateTimeFormatter.ofPattern("MMMM"));
        String dayWithSuffix = date.getDayOfMonth() + getOrdinalSuffix(date.getDayOfMonth());
        int year = date.getYear();
        return String.format("%s %s, %d", month, dayWithSuffix, year);
    }
    
    private String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
    
    /**
     * Convert markdown text to HTML using CommonMark
     */
    private String convertMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
    
    /**
     * Container for processed content
     */
    private static class ContentItem {
        final Path file;
        final Map<String, String> metadata;
        final String markdownContent;
        
        ContentItem(Path file, Map<String, String> metadata, String markdownContent) {
            this.file = file;
            this.metadata = metadata;
            this.markdownContent = markdownContent;
        }
    }
    
    /**
     * Comparator for sorting posts by date in reverse chronological order
     */
    private static class PostDateComparator implements Comparator<Map<String, String>> {
        @Override
        public int compare(Map<String, String> post1, Map<String, String> post2) {
            String date1 = post1.get(Constants.DATE_VAR);
            String date2 = post2.get(Constants.DATE_VAR);
            
            // Handle missing dates
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            
            try {
                LocalDate localDate1 = LocalDate.parse(date1);
                LocalDate localDate2 = LocalDate.parse(date2);
                return localDate2.compareTo(localDate1); // Reverse order
            } catch (DateTimeParseException e) {
                // Fall back to string comparison if date parsing fails
                return date2.compareTo(date1);
            }
        }
    }
} 
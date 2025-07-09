package com.pinealpha.arc;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for YAML-style frontmatter in Markdown files.
 * Extracts metadata from the frontmatter section and separates it from content.
 */
public class FrontmatterParser {
    
    /**
     * Parse frontmatter string into a map of key-value pairs
     * @param frontmatter The frontmatter content (without delimiters)
     * @return Map of parsed frontmatter variables
     */
    public Map<String, String> parse(String frontmatter) {
        Map<String, String> result = new HashMap<>();
        
        if (frontmatter == null || frontmatter.isEmpty()) {
            return Map.copyOf(result);
        }
        
        frontmatter.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> line.contains(":"))
            .forEach(line -> parseLine(line, result));
        
        return Map.copyOf(result);
    }
    
    /**
     * Parse a single frontmatter line
     */
    private void parseLine(String line, Map<String, String> result) {
        String[] parts = line.split(":", 2);
        if (parts.length != 2) {
            return;
        }
        
        String key = parts[0].trim();
        String value = parts[1].trim();
        
        // Remove surrounding quotes if present
        value = removeQuotes(value);
        
        result.put(key, value);
    }
    
    /**
     * Remove surrounding quotes from a value
     */
    private String removeQuotes(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\""))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
    
    /**
     * Extract frontmatter from content
     * @param content The full content including frontmatter
     * @return The frontmatter content without delimiters, or empty string if none
     */
    public String extractFrontmatter(String content) {
        if (!content.startsWith("---\n") && !content.startsWith("---\r\n")) {
            return "";
        }
        
        // Look for the end of frontmatter
        int endIndex = content.indexOf("\n---\n");
        if (endIndex == -1) {
            endIndex = content.indexOf("\n---\r\n");
        }
        
        // Handle case where file only contains frontmatter (ends with --- but no content after)
        if (endIndex == -1) {
            // Check if the content ends with ---
            String trimmed = content.trim();
            if (trimmed.endsWith("---")) {
                // Find the last occurrence of --- that's on its own line
                int lastDashIndex = trimmed.lastIndexOf("\n---");
                if (lastDashIndex > 0) {
                    return content.substring("---\n".length(), lastDashIndex);
                }
            }
            return "";
        }
        
        return content.substring(
            "---\n".length(),
            endIndex
        );
    }
    
    /**
     * Extract the content portion (without frontmatter)
     * @param content The full content including frontmatter
     * @return The content without frontmatter
     */
    public String extractContent(String content) {
        if (!content.startsWith("---\n")) {
            return content;
        }
        
        int endIndex = content.indexOf("\n---\n");
        if (endIndex == -1) {
            return content;
        }
        
        return content.substring(
            endIndex + "\n---\n".length()
        );
    }
} 
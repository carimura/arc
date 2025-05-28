package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template engine for processing Arc templates.
 * Handles variable substitution, conditionals, loops, and includes.
 */
public class TemplateEngine {
    
    private final Map<String, Object> globalVariables = new HashMap<>();
    private final Pattern includePattern = Pattern.compile(Constants.INCLUDE_PATTERN);
    private final Pattern ifPattern = Pattern.compile(Constants.IF_PATTERN, Pattern.DOTALL);
    private final Pattern forPattern = Pattern.compile(Constants.FOR_PATTERN, Pattern.DOTALL);
    
    /**
     * Register a global variable that will be available in all templates
     * @param name Variable name
     * @param value Variable value
     */
    public void registerGlobalVariable(String name, Object value) {
        globalVariables.put(name, value);
    }
    
    /**
     * Process a template with the given variables
     * @param template The template content
     * @param pageVariables Page-specific variables
     * @param content The main content to inject
     * @param templatesDir Directory containing template files
     * @return The processed template
     */
    public String processTemplate(String template, Map<String, String> pageVariables, 
                                  String content, Path templatesDir) throws IOException {
        // Combine global and page-specific variables
        Map<String, Object> allVariables = new HashMap<>(globalVariables);
        allVariables.putAll(pageVariables);
        allVariables.put(Constants.CONTENT_VAR, content);
        
        // Process in order: includes -> loops -> conditionals -> variables
        String result = processIncludes(template, templatesDir);
        result = processLoops(result, allVariables);
        result = processConditionals(result, allVariables);
        result = processVariables(result, allVariables);
        
        return result;
    }
    
    /**
     * Process include directives in the template
     */
    private String processIncludes(String content, Path templatesDir) throws IOException {
        Matcher matcher = includePattern.matcher(content);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String includeFile = matcher.group(1);
            Path includePath = templatesDir.resolve(includeFile);
            
            if (!Files.exists(includePath)) {
                throw new IOException("Include file not found: " + includePath);
            }
            
            String includeContent = Files.readString(includePath);
            // Recursively process includes in the included file
            String processedContent = processIncludes(includeContent, templatesDir);
            matcher.appendReplacement(result, Matcher.quoteReplacement(processedContent));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processConditionals(String content, Map<String, Object> variables) {
        Matcher matcher = ifPattern.matcher(content);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String ifContent = matcher.group(2);
            
            boolean conditionMet = evaluateCondition(condition, variables);
            String replacement = conditionMet ? ifContent : "";
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            if (parts.length == 2) {
                String leftSide = parts[0].trim();
                String rightSide = parts[1].trim();
                
                // Remove quotes from right side if present (handle both single and double quotes)
                if ((rightSide.startsWith("'") && rightSide.endsWith("'")) ||
                    (rightSide.startsWith("\"") && rightSide.endsWith("\""))) {
                    rightSide = rightSide.substring(1, rightSide.length() - 1);
                }
                
                Object leftValue = getVariableValue(leftSide, variables);
                boolean result = leftValue != null && leftValue.toString().equals(rightSide);
                return result;
            }
        }
        
        // Fall back to simple existence check
        Object value = getVariableValue(condition, variables);
        return value != null && !value.toString().isEmpty();
    }
    
    private String processLoops(String template, Map<String, Object> variables) {
        StringBuilder result = new StringBuilder(template);
        Matcher matcher = forPattern.matcher(template);
        
        while (matcher.find()) {
            String itemVar = matcher.group(1);
            String collectionName = matcher.group(2);
            String loopContent = matcher.group(3);
            
            Object collection = variables.get(collectionName);
            StringBuilder loopResult = new StringBuilder();
            
            if (collection instanceof List<?> items) {
                for (Object item : items) {
                    Map<String, Object> loopVariables = new HashMap<>(variables);
                    loopVariables.put(itemVar, item);
                    
                    String itemContent = processConditionals(loopContent, loopVariables);
                    itemContent = processLoopVariables(itemContent, itemVar, item);
                    loopResult.append(itemContent);
                }
            }
            
            int start = matcher.start();
            int end = matcher.end();
            result.replace(start, end, loopResult.toString());
            matcher = forPattern.matcher(result);
        }
        
        return result.toString();
    }
    
    private String processLoopVariables(String content, String itemVar, Object item) {
        if (item instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                String key = entry.getKey().toString();
                String placeholder = String.format(Constants.VARIABLE_PATTERN, 
                    itemVar + "\\." + key);
                String replacement = entry.getValue() != null ? 
                    entry.getValue().toString() : "";
                content = content.replaceAll(placeholder, 
                    Matcher.quoteReplacement(replacement));
            }
        }
        return content;
    }
    
    private String processVariables(String template, Map<String, Object> variables) {
        String result = template;
        
        // First, handle nested properties (variables with dots)
        Pattern nestedPattern = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z0-9_.]+)\\s*\\}\\}");
        Matcher nestedMatcher = nestedPattern.matcher(result);
        
        while (nestedMatcher.find()) {
            String fullPath = nestedMatcher.group(1);
            Object value = getVariableValue(fullPath, variables);
            String replacement = value != null ? value.toString() : "";
            result = result.replace(nestedMatcher.group(0), replacement);
            nestedMatcher = nestedPattern.matcher(result); // Reset matcher after replacement
        }
        
        // Then handle simple variables (without dots)
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = String.format(Constants.VARIABLE_PATTERN, entry.getKey());
            String replacement = entry.getValue() != null ? 
                entry.getValue().toString() : "";
            result = result.replaceAll(placeholder, 
                Matcher.quoteReplacement(replacement));
        }
        
        return result;
    }
    
    private Object getVariableValue(String path, Map<String, Object> variables) {
        if (variables.containsKey(path)) {
            return variables.get(path);
        }
        
        // Handle nested properties
        String[] parts = path.split("\\.");
        Object current = variables;
        
        for (String part : parts) {
            if (!(current instanceof Map<?, ?>)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
} 
package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles file system operations for the Arc static site generator.
 * Responsible for finding files, copying assets, and managing output directories.
 */
public class FileProcessor {
    
    /**
     * Find all Markdown files in a directory tree
     * @param directory The root directory to search
     * @return List of paths to Markdown files
     */
    public List<Path> findMarkdownFiles(Path directory) throws IOException {
        List<Path> markdownFiles = new ArrayList<>();
        
        if (!Files.exists(directory)) {
            return markdownFiles;
        }
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".md")) {
                    markdownFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                // Skip hidden directories and the site output directory
                if (dirName.startsWith(".") || dirName.equals(Constants.SITE_DIR)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return markdownFiles;
    }
    
    /**
     * Copy assets directory from source to destination
     * @param sourceDir The source assets directory
     * @param targetDir The target assets directory
     */
    public void copyAssets(Path sourceDir, Path targetDir) throws IOException {
        Path sourceAssets = sourceDir.resolve(Constants.ASSETS_DIR);
        Path targetAssets = targetDir.resolve(Constants.ASSETS_DIR);
        
        if (!Files.exists(sourceAssets)) {
            return;
        }
        
        Files.walkFileTree(sourceAssets, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) 
                    throws IOException {
                Path targetSubDir = targetAssets.resolve(sourceAssets.relativize(dir));
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
                    throws IOException {
                Path targetFile = targetAssets.resolve(sourceAssets.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        
        System.out.println("Copied assets to: " + targetAssets);
    }
    
    /**
     * Determine the output path for a processed file
     * @param sourceFile The source Markdown file
     * @param appDir The application directory
     * @param siteDir The site output directory
     * @return The path where the HTML file should be written
     */
    public Path determineOutputPath(Path sourceFile, Path appDir, Path siteDir) {
        String fileName = sourceFile.getFileName().toString()
            .replace(".md", ".html");
        
        // Check if file is in the pages directory - these go to site root
        if (sourceFile.getParent() != null && 
            sourceFile.getParent().endsWith(Constants.PAGES_DIR)) {
            return siteDir.resolve(fileName);
        }
        
        // For other files, maintain directory structure
        Path relativePath = appDir.relativize(sourceFile);
        Path outputDir = siteDir.resolve(relativePath).getParent();
        return outputDir != null ? outputDir.resolve(fileName) : siteDir.resolve(fileName);
    }
    
    /**
     * Write content to a file, creating directories as needed
     * @param outputPath The path to write to
     * @param content The content to write
     */
    public void writeFile(Path outputPath, String content) throws IOException {
        Path parentDir = outputPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.writeString(outputPath, content);
    }
    
    /**
     * Create a directory if it doesn't exist
     * @param directory The directory to create
     */
    public void createDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
} 
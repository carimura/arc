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
     * Copy every file under app/root/ to the site root, preserving subdirectories.
     * Intended for static files that must live at the site root (robots.txt,
     * favicon.ico, CNAME, etc.). No-op if app/root/ does not exist.
     */
    public void copyRootFiles(Path appDir, Path siteDir) throws IOException {
        Path sourceRoot = appDir.resolve(Constants.ROOT_DIR);
        if (!Files.exists(sourceRoot)) {
            return;
        }

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetSubDir = siteDir.resolve(sourceRoot.relativize(dir));
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = siteDir.resolve(sourceRoot.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Copied root files from: " + sourceRoot);
    }

    /**
     * Determine the output path for a processed file
     * @param sourceFile The source Markdown file
     * @param appDir The application directory
     * @param siteDir The site output directory
     * @return The path where the HTML file should be written
     */
    public Path determineOutputPath(Path sourceFile, Path appDir, Path siteDir) {
        return determineOutputPath(sourceFile, appDir, siteDir, null);
    }

    public Path determineOutputPath(Path sourceFile, Path appDir, Path siteDir, String contentType) {
        String fileName = sourceFile.getFileName().toString()
            .replace(".md", ".html");
        
        // Check if file is in the pages directory - these go to site root by default.
        if (sourceFile.getParent() != null && 
            sourceFile.getParent().endsWith(Constants.PAGES_DIR) &&
            (contentType == null || contentType.isBlank() || Constants.PAGE_TYPE.equals(contentType))) {
            return siteDir.resolve(fileName);
        }

        if (Constants.POST_TYPE.equals(contentType)) {
            return siteDir.resolve(Constants.POSTS_DIR).resolve(fileName);
        }

        if (contentType != null && !contentType.isBlank() && !Constants.PAGE_TYPE.equals(contentType)) {
            return siteDir.resolve(contentType).resolve(fileName);
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

package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Hot reload watcher for Arc that monitors file changes and triggers rebuilds.
 */
public class HotReloadWatcher {
    
    private final Path watchDir;
    private final Runnable onChangeCallback;
    private volatile boolean running = true;
    

    
    public HotReloadWatcher(Path watchDir, Runnable onChangeCallback) {
        this.watchDir = watchDir;
        this.onChangeCallback = onChangeCallback;
    }
    
    /**
     * Start watching for file changes
     */
    public void watch() throws IOException, InterruptedException {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Register the directory and subdirectories
            registerRecursive(watchDir, watchService);
            
            System.out.println("\n🔥 Arc Hot Reload Started");
            System.out.println("📁 Watching: " + watchDir);
            System.out.println("🔄 Press Ctrl+C to stop\n");
            
            onChangeCallback.run();
            
            while (running) {
                WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
                if (key == null) continue;
                
                boolean shouldRebuild = false;
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;
                    
                    Path changed = (Path) event.context();
                    String fileName = changed.toString();
                    
                    // Check if it's a file we care about
                    if (fileName.endsWith(".md") || fileName.endsWith(".html") || 
                        fileName.endsWith(".css") || fileName.endsWith(".js")) {
                        shouldRebuild = true;
                        System.out.println("\n\n ⚡️ CHANGE DETECTED AT " + LocalTime.now().format(Constants.TIME_FORMAT) + " --> " + changed + " ⚡️\n\n");
                    }
                                            

                    // If a directory was created, register it
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path child = ((Path) key.watchable()).resolve(changed);
                        if (Files.isDirectory(child)) {
                            registerRecursive(child, watchService);
                        }
                    }
                }
                
                key.reset();
                
                if (shouldRebuild) {
                    onChangeCallback.run();
                }
            }
        }
    }
    
    /**
     * Register a directory and all its subdirectories
     */
    private void registerRecursive(Path dir, WatchService watchService) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip hidden directories and the site output directory
                String dirName = dir.getFileName().toString();
                if (dirName.startsWith(".") || dirName.equals(Constants.SITE_DIR)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                dir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public void stop() {
        running = false;
    }
} 
package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for the Arc static site generator.
 * Orchestrates the site generation process.
 */
public class Arc {
    private final FileProcessor fileProcessor;
    private final PageProcessor pageProcessor;
    
    private final Path currentDir;
    private final Path appDir;
    private final Path siteDir;
    
    public Arc() {
        this.fileProcessor = new FileProcessor();
        FrontmatterParser frontmatterParser = new FrontmatterParser();
        TemplateEngine templateEngine = new TemplateEngine();
        RssGenerator rssGenerator = new RssGenerator(fileProcessor);
        this.pageProcessor = new PageProcessor(frontmatterParser, fileProcessor, templateEngine, rssGenerator);
        this.currentDir = Paths.get("");
        this.appDir = currentDir.resolve(Constants.APP_DIR);
        this.siteDir = currentDir.resolve(Constants.SITE_DIR);
    }
    
    /**
     * Main method to run the site generation process
     */
    public static void main(String[] args) throws Exception {
        Arc arc = new Arc();
        
        // Check for watch mode
        boolean watchMode = args.length > 0 && "--watch".equals(args[0]);
        
        if (watchMode) {
            arc.runWatchMode();
        } else {
            arc.generate();
        }
    }
    
    /**
     * Run in watch mode - monitors files and rebuilds on changes
     */
    private void runWatchMode() throws IOException, InterruptedException {
        HotReloadWatcher watcher = new HotReloadWatcher(appDir, () -> {
            try {
                generate();
            } catch (IOException e) {
                System.err.println("Error during rebuild: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // Add shutdown hook to handle Ctrl+C gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping hot reload watcher...");
            watcher.stop();
        }));
        
        watcher.watch();
    }
    
    public void generate() throws IOException {
        System.out.println("-------- STARTING ARC GENERATE() --------");
        
        fileProcessor.createDirectory(siteDir);
        fileProcessor.copyAssets(appDir, siteDir);
        pageProcessor.processAllContent(appDir, siteDir);
        
        System.out.println("-------- SITE GENERATION COMPLETE --------");
    }
}

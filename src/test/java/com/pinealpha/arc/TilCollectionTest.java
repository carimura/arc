// ABOUTME: Verifies that posts with type "til" are collected into a separate tils template variable
// ABOUTME: Run via: javac -d /tmp/arc-test -cp target/classes src/test/java/com/pinealpha/arc/TilCollectionTest.java && java -cp target/classes:/tmp/arc-test com.pinealpha.arc.TilCollectionTest
package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TilCollectionTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTilsAvailableInTemplate();
        testTilsSortedReverseChronological();
        testTilsAndPostsSeparate();

        if (failures > 0) {
            System.err.println(failures + " test(s) failed");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }

    static void testTilsAvailableInTemplate() throws Exception {
        Path tmpDir = createTestSite(List.of(
            new TestPost("2026-04-07-first-til.md", "First TIL", "2026-04-07", "til", "First TIL content"),
            new TestPost("2026-04-08-second-til.md", "Second TIL", "2026-04-08", "til", "Second TIL content")
        ));

        try {
            // Create a page template that loops over tils
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for til in tils %}[{{ til.title }}]{% endfor %}");

            // Create a page that uses the template
            Path pagesDir = tmpDir.resolve("app/pages");
            Files.createDirectories(pagesDir);
            Files.writeString(pagesDir.resolve("playground.md"),
                "---\ntitle: Playground\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/playground.html"));
            if (!output.contains("[First TIL]") || !output.contains("[Second TIL]")) {
                fail("testTilsAvailableInTemplate",
                    "Expected both TILs in output, got: " + output);
            } else {
                pass("testTilsAvailableInTemplate");
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testTilsSortedReverseChronological() throws Exception {
        Path tmpDir = createTestSite(List.of(
            new TestPost("2026-04-05-oldest.md", "Oldest", "2026-04-05", "til", "content"),
            new TestPost("2026-04-08-newest.md", "Newest", "2026-04-08", "til", "content"),
            new TestPost("2026-04-06-middle.md", "Middle", "2026-04-06", "til", "content")
        ));

        try {
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for til in tils %}[{{ til.title }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.createDirectories(pagesDir);
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            int newestIdx = output.indexOf("[Newest]");
            int middleIdx = output.indexOf("[Middle]");
            int oldestIdx = output.indexOf("[Oldest]");

            if (newestIdx < 0 || middleIdx < 0 || oldestIdx < 0) {
                fail("testTilsSortedReverseChronological",
                    "Missing TIL entries in output: " + output);
            } else if (newestIdx < middleIdx && middleIdx < oldestIdx) {
                pass("testTilsSortedReverseChronological");
            } else {
                fail("testTilsSortedReverseChronological",
                    "TILs not in reverse chronological order: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testTilsAndPostsSeparate() throws Exception {
        Path tmpDir = createTestSite(List.of(
            new TestPost("2026-04-07-a-til.md", "A TIL", "2026-04-07", "til", "til content"),
            new TestPost("2026-04-07-a-post.md", "A Post", "2026-04-07", "post", "post content")
        ));

        try {
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "TILS:{% for til in tils %}[{{ til.title }}]{% endfor %}" +
                "POSTS:{% for post in posts %}[{{ post.title }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.createDirectories(pagesDir);
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            String tilsSection = output.substring(output.indexOf("TILS:"), output.indexOf("POSTS:"));
            String postsSection = output.substring(output.indexOf("POSTS:"));

            boolean tilCorrect = tilsSection.contains("[A TIL]") && !tilsSection.contains("[A Post]");
            boolean postCorrect = postsSection.contains("[A Post]") && !postsSection.contains("[A TIL]");

            if (tilCorrect && postCorrect) {
                pass("testTilsAndPostsSeparate");
            } else {
                fail("testTilsAndPostsSeparate",
                    "TILs and posts should be in separate collections. Output: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    // --- helpers ---

    record TestPost(String filename, String title, String date, String type, String content) {}

    static Path createTestSite(List<TestPost> posts) throws IOException {
        Path tmpDir = Files.createTempDirectory("arc-til-test");
        Path postsDir = tmpDir.resolve("app/posts");
        Path templatesDir = tmpDir.resolve("app/templates");
        Path siteDir = tmpDir.resolve("site");
        Files.createDirectories(postsDir);
        Files.createDirectories(templatesDir);
        Files.createDirectories(siteDir);

        // Create a minimal post template
        Files.writeString(templatesDir.resolve("post.html"), "{{ content }}");

        for (TestPost post : posts) {
            String frontmatter = String.format(
                "---\ntitle: \"%s\"\ndate: %s\ntype: %s\ntemplate: post.html\n---\n%s\n",
                post.title, post.date, post.type, post.content);
            Files.writeString(postsDir.resolve(post.filename), frontmatter);
        }

        return tmpDir;
    }

    static void buildSite(Path tmpDir) throws IOException {
        FrontmatterParser parser = new FrontmatterParser();
        FileProcessor fileProcessor = new FileProcessor();
        TemplateEngine templateEngine = new TemplateEngine();
        RssGenerator rssGenerator = new RssGenerator(fileProcessor);
        PageProcessor processor = new PageProcessor(parser, fileProcessor, templateEngine, rssGenerator);
        processor.processAllContent(tmpDir.resolve("app"), tmpDir.resolve("site"));
    }

    static void pass(String testName) {
        System.out.println("PASS: " + testName);
    }

    static void fail(String testName, String message) {
        System.err.println("FAIL: " + testName + " - " + message);
        failures++;
    }

    static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursive(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}

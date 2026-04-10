// ABOUTME: Verifies that any frontmatter type automatically becomes a template collection
// ABOUTME: Run via: mvn package, then mvn exec:java -Dexec.mainClass=com.pinealpha.arc.GeneralizedCollectionTest -Dexec.classpathScope=test
package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class GeneralizedCollectionTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testHyphenatedTypeBecomesUnderscoredPluralCollection();
        testMultipleArbitraryTypesEachGetCollection();

        if (failures > 0) {
            System.err.println(failures + " test(s) failed");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }

    static void testHyphenatedTypeBecomesUnderscoredPluralCollection() throws Exception {
        Path tmpDir = createTestSite(List.of(
            new TestPost("2026-04-08-first.md", "First", "2026-04-08", "x-post", "body"),
            new TestPost("2026-04-09-second.md", "Second", "2026-04-09", "x-post", "body")
        ));

        try {
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for p in x_posts %}[{{ p.title }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.createDirectories(pagesDir);
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("[First]") && output.contains("[Second]")) {
                pass("testHyphenatedTypeBecomesUnderscoredPluralCollection");
            } else {
                fail("testHyphenatedTypeBecomesUnderscoredPluralCollection",
                    "Expected both x-posts in x_posts collection, got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testMultipleArbitraryTypesEachGetCollection() throws Exception {
        Path tmpDir = createTestSite(List.of(
            new TestPost("2026-04-01-event.md", "Conf", "2026-04-01", "event", "body"),
            new TestPost("2026-04-02-project.md", "ArcSSG", "2026-04-02", "project", "body")
        ));

        try {
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "E:{% for e in events %}[{{ e.title }}]{% endfor %}|" +
                "P:{% for p in projects %}[{{ p.title }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.createDirectories(pagesDir);
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("E:[Conf]") && output.contains("P:[ArcSSG]")) {
                pass("testMultipleArbitraryTypesEachGetCollection");
            } else {
                fail("testMultipleArbitraryTypesEachGetCollection",
                    "Expected events and projects collections, got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    // --- helpers ---

    record TestPost(String filename, String title, String date, String type, String content) {}

    static Path createTestSite(List<TestPost> posts) throws IOException {
        Path tmpDir = Files.createTempDirectory("arc-generalized-test");
        Path postsDir = tmpDir.resolve("app/posts");
        Path templatesDir = tmpDir.resolve("app/templates");
        Path siteDir = tmpDir.resolve("site");
        Files.createDirectories(postsDir);
        Files.createDirectories(templatesDir);
        Files.createDirectories(siteDir);

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

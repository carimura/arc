// ABOUTME: Verifies that JSON files in app/data are loaded as global template variables
// ABOUTME: Run via: mvn package, then mvn exec:java -Dexec.mainClass=com.pinealpha.arc.DataFileTest -Dexec.classpathScope=test
package com.pinealpha.arc;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DataFileTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testJsonArrayLoadedAsListVariable();
        testJsonObjectLoadedAsMapVariable();
        testHyphenInFilenameBecomesUnderscore();
        testMissingDataDirIsFine();
        testEmptyDataDirIsFine();
        testInvalidJsonThrowsClearError();
        testNonJsonFilesIgnored();
        testMultipleDataFilesCoexist();

        if (failures > 0) {
            System.err.println(failures + " test(s) failed");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }

    static void testJsonArrayLoadedAsListVariable() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("links.json"),
                "[\n" +
                "  {\"name\": \"First\", \"url\": \"/first\"},\n" +
                "  {\"name\": \"Second\", \"url\": \"/second\"}\n" +
                "]\n");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for link in links %}[{{ link.name }}->{{ link.url }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("[First->/first]") && output.contains("[Second->/second]")) {
                pass("testJsonArrayLoadedAsListVariable");
            } else {
                fail("testJsonArrayLoadedAsListVariable",
                    "Expected both link entries in output, got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testJsonObjectLoadedAsMapVariable() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("settings.json"),
                "{\"theme\": \"dark\", \"site_title\": \"My Site\"}\n");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "theme={{ settings.theme }};title={{ settings.site_title }}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("theme=dark") && output.contains("title=My Site")) {
                pass("testJsonObjectLoadedAsMapVariable");
            } else {
                fail("testJsonObjectLoadedAsMapVariable",
                    "Expected dark/My Site in output, got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testHyphenInFilenameBecomesUnderscore() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("x-posts.json"),
                "[{\"text\": \"hello\"}]\n");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for p in x_posts %}[{{ p.text }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("[hello]")) {
                pass("testHyphenInFilenameBecomesUnderscore");
            } else {
                fail("testHyphenInFilenameBecomesUnderscore",
                    "Expected [hello], got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testMissingDataDirIsFine() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"), "OK");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            try {
                buildSite(tmpDir);
                String output = Files.readString(tmpDir.resolve("site/test.html"));
                if (output.contains("OK")) {
                    pass("testMissingDataDirIsFine");
                } else {
                    fail("testMissingDataDirIsFine", "Expected OK in output, got: " + output);
                }
            } catch (Exception e) {
                fail("testMissingDataDirIsFine", "Build threw: " + e.getMessage());
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testEmptyDataDirIsFine() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"), "OK");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            try {
                buildSite(tmpDir);
                pass("testEmptyDataDirIsFine");
            } catch (Exception e) {
                fail("testEmptyDataDirIsFine", "Build threw: " + e.getMessage());
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testInvalidJsonThrowsClearError() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("broken.json"), "{ this is not json");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"), "OK");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            try {
                buildSite(tmpDir);
                fail("testInvalidJsonThrowsClearError",
                    "Expected an exception for invalid JSON, but build succeeded");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("broken.json")) {
                    pass("testInvalidJsonThrowsClearError");
                } else {
                    fail("testInvalidJsonThrowsClearError",
                        "Exception did not mention filename. Got: " + msg);
                }
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testNonJsonFilesIgnored() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve(".DS_Store"), "garbage");
            Files.writeString(dataDir.resolve("notes.txt"), "hand notes");
            Files.writeString(dataDir.resolve("real.json"), "[{\"x\":\"y\"}]");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "{% for r in real %}[{{ r.x }}]{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            try {
                buildSite(tmpDir);
                String output = Files.readString(tmpDir.resolve("site/test.html"));
                if (output.contains("[y]")) {
                    pass("testNonJsonFilesIgnored");
                } else {
                    fail("testNonJsonFilesIgnored", "Expected [y], got: " + output);
                }
            } catch (Exception e) {
                fail("testNonJsonFilesIgnored", "Build threw: " + e.getMessage());
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    static void testMultipleDataFilesCoexist() throws Exception {
        Path tmpDir = createTestSite();
        try {
            Path dataDir = tmpDir.resolve("app/data");
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("alpha.json"), "[{\"v\":\"a1\"}]");
            Files.writeString(dataDir.resolve("beta.json"), "[{\"v\":\"b1\"}]");

            Path templatesDir = tmpDir.resolve("app/templates");
            Files.writeString(templatesDir.resolve("page.html"),
                "A:{% for x in alpha %}{{ x.v }}{% endfor %}|" +
                "B:{% for x in beta %}{{ x.v }}{% endfor %}");

            Path pagesDir = tmpDir.resolve("app/pages");
            Files.writeString(pagesDir.resolve("test.md"),
                "---\ntitle: Test\ntype: page\ntemplate: page.html\n---\n");

            buildSite(tmpDir);

            String output = Files.readString(tmpDir.resolve("site/test.html"));
            if (output.contains("A:a1") && output.contains("B:b1")) {
                pass("testMultipleDataFilesCoexist");
            } else {
                fail("testMultipleDataFilesCoexist",
                    "Expected both data files registered, got: " + output);
            }
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    // --- helpers ---

    static Path createTestSite() throws IOException {
        Path tmpDir = Files.createTempDirectory("arc-data-test");
        Path postsDir = tmpDir.resolve("app/posts");
        Path pagesDir = tmpDir.resolve("app/pages");
        Path templatesDir = tmpDir.resolve("app/templates");
        Path siteDir = tmpDir.resolve("site");
        Files.createDirectories(postsDir);
        Files.createDirectories(pagesDir);
        Files.createDirectories(templatesDir);
        Files.createDirectories(siteDir);
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

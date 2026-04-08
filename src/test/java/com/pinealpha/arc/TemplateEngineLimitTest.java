// ABOUTME: Standalone verification script for the {% for x in items limit N %} syntax
// ABOUTME: Run via: javac -d /tmp/arc-test -cp target/classes src/test/java/com/pinealpha/arc/TemplateEngineLimitTest.java && java -cp target/classes:/tmp/arc-test com.pinealpha.arc.TemplateEngineLimitTest
package com.pinealpha.arc;

import java.nio.file.Path;
import java.util.*;

public class TemplateEngineLimitTest {
    public static void main(String[] args) throws Exception {
        int failures = 0;

        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, String> m = new HashMap<>();
            m.put("title", "Item " + i);
            items.add(m);
        }

        TemplateEngine engine = new TemplateEngine();
        engine.registerGlobalVariable("items", items);

        // Test 1: no limit → all 10
        String tplAll = "{% for item in items %}[{{ item.title }}]{% endfor %}";
        String allOut = engine.processTemplate(tplAll, new HashMap<>(), "", Path.of("."));
        int allCount = allOut.split("\\[", -1).length - 1;
        if (allCount != 10) {
            System.err.println("FAIL: expected 10 items without limit, got " + allCount + " -> " + allOut);
            failures++;
        } else {
            System.out.println("PASS: unlimited loop renders all 10");
        }

        // Test 2: limit 3 → 3 items
        String tplLimit = "{% for item in items limit 3 %}[{{ item.title }}]{% endfor %}";
        String limitOut = engine.processTemplate(tplLimit, new HashMap<>(), "", Path.of("."));
        int limitCount = limitOut.split("\\[", -1).length - 1;
        if (limitCount != 3) {
            System.err.println("FAIL: expected 3 items with limit 3, got " + limitCount + " -> " + limitOut);
            failures++;
        } else {
            System.out.println("PASS: limit 3 renders 3");
        }

        // Test 3: limit larger than collection → all items
        String tplBig = "{% for item in items limit 99 %}[{{ item.title }}]{% endfor %}";
        String bigOut = engine.processTemplate(tplBig, new HashMap<>(), "", Path.of("."));
        int bigCount = bigOut.split("\\[", -1).length - 1;
        if (bigCount != 10) {
            System.err.println("FAIL: expected 10 items with limit 99, got " + bigCount);
            failures++;
        } else {
            System.out.println("PASS: limit 99 renders all 10");
        }

        if (failures > 0) {
            System.err.println(failures + " test(s) failed");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }
}

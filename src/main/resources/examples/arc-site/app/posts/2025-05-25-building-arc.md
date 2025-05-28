---
title: "Engineering Arc: Technical Insights from Building a Java Static Site Generator"
date: 2025-05-25
template: post.html
---

# Engineering Arc: Technical Insights from Building a Java Static Site Generator

## Introduction

Arc is a static site generator written in Java 25, designed to convert Markdown files into HTML using a flexible template system. This post explores the technical decisions, challenges, and architectural patterns that shaped Arc’s development.

## Project Goals and Constraints

- **Maintain a clean output structure**: The generator should strip the `app` directory from output paths and write all generated files to a `site` directory.
- **Support flexible templating**: Content should be injected into HTML templates using a simple and consistent syntax.
- **Enable metadata-driven generation**: Frontmatter in Markdown files should control template selection and provide variables for injection.

## Directory and File Structure

Arc expects the following structure:

```
app/
  ├── posts/         # Markdown files with frontmatter
  └── templates/     # HTML templates (e.g., post.html)
site/                # Output directory (auto-generated)
```

The generator recursively scans `app/` for `.md` files, processes them, and writes output to `site/`, mirroring the original structure minus the `app` prefix.

## Frontmatter Parsing

Arc uses YAML-style frontmatter to extract metadata. For example:

```markdown
---
title: "Custom Post Title"
template: post.html
---
Content goes here.
```

Frontmatter is parsed into a `Map<String, String>`, making all variables available for template injection.

## Template Engine Design

The template system is intentionally minimal. All variables—including content—are injected using the `{{ variable }}` syntax. For example, a template might look like:

```html
<!DOCTYPE html>
<html>
<head>
  <title>{{ title }}</title>
</head>
<body>
  {{ content }}
</body>
</html>
```

The Java implementation replaces all `{{ ... }}` placeholders with their corresponding values from the frontmatter map, plus the rendered content.

## Markdown Rendering

Arc leverages the CommonMark library for robust Markdown-to-HTML conversion. After extracting frontmatter, the Markdown body is parsed and rendered, supporting:
- Headings, lists, code blocks
- Links and images
- Inline HTML

## Key Implementation Details

- **File Scanning**: Uses `Files.walk` to recursively find all `.md` files under `app/`.
- **Frontmatter Extraction**: Uses regex to extract the section between leading `---` lines; parses each line as a key-value pair.
- **Template Loading**: Reads the specified template (default: `post.html`) from `app/templates/`.
- **Variable Injection**: Iterates over the frontmatter map, replacing `{{ key }}` in the template with the value. Content is injected as `{{ content }}`.
- **Output Path Calculation**: The output path mirrors the input, minus the `app` prefix, and replaces `.md` with `.html`.

## Challenges and Solutions

- **Consistent Template Syntax**: Early iterations used both `{% content %}` and `{{ title }}`. We standardized on `{{ ... }}` for clarity and maintainability.
- **Clean Output Paths**: Careful path manipulation ensures the output directory structure is clean and doesn’t leak implementation details (like the `app` folder).
- **Error Handling**: The generator gracefully handles missing templates or malformed frontmatter, providing clear error messages.

## Future Work

Arc is intentionally minimal, but the architecture supports easy extension. Possible enhancements include:
- Custom template variables
- Tag/category pages
- RSS feed generation
- Asset pipeline (images, CSS)
- Watch mode for live rebuilds

## Conclusion

Building Arc was an exercise in simplicity and clarity. By focusing on clean architecture and minimal dependencies, we created a generator that’s easy to understand, extend, and maintain. The project demonstrates that even with a small codebase, you can achieve flexible, robust static site generation in Java.


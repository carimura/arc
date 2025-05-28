# Arc - A Simple Static Site Generator

**WARNING: Not sure how you got here but it's very raw at the moment. About 3 hours of AI coding.**

Arc is a lightweight static site generator written in Java that converts Markdown files to HTML. It's designed to be simple, fast, and easy to use for generating static websites from Markdown content.

## Features

- Converts Markdown (`.md`) files to styled HTML
- Supports frontmatter for metadata
- Handles templates with variables and loops
- Maintains directory structure in the output
- Simple command-line interface
- Built-in web server for testing

## Requirements

- Java 25 or later
- Maven 3.6.0 or later

## Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/arc.git
   cd arc
   ```

2. Build the project with Maven:
   ```bash
   mvn clean package
   ```

   This will create two files in the `target` directory:
   - `arc-1.0-SNAPSHOT.jar` - The main JAR file
   - `arc-1.0-SNAPSHOT-jar-with-dependencies.jar` - The standalone JAR with all dependencies

3. This will also run jpackage to create a local app (currently supported platform: macos arm64)

4. create an executable script in your path somewhere and point it to the arc file like so:

```bash
#!/bin/bash
/path-to-this-project/target/jpackage/arc.app/Contents/MacOS/arc
```

5. Copy all the contents from /src/main/resources/examples/arc-site to a separate folder

6. simply run `arc` in that folder. Boom.



## Directory Structure

Arc expects the following directory structure:
```
project/
├── app/
│   ├── posts/         # Blog posts in Markdown
│   ├── pages/         # Static pages in Markdown
│   ├── templates/     # HTML templates
│   └── assets/        # Static assets (CSS, JS, images)
└── site/              # Generated site (created by Arc)
```

## Usage

### Basic Usage

1. Navigate to your project directory (e.g., `arc-test`)
2. Run the Arc generator:
   ```bash
   java -jar /path/to/arc/target/arc-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
   Or if you created a native package:
   ```bash
   ./arc
   ```

3. The generated site will be in the `site` directory.

### Testing with the Built-in Web Server

To test your site locally, you can use Java's built-in web server:

```bash
# From your project directory
jwebserver -p 8000 -d site
```

Then open http://localhost:8000 in your browser.

### Development Workflow

1. Edit your Markdown files in the `app` directory
2. Run the Arc generator to rebuild the site
3. Refresh your browser to see changes
4. Repeat!

## Template System

Arc uses a simple template system with the following features:

- `{{ variable }}` for variable substitution
- `{% if condition %}...{% endif %}` for conditionals
- `{% for item in collection %}...{% endfor %}` for loops
- `{% include "file.html" %}` for including partials

## Example Frontmatter

```yaml
---
title: My Awesome Post
date: 2025-05-26
template: post.html
description: A brief description of the post
---

# My Awesome Post

This is the content of my post...
```

## License

MIT

# Arc - A Simple, Fast, Static Site Generator in Java

Arc is a lightweight static site generator written in Java that converts Markdown files to HTML. It's designed to be simple, fast, and easy to use for generating static websites from Markdown content.

To read about building Arc, go [here](http://chadarimura.com/posts/2025-05-28-building-arc.html).

Warning: this was built in about 5 hours with an AI buddy because I wanted to rebuild my personal site and learn Cursor.

## Features

- Converts Markdown files to HTML using CommonMark
- Supports YAML frontmatter for metadata
- Simple template system
- Variables: `{{ variable }}`
- Loops: `{for post in posts}...{% endfor %}`
- Conditionals: `{% if variable %}...{% endif %}`
- Includes: `{% include "header.html" %}`
- Has a built-in hot reload mode for development
- Uses `jpackage` to build a native executable

## Requirements

- Java 24 or later
- Faith

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
This will also run jpackage to create a local app (currently supported platform: macos arm64)

3. create an executable script in your path somewhere and point it to the arc file like so:

```bash
#!/bin/bash
/path-to-this-project/target/jpackage/arc.app/Contents/MacOS/arc
```

4. Copy all the contents from /src/main/resources/examples/arc-site to a separate folder

5. Run `arc --watch` in that folder which runs in hot-reload mode.

6. cd to /site directory and type `jwebserver`

7. Open http://localhost:8000 in your browser.



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

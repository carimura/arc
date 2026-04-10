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
- RSS feed generation with configurable site metadata
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

Run `arc --help` to print usage and content routing details.



## Directory Structure

Arc expects the following directory structure:
```
project/
├── app/
│   ├── posts/         # Blog posts in Markdown
│   ├── pages/         # Static pages in Markdown
│   ├── templates/     # HTML templates
│   ├── assets/        # Static assets (CSS, JS, images)
│   ├── data/          # JSON data files (optional)
│   └── site.config    # Site configuration (optional)
└── site/              # Generated site (created by Arc)
```

Content with `type: page` in `app/pages` is generated at the site root. `type: post` generates under `site/posts/` to preserve the existing blog URL structure. Other content types are generated under a matching output directory, so `type: til` generates under `site/til/`.

## Collections

Every distinct frontmatter `type:` value automatically becomes a global template collection variable. The naming rule is: append `s` to the type, then convert any hyphens to underscores so the result is a valid template identifier.

| Frontmatter type | Collection variable |
|------------------|---------------------|
| `post`           | `posts`             |
| `til`            | `tils`              |
| `x-post`         | `x_posts`           |
| `event`          | `events`            |

Collections are sorted by `date:` in reverse chronological order. Use them in any template:

```html
{% for til in tils %}
  <li><a href="{{ til.url }}">{{ til.title }}</a></li>
{% endfor %}
```

## Data Files

JSON files placed in `app/data/` are loaded at build time and registered as global template variables. The filename (minus the `.json` extension, with hyphens converted to underscores) becomes the variable name.

| File                        | Variable    |
|-----------------------------|-------------|
| `app/data/links.json`       | `links`     |
| `app/data/x-posts.json`     | `x_posts`   |
| `app/data/site-meta.json`   | `site_meta` |

A JSON **array** is exposed as a list and used with `{% for %}` loops:

```json
[
  { "name": "GitHub",   "url": "https://github.com/me" },
  { "name": "LinkedIn", "url": "https://linkedin.com/in/me" }
]
```

```html
{% for link in links %}
  <a href="{{ link.url }}">{{ link.name }}</a>
{% endfor %}
```

A JSON **object** is exposed as a map and accessed with dotted notation:

```json
{ "theme": "dark", "tagline": "Built with Arc" }
```

```html
<body class="{{ settings.theme }}">
  <p>{{ settings.tagline }}</p>
</body>
```

Items inside a `{% for %}` loop must be flat — the template engine substitutes one level of `item.key` references and does not recurse into nested objects or arrays. Nested structures outside of loops can be reached with chained dots (`{{ var.outer.inner }}`). Non-`.json` files in `app/data/` are ignored. Invalid JSON aborts the build with an error that names the offending file.


## Site Configuration

Arc supports an optional `app/site.config` file for global site settings:

```yaml
---
title: My Arc Site
description: A beautiful website built with Arc
url: https://example.com
author: Your Name
language: en-us
rss_max_items: 10
---
```

These settings are used for RSS feed generation and are available as template variables.

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

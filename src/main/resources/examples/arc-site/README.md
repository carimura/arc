# Arc Example Site

This is an example site for the Arc static site generator. You can use this as a starting point for your own site.

## Getting Started

1. Copy the contents of this directory to a new location:
   ```bash
   mkdir my-site
   cp -r /path/to/arc-jar/examples/arc-site/* my-site/
   cd my-site
   ```

2. Run the Arc generator:
   ```bash
   java -jar /path/to/arc-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

3. View your site by opening `site/index.html` in a web browser or use Java's built-in web server:
   ```bash
   cd site
   jwebserver -p 8000
   ```
   Then visit http://localhost:8000 in your browser.

## Project Structure

```
.
├── app/                  # Source files for your site
│   ├── assets/           # Static assets (CSS, JS, images)
│   ├── pages/            # Static pages in Markdown format
│   ├── posts/            # Blog posts in Markdown format
│   └── templates/        # HTML templates
└── site/                 # Generated site (created when you run the generator)
```

## Customizing

- Edit the templates in `app/templates/` to change the site's layout and styling
- Add new posts as Markdown files in `app/posts/`
- Create new pages in `app/pages/`
- Add custom CSS/JS in `app/assets/`

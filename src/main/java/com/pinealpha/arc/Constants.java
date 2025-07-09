package com.pinealpha.arc;

import java.time.format.DateTimeFormatter;

public class Constants {

    // Template variable names
    public static final String CONTENT_VAR = "content";
    public static final String URL_VAR = "url";
    public static final String POSTS_VAR = "posts";
    public static final String TEMPLATE_VAR = "template";
    public static final String DATE_VAR = "date";
    public static final String ACTIVE_NAV_VAR = "active_nav";
    public static final String LATEST_POST_VAR = "latest_post";
    
    // Directory names
    public static final String APP_DIR = "app";
    public static final String SITE_DIR = "site";
    public static final String POSTS_DIR = "posts";
    public static final String PAGES_DIR = "pages";
    public static final String TEMPLATES_DIR = "templates";
    public static final String ASSETS_DIR = "assets";
    
    // RSS and Config constants
    public static final String RSS_FEED_FILE = "feed.xml";
    public static final String SITE_CONFIG_FILE = "site.config";
    
    // RSS Default values
    public static final String DEFAULT_SITE_TITLE = "My Arc Site";
    public static final String DEFAULT_SITE_DESCRIPTION = "A site generated with Arc";
    public static final String DEFAULT_SITE_URL = "http://localhost:8080";
    public static final String DEFAULT_SITE_AUTHOR = "Arc User";
    public static final String DEFAULT_SITE_LANGUAGE = "en-us";
    public static final int DEFAULT_RSS_MAX_ITEMS = 10;
    
    // Template syntax patterns
    public static final String VARIABLE_PATTERN = "\\{\\{\\s*%s\\s*\\}\\}";
    public static final String INCLUDE_PATTERN = "\\{%\\s*include\\s+\"([^\"]+)\"\\s*%\\}";
    public static final String IF_PATTERN = "\\{\\%\\s*if\\s+([^%}]+)\\s*\\%\\}(.*?)(\\{\\%\\s*endif\\s*\\%\\}|$)";
    public static final String FOR_PATTERN = "\\{\\%\\s*for\\s+(\\w+)\\s+in\\s+(\\w+)\\s*\\%\\}(.*?)\\{\\%\\s*endfor\\s*\\%\\}";
    

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter POST_DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Constants() {
        // Prevent instantiation
    }
} 
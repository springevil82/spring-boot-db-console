# 🗄️ Spring Boot DB Console Starter

> An embedded, zero-config database UI console for Spring Boot applications — like pgAdmin or DBeaver, available at a single HTTP endpoint.

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x%20%7C%203.x-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0%20(Free)-brightgreen)](LICENSE)
[![Open Source](https://img.shields.io/badge/Free-Open%20Source-green)](https://github.com/db-console/spring-boot-db-console)

---

## ✨ Features

| Feature                   | Details                                                                                             |
|---------------------------|-----------------------------------------------------------------------------------------------------|
| **Auto-detection**        | Finds **all** `DataSource` beans automatically — no manual configuration needed                     |
| **SQL Editor**            | Full CodeMirror editor with SQL syntax highlighting, execution, smart autocomplete (aliases), and SQL reformat |
| **Schema Browser**        | Collapsible tree: Catalogs → Schemas → Tables/Views → Columns (with PK/nullable markers) → Indexes  |
| **Results Grid**          | Paginated table with NULL / number / boolean colouring, row count, execution time                   |
| **CSV Export**            | One-click export with UTF-8 BOM (Excel-compatible)                                                  |
| **Context Menu**          | Right-click on tables/columns: Preview, Copy name, Show indexes                                     |
| **Multiple DataSources**  | Switch between datasources via dropdown                                                             |
| **Resizable panels**      | Drag to resize sidebar, editor, and results pane                                                    |
| **Java 8 compatible**     | Compiled bytecode targets Java 8; runs on Java 8 through 25+                                        |
| **Spring Boot 2.x & 3.x** | No `javax.servlet` imports — works on both generations                                              |
| **Free & Open Source**    | Apache 2.0 License — fully free to use, modify, and distribute                                      |

---

## 🚀 Quick Start

1. Add the dependency.
2. Explicitly enable the component: `db-console.enabled=true`.


### Maven

```xml
<dependency>
    <groupId>io.github.db-console</groupId>
    <artifactId>spring-boot-db-console-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.db-console:spring-boot-db-console-starter:1.0.0'
```

### Enable component

```properties
# application.properties
db-console.enabled=true
```

That's it. Start your application and open:

```
http://localhost:8080/db-console
```

---

## 🖥️ UI Overview

### Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+Enter` / `Cmd+Enter` | Execute query (or selected text) |
| `Ctrl+/` | Toggle line comment |
| `Ctrl+Space` / `Cmd+Space` | Show SQL autocomplete suggestions (tables/columns/aliases) |
| `Ctrl+L` / `Cmd+L` | Reformat SQL and uppercase tokens (except string literals) |

### Mouse Interactions

| Action | Result |
|---|---|
| Double-click on table | Preview first 100 rows |
| Right-click on table | Context menu (Preview, Copy name, Show indexes) |
| Right-click on column | Copy column / qualified name |
| Drag sidebar edge | Resize schema browser |
| Drag horizontal divider | Resize editor / results |

---

## ⚙️ Configuration

The component is **disabled by default** for security. All properties are optional with sensible defaults:

```properties
# application.properties

# Enable DB Console (default: false, must be explicitly set to true)
db-console.enabled=true

# Change the URL path (default: /db-console)
db-console.path=/db-console

# Default row limit for SELECT queries
db-console.max-rows=500

# Exclude specific DataSource beans (e.g. Spring Batch internal datasource)
db-console.exclude-datasources=batchDataSource,quartzDataSource
```

---

## 🔒 Security

The console endpoint is a plain Spring MVC controller, so **Spring Security rules apply automatically**.

---

## 🏗️ How It Works

```
Host Application
      │
      ├─ DataSource beans (auto-detected)
      │
      └─ DbConsoleAutoConfiguration  (triggered by spring.factories / AutoConfiguration.imports)
              │
              ├─ DataSourceRegistry   – collects all DataSource beans by name
              ├─ DatabaseService      – JDBC metadata + statement execution
              └─ DbConsoleController  – REST endpoints + serves index.html
                      │
                      └─ /db-console/**  (served as Spring MVC routes)
```

**Auto-configuration conditions:**
- Servlet web application (`@ConditionalOnWebApplication(SERVLET)`)
- `DataSource` class present on classpath (`@ConditionalOnClass`)
- `db-console.enabled = true` (`@ConditionalOnProperty`)
- Both `spring.factories` (Spring Boot 2.x) and `AutoConfiguration.imports` (Spring Boot 3.x) are registered

---

## 🤝 Contributing

Contributions are welcome! Please open an issue or pull request on GitHub.

```bash
git clone https://github.com/db-console/spring-boot-db-console
cd spring-boot-db-console
mvn test
```

---

## 📄 License

**Apache License 2.0** — Free and open-source software.  
You are free to use, modify, and distribute this software without restrictions.  
See [LICENSE](LICENSE) for full terms.

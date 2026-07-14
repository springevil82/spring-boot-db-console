# 🗄️ Spring Boot DB Console Starter

> An embedded, zero-config database UI console for Spring Boot applications — like pgAdmin or DBeaver, available at a single HTTP endpoint.

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x%20%7C%203.x-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange)](LICENSE)

---

## ✨ Features

| Feature | Details |
|---|---|
| **Auto-detection** | Finds **all** `DataSource` beans automatically — no manual configuration needed |
| **SQL Editor** | Full CodeMirror editor with SQL syntax highlighting, Ctrl+Enter execution, selection-only execution |
| **Schema Browser** | Collapsible tree: Catalogs → Schemas → Tables/Views → Columns (with PK/nullable markers) → Indexes |
| **Results Grid** | Paginated table with NULL / number / boolean colouring, row count, execution time |
| **CSV Export** | One-click export with UTF-8 BOM (Excel-compatible) |
| **Context Menu** | Right-click on tables/columns: Preview, Copy name, Show indexes |
| **Multiple DataSources** | Switch between datasources via dropdown |
| **Resizable panels** | Drag to resize sidebar, editor, and results pane |
| **Zero config** | Works with a single Maven/Gradle dependency |
| **Java 8 compatible** | Compiled bytecode targets Java 8; runs on Java 8 through 21+ |
| **Spring Boot 2.x & 3.x** | No `javax.servlet` imports — works on both generations |

---

## 🚀 Quick Start

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

That's it. Start your application and open:

```
http://localhost:8080/db-console
```

---

## 🖥️ UI Overview

```
┌─ DB Console ─────────────────────────── DataSource: dataSource ★ ─┐
│                                                                      │
│  ┌─ Schema Browser ─────┐  ┌─ SQL Editor ──────────────────────┐   │
│  │ 🗂 PUBLIC             │  │  ▶ Run  ✕ Clear  Limit 100  …    │   │
│  │  ├ 📋 users           │  │                                    │   │
│  │  │  ├ 🔑 id INT       │  │  SELECT u.*, o.total              │   │
│  │  │  ├ ● name VARCHAR  │  │  FROM users u                     │   │
│  │  │  └ ◌ email VARCHAR │  │  JOIN orders o ON o.user_id = u.id│   │
│  │  ├ 📋 orders          │  │  WHERE u.active = true            │   │
│  │  └ 👁 v_active_users  │  │  LIMIT 100;                       │   │
│  └──────────────────────┘  ├────────────────────────────────────┤   │
│                             │ ✓ 42 rows · 12ms     ↓ Export CSV │   │
│                             │ # │ id │ name  │ email │ total    │   │
│                             │ 1 │  1 │ Alice │ a@… │  299.00   │   │
│                             │ 2 │  2 │ Bob   │ b@… │  149.50   │   │
│                             └────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

### Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+Enter` / `Cmd+Enter` | Execute query (or selected text) |
| `Ctrl+/` | Toggle line comment |

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

All properties are optional with sensible defaults:

```properties
# application.properties

# Disable the console entirely (e.g. in production)
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

### Permit the console path in Spring Security

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        // Only allow admins to access the DB Console
        .requestMatchers("/db-console/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    );
    return http.build();
}
```

### Disable in production

```properties
# application-prod.properties
db-console.enabled=false
```

---

## 🗂️ REST API Reference

The console exposes a REST API that the UI uses internally. You can also call it programmatically.

| Method | Path | Description |
|---|---|---|
| `GET` | `/db-console` | Serve the UI |
| `GET` | `/db-console/api/datasources` | List all registered datasources |
| `GET` | `/db-console/api/datasources/{name}/schemas` | List schemas/catalogs |
| `GET` | `/db-console/api/datasources/{name}/tables?catalog=&schema=` | List tables |
| `GET` | `/db-console/api/datasources/{name}/columns?table=&schema=` | List columns |
| `GET` | `/db-console/api/datasources/{name}/indexes?table=&schema=` | List indexes |
| `POST` | `/db-console/api/datasources/{name}/execute` | Execute SQL |
| `GET` | `/db-console/api/datasources/{name}/preview?table=&limit=100` | Preview table data |
| `POST` | `/db-console/api/datasources/{name}/export` | Export results as CSV |

### Execute SQL – request body

```json
{
  "sql": "SELECT * FROM users WHERE active = true",
  "maxRows": 100,
  "schema": "public",
  "catalog": null
}
```

### Execute SQL – response

```json
{
  "type": "SELECT",
  "columns": ["id", "name", "email"],
  "rows": [[1, "Alice", "alice@example.com"], [2, "Bob", "bob@example.com"]],
  "executionTimeMs": 12,
  "updateCount": 0,
  "error": null
}
```

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
- `db-console.enabled != false` (`@ConditionalOnProperty`)
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

Apache License 2.0 — see [LICENSE](LICENSE).

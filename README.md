# TCubed (T³) — Task Tracking Tool

---

TCubedApp is a Java 17 / Spring Boot 3.2.0 Kanban-style task tracking web application built with Thymeleaf and MySQL.

## Prerequisites

* **Java 17+** (JDK required to run the Maven wrapper)
* **MySQL 8.x** (local instance, or connection credentials for a remote MySQL database)
* **Maven:** Not required to install globally—use the bundled Maven wrapper (`./mvnw` on macOS/Linux, `.\mvnw.cmd` on Windows).

> **Branching Note for Incoming Team:** Start from the `development` branch

---

## Quick Start (Local Development)

### 1. Set Up the Local MySQL Database
1. Create a local MySQL database named `TCubedDB` and a local user `TCubedTs` with password `TCubed2026` (or adjust the JVM flags below to match your local credentials).
2. Run the schema script first, then the seed script:
   1. `database/SetupDB.sql`
   2. `database/SeedDB.sql`
3. **Default seeded login account(s) for local testing:** `test@regis.edu` / `TestP@ss`.

### 2. Run the Application from the Command Line

**macOS / Linux:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_URL=jdbc:mysql://localhost:3306/TCubedDB -DDB_USER=TCubedTs -DDB_PASSWORD=TCubed2026"
```

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-DDB_URL=jdbc:mysql://localhost:3306/TCubedDB -DDB_USER=TCubedTs -DDB_PASSWORD=TCubed2026"
```

Open a browser and navigate to:
```text
http://localhost:8080
```

### 3. Running in NetBeans (Optional)
In **Project Properties → Run**, set:
* **Main Class:** `edu.regis.tcubed.TCubedApp`
* **VM Options:**
  ```text
  -DDB_URL=jdbc:mysql://localhost:3306/TCubedDB -DDB_USER=TCubedTs -DDB_PASSWORD=TCubed2026
  ```

---

## Running Automated Tests

```bash
# macOS / Linux
./mvnw test

# Windows (PowerShell)
.\mvnw.cmd test
```

Current test suites live under `src/test/java/edu/regis/tcubed/`:
* `controller/ProjectApiControllerTest.java`
* `controller/TaskApiControllerTest.java`
* `repository/ProjectRepositoryTest.java`
* `repository/TaskRepositoryTest.java`

---

## Windows Troubleshooting

* **`JAVA_HOME` not defined when running `.\mvnw.cmd`:** NetBeans uses its own bundled JDK without setting `JAVA_HOME` in Windows. Point `JAVA_HOME` to your JDK root folder (for example, `C:\Program Files\Apache NetBeans\jdk`) without `\bin`:
  ```powershell
  [System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Apache NetBeans\jdk", "User")
  ```

---

## Project Structure

```text
TCubedApp/
├── database/                         # SetupDB.sql (schema) and SeedDB.sql (seed data)
├── docs/                             # SRS PDF and database design diagrams
│   └── db_design/                    # ERDiagram.png and RelationSchema.png
├── src/main/java/edu/regis/tcubed/
│   ├── TCubedApp.java                # Spring Boot entry point
│   ├── config/                       # AuthInterceptor, WebConfig
│   ├── controller/                   # MVC views & REST API controllers
│   ├── model/                        # Account, Project, Task, TaskStatus
│   └── repository/                   # JDBC repositories & DatabaseConnectionManager
├── src/main/resources/
│   ├── static/css/style.css          # Global stylesheet
│   ├── static/js/                    # API clients & drag-and-drop logic
│   └── templates/                    # Thymeleaf views (login, register, my-tasks, projects, dashboard)
└── src/test/java/edu/regis/tcubed/   # Controller & repository unit tests
```

---

## Web Deployment (Koyeb & Aiven MySQL)

The web deployment (`W-Web`) runs on **Koyeb** backed by **Aiven MySQL**.
* **Configuration:** Inject production `DB_URL`, `DB_USER`, and `DB_PASSWORD` via Koyeb environment/JVM configuration—do **not** reuse local development credentials.
* **Access & Handoff:** **[Fill in: Name/contact of current Koyeb & Aiven account owner, live URL, and any SSL query parameters required in `DB_URL`.]**

---

## Documentation

| Document | Purpose |
| :--- | :--- |
| [`CHANGELOG.md`](CHANGELOG.md) | Sprint-by-sprint history (Sprints 1–4), terminology, and functional requirements |
| [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) | Security debt, architectural trade-offs, test gaps, and open functional bugs |
| [`docs/TCubed Software Requirements Specification.pdf`](docs/TCubed%20Software%20Requirements%20Specification.pdf) | Software requirements specification (SRS) |
| [`docs/db_design/ERDiagram.png`](docs/db_design/ERDiagram.png) & [`RelationSchema.png`](docs/db_design/RelationSchema.png) | Database entity-relationship diagram and relational schema |
# Known Issues & Handoff Notes

This document tracks known security debt, architectural trade-offs, build/environment gotchas, and open functional bugs.

---

## 1. Security Debt 

* **Unsalted SHA-256 Password Hashing (`src/main/java/edu/regis/tcubed/repository/AccountRepository.java`):**
  New account passwords are hashed using SHA-256 rather than a salted, adaptive password-hashing algorithm.
  * *Recommendation:* Migrate to BCrypt (e.g., `spring-security-crypto`) with per-user salts.
* **Plaintext Security Question Answers (`AccountRepository.java`):**
  `createAccount` hashes `Password`, but stores the security recovery `Answer` in plaintext in the `ACCOUNT` table.
  * *Recommendation:* Normalize and hash recovery answers before persisting them.
* **Documented Local Database Credentials (`README.md`):**
  `README.md` lists local MySQL credentials (`TCubedTs` / `TCubed2026`). These are strictly for local development—confirm that production Aiven MySQL uses separate credentials injected via Koyeb environment/JVM configuration.

---

## 2. Architectural Notes & Technical Debt

* **Logical Bucket Positions vs. Database `BucketId` (`model/TaskStatus.java`, `repository/TaskRepository.java`):**
  * `TaskStatus` (`1: To-Do`, `2: In Progress`, `3: Done`, `4: Trash`) labels its integer field `bucketId` ("database bucket ID"), and `Task` stores that same `1–4` value.
  * In `TaskRepository`, `1–4` is actually mapped to `BUCKET.Position` (logical column order) per board, while reads alias `bu.Position AS LogicalBucketId` and writes translate it to the real auto-incremented `BUCKET.BucketId`.
  * **Risky Fallback:** In both `updateTaskPosition` and `updateTaskForMember`, if the board/bucket lookup fails, the code falls back to writing the raw `1–4` value directly into `TASK.BucketId`—which can cause cross-project data bleed or a foreign key error instead of failing cleanly.
  * **Extensibility Note:** `ensureBoardBucketsExist` hardcodes the 4-column layout (`To-Do`, `In Progress`, `Done`, `Trash`), and `TaskStatus.fromBucketId` defaults unrecognized values to `TODO`.
* **Side-Effect on Read (`TaskRepository.getTasksByProjectName`):**
  * Calling `getTasksByProjectName` invokes `ensureBoardBucketsExist`, which inserts a new `PROJECT`, `BOARD`, and 4 `BUCKET` rows if the project name does not exist. A mistyped project name can silently create phantom workspaces in the database.
* **Inconsistent Repository-Level Authorization (`repository/TaskRepository.java`):**
  * `*ForMember` methods (`getTasksForMember`, `findTaskForMember`, `updateTaskForMember`, `deleteTaskForMember`) join `PROJECT_MEMBER` to verify user access, while `getTasksByProjectName`, `createTask`, and `updateTaskPosition` do not enforce membership at the repository layer.
* **Silent Unassignment & Ambiguous First-Name Matching (`TaskRepository.resolveAssignee`):**
  * Unmatched assignee inputs silently bind `NULL` (unassigned) instead of returning an error, and falling back to `LOWER(FirstName)` can match the wrong user if two accounts share a first name.
* **No Service Layer (`controller/` -> `repository/`):**
  Controllers currently call repositories directly with no intermediate service layer. As a result, business logic and SQL handling are concentrated in `TaskRepository.java` and `ProjectRepository.java`, which have grown significantly.
  * *Recommendation:* Consider introducing a `service/` layer before adding major new features.
* **Manual JDBC Connection Management (`repository/DatabaseConnectionManager.java`):**
  Database connectivity is managed manually via `-DDB_URL`, `-DDB_USER`, and `-DDB_PASSWORD` JVM properties rather than Spring Boot's auto-configured `DataSource` / HikariCP connection pooling and `application.properties`.
* **Documentation & Spec Drift (`docs/`):**
  Verify whether `docs/db_design/ERDiagram.png`, `docs/db_design/RelationSchema.png`, and `docs/TCubed Software Requirements Specification.pdf` fully match the latest `database/SetupDB.sql` schema and implemented use cases.

---

## 3. Build, Environment, & Test Gaps

* **Windows `JAVA_HOME` with NetBeans Bundled JDK:**
  Running `.\mvnw.cmd` from PowerShell fails if `JAVA_HOME` is unset, because NetBeans uses its own bundled JDK (e.g., `C:\Program Files\Apache NetBeans\jdk`) without registering `JAVA_HOME` in Windows.
* **Unit Test Coverage Gaps (`src/test/`):**
  Automated tests currently exist for `ProjectApiControllerTest`, `TaskApiControllerTest`, `ProjectRepositoryTest`, and `TaskRepositoryTest`. There is no automated test coverage yet for:
  * Authentication & session security (`AuthController`, `AuthInterceptor`, `AccountRepository`)
  * Board views (`BoardController`, `ProjectController`)
  * Domain models (`Account`, `Project`, `Task`, `TaskStatus`)
* **Java 23+ / Byte Buddy Compatibility (`pom.xml`):** Spring Boot 3.2.0 bundles Byte Buddy `1.14.10`, which officially supports up to Java 22. Running Mockito tests on newer JDKs (such as NetBeans' bundled Java 26) requires `-Dnet.bytebuddy.experimental=true` until Spring Boot / Byte Buddy is upgraded, or using JDK 17–21.

---

## 4. Open Functional Bugs

> *Add any known UI, API, or database bugs below before handoff.*

### [Bug Title — e.g., Drag-and-drop position not persisting after refresh]
* **Severity:** Blocker / Major / Minor
* **Area / Files:** `src/main/java/edu/regis/tcubed/...`
* **Steps to Reproduce:**
  1. Log in as `<user>` and navigate to `<route>`.
  2. Perform `<action>`.
* **Expected Behavior:** ...
* **Actual Behavior:** ...
* **Notes / Suspected Cause:** ...
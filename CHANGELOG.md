# TCubedApp - Documentation & Changelog

## 📖 Team Terminology & UI Decisions
To maintain consistency across frontend and backend development, the team has agreed on the following vocabulary and design patterns:
* **Container:** The terminology used for an individual work item (replacing Trello's "card"). 
* **Board:** The primary project workspace containing status columns.
* **Trash Bucket / Archive:** A designated UI drop-zone used for quick deletion or archiving of Containers.
* **Task ID Format:** Sequential, auto-incrementing identifiers formatted as `TP-XXX` (e.g., `TP-101`, `TP-131`).
* **Task States (Columns):** 
  * `To-Do`
  * `In Progress`
  * `Done`

## ⚙️ Functional Requirements
* **Authentication:** Users must securely log in. Credentials are validated against the `Account` table in the Aiven MySQL database.
* **Routing:** Upon successful login, users are routed to a Project Selection view before accessing the main dashboard.
* **Interactive UI:** Users must be able to move Containers between status columns using drag-and-drop functionality.
* **Cross-Platform Compatibility:** The project must build successfully on both macOS and Windows without file-locking errors (utilizing Maven Profiles for OneDrive environments).

---

## 📝 Changelog

### [Sprint 1] - Initial Setup & Prototyping
**Frontend & Architecture (Oscar):**
* Scaffolded the core Board UI using Thymeleaf and CSS Flexbox.
* Implemented a native HTML5 drag-and-drop proof of concept for moving Containers between status columns.
* Created the `Task.java` POJO and injected mock data via `BoardController` to decouple the UI design from the database schema development.
* Updated `AuthController` routing to direct users to a `projects.html` view upon successful login.

**Backend & Infrastructure (Thomas & Dylan):**
* Implemented Maven profile in `pom.xml` to support cross-platform builds without breaking the Windows OneDrive directory fix.
* Configured application properties for Java 17 and Spring Boot 3.2.0.
* (WIP) Designing the MySQL relational schema and ER diagram for the `Sprint` and `Task` (Container) tables.

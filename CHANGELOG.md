# TCubedApp - Documentation & Changelog

## 📖 Team Terminology & UI Decisions
To maintain consistency across frontend and backend development, the team has agreed on the following vocabulary and design patterns:
* **Container:** The terminology used for an individual work item (replacing Trello's "card").
* **Board:** The primary project workspace containing status columns.
* **Trash Bucket / Archive:** A designated UI drop-zone used for quick deletion or archiving of Containers.
* **Task ID Format:** Sequential, auto-incrementing identifiers formatted as `TP-XXX` (e.g., `TP-101`, `TP-102`).
* **Task States (Columns):** 
  * `To-Do`
  * `In Progress`
  * `Done`

## ⚙️ Functional Requirements
* **Authentication & Session:** Users authenticate against the `Account` table. The active user identifier is stored in `HttpSession` to persist across views and handle session termination via `/logout`.
* **Routing:** Upon successful login, users are redirected to a Project Selection view (`/projects`) before accessing the board dashboard (`/dashboard`).
* **Interactive UI:** Users can move Containers between status columns and into the Trash Bucket using native HTML5 drag-and-drop.
* **Modular View Architecture:** Global UI components (navigation bar, user profile dropdown) are centralized using Thymeleaf fragments, with presentation styling isolated in a shared stylesheet.
* **Cross-Platform Compatibility:** The project builds cleanly across macOS and Windows development environments using Maven profile overrides for local build targets.

---

## 📝 Changelog

### [Sprint 1] - Initial Setup, UI Architecture & Prototyping

#### Added
* `src/main/resources/static/style.css`: Centralized global stylesheet managing the layout for the navigation bar, board columns, container cards, dropdown menus, and drop targets.
* `src/main/resources/templates/fragments.html`: Reusable Thymeleaf fragment (`top-nav`) featuring breadcrumb links, session-driven user profile display, and logout controls.
* `src/main/resources/templates/projects.html`: Project selection dashboard rendered post-authentication, integrated with the shared navigation fragment.
* `src/main/java/edu/regis/tcubed/BoardController.java`: Dedicated Spring MVC controller managing `/dashboard` routing and model attributes.
* `src/main/java/edu/regis/tcubed/Task.java`: Domain model POJO representing Containers (`taskId`, `title`, `description`, `status`, `assignee`).
* `CHANGELOG.md`: Established team repository documentation, terminology definitions, and progress tracking.

#### Changed
* `src/main/java/edu/regis/tcubed/TCubedApp.java`:
  * Updated `AuthController.handleLogin` to store authenticated user identity into `HttpSession`.
  * Changed post-login response from direct template rendering to a `redirect:/projects` flow.
  * Added `@GetMapping("/logout")` to invalidate active HTTP sessions and route back to login.
  * Decoupled dashboard view logic into `BoardController`.
* `src/main/resources/templates/dashboard.html`:
  * Integrated external stylesheet and injected `fragments.html` top navigation.
  * Refactored container columns to dynamically iterate over Java mock data using `th:each` and `th:if`.
  * Implemented HTML5 Drag-and-Drop API listeners (`dragstart`, `dragover`, `drop`) on container columns.
  * Added functional Trash Bucket drop zone to remove dragged DOM elements on drop.

#### Infrastructure & Backend (Team Synced)
* `pom.xml`: Integrated Java 17 and Spring Boot 3.2.0 properties, standard main class configuration, and the `windows-onedrive-fix` Maven build profile.
* `database/SetupDB.sql`: (In progress - Dylan) Relational schema design and ER diagram modeling for Task and Sprint persistence.
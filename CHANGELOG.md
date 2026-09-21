# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Releases below are marked by date. The project is not yet tagged in Git, so the version numbers
describe milestones rather than tagged points in history.

## [Unreleased]

### Added

- **Admin module** — the admin role is now usable end to end. A tabbed admin dashboard provides:
  - **User management** — list accounts with their farm counts and change roles, guarded so an
    admin cannot change their own role, delete themselves, strand the platform without an admin,
    or delete a user that still owns farms or workflow records.
  - **Knowledge base CRUD** — create, edit and delete crop/disease reference entries, with
    duplicate-name protection so entries stay addressable by name.
  - **Analytics** — users by role, requests and reports by status, advisory requests per ISO week
    over the last eight weeks, and the most-mentioned issues across disease reports.
- `AdminSeeder` bootstraps the first `ADMIN` account from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Signup only
  offers FARMER and OFFICER, so the admin screens were previously unreachable. The seeder is inert
  unless both variables are set and never touches an existing admin.
- 28 unit tests across `AdminServiceTest`, `KnowledgeBaseServiceTest` and `AdminSeederTest`.
- README v2: live demo and Swagger links, architecture diagram, full environment variable tables
  for both backend and frontend, and step-by-step Render/Vercel deployment instructions.
- `LICENSE` (MIT).

### Changed

- The frontend admin stub is now a real implementation, and service-layer coverage rose from 92.6%
  to 95.1%.
- The `admin` and `knowledge-base` endpoints are entirely `ADMIN`-guarded, matching the permissions
  table in the Problem Statement.
- README local-setup instructions corrected: the previous `cp .env.example .env` step had no
  effect because Spring Boot does not read `.env` files. Documents the working path instead
  (built-in defaults plus `docker compose`, or exporting the variables before running), and the
  Node.js requirement corrected to 20.19+/22.12+ for Vite 8.

## [0.3.0] - 2026-09-21

### Added

- **Season history module** — `SeasonLog` is now reachable end to end: a validated
  `SeasonLogRequest`, `SeasonLogService`, and a `SeasonLogController` exposing CRUD under
  `/api/farms/{farmId}/season-logs` behind `@PreAuthorize`, plus a farmer-facing list and a form
  handling both create and edit. Covered by 12 unit tests.
- **Test coverage reporting** — JaCoCo with a report and a check bound to the test phase,
  enforcing a minimum of 40% line coverage on the `service` package so `./mvnw test` fails when
  coverage drops. Current figure: 92.6%.
- **Frontend CI workflow** — `frontend.yml` runs `npm ci`, lint and build on Node 22, uploads the
  build artifact, and fails the run on either lint or build errors. Previously a frontend-only
  push triggered no pipeline at all.
- Coverage summary output and a JaCoCo report artifact in the backend workflow.
- Root `.env.example` template documenting the database, JWT and CORS variables.

### Changed

- Farm read authorization centralized in `FarmService.getFarmReadableBy`, replacing repeated
  ownership checks. Farmers are limited to their own farms; officers and admins may read any farm.
- `SeasonLog` now exposes `farmId`, since the `farm` association is `@JsonIgnore`d and clients had
  no way to reference it.
- Frontend auth context split into `authContext.js` (context and `useAuth` hook) and
  `AuthProvider.jsx` (provider only), so both files satisfy `react-refresh/only-export-components`.
  Session restoration moved from an effect to a lazy `useState` initializer, which also removed the
  now-unnecessary `loading` flag. Corrupt `localStorage` is caught instead of throwing during boot.
- Three overlapping deployment guides consolidated into `RENDER_DEPLOYMENT.md`, corrected to match
  the actual Docker-based `render.yaml`.

### Fixed

- **Farm data disclosure:** the soil readings endpoint resolved a farm by id without checking the
  caller, so any authenticated farmer could read any farm's readings by changing the id in the URL.
  Access now goes through the centralized ownership check.
- Season logs could be read by pairing an owned farm id with a log id belonging to another farm.
  A log's own farm is now authoritative and a mismatch returns 404.
- The officer advisory queue rendered an always-`undefined` farm id.
- Dropped the blanket `RuntimeException` → HTTP 400 handler: not-found, forbidden and conflict
  conditions now return 404/403/409, and the catch-all no longer echoes exception messages to
  clients.

### Removed

- Unused code: the never-read `AdvisoryRequest.aiSuggestion` field, an unreferenced
  `getLatestReading` service method and its repository finder, and unused repository methods.

## [0.2.0] - 2026-09-01

### Added

- JUnit 5 and Mockito unit tests for `UserService`, `FarmService`, `SoilReadingService`,
  `AdvisoryRequestService` and `DiseaseReportService`.
- Backend CI workflow: runs the suite against a temporary `postgres:15-alpine` service, builds the
  JAR, and fails the run on test failure.
- Deployment: Render Blueprint provisioning the web service and PostgreSQL together, a multi-stage
  `backend/Dockerfile`, and `frontend/vercel.json` for React Router SPA rewrites.
- `DataSourceConfig`, normalizing `postgres://` and `postgresql://` connection strings into JDBC
  form and extracting embedded credentials, so a single `DATABASE_URL` is enough in production.
- Configurable CORS origins via `FRONTEND_URL`, and a Spring Boot Actuator `/actuator/health`
  endpoint wired to the platform health check.

### Changed

- Login and signup error handling now distinguishes network failures, CORS rejections and server
  errors instead of showing a generic message.

### Fixed

- Render Blueprint syntax for environment and database configuration.
- `mvnw` execute bit and the Dockerfile build.

## [0.1.0] - 2026-08-12

Initial working MVP: the full advisory and disease-report workflows, end to end.

### Added

- Spring Boot 3 backend with JWT authentication (`jjwt`), `BCryptPasswordEncoder` password
  hashing, and role-based access for `FARMER`, `OFFICER` and `ADMIN`.
- Domain entities and relationships: `User`, `Farm`, `SoilReading`, `SeasonLog`,
  `AdvisoryRequest`, `DiseaseReport` and `KnowledgeBaseEntry`.
- Advisory request workflow with its status state machine:
  `PENDING → ASSIGNED → RESPONDED → CLOSED`.
- Disease report workflow with its status state machine:
  `REPORTED → UNDER_REVIEW → RESOLVED`.
- React frontend with login/signup, role-based routing, farm management, soil reading logging, and
  farmer/officer screens for both workflows.
- Architecture, ER and class diagrams committed under `docs/diagram/`.
- Docker Compose file for a local PostgreSQL 15 instance.

### Changed

- The backend was migrated from FastAPI (Python) to Spring Boot (Java) to match the capstone's Java
  track. The earlier Python scaffolding, Alembic migrations and exploratory data analysis notebook
  are retained under `ml/`.

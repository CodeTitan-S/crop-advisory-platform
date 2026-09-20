# 60-Day Capstone Implementation Guide
## Crop Advisory & Farming Guidance Platform — Java (Spring Boot) Track

This maps the `Problem_Statement.md` directly onto the capstone's Day 1–60 structure, using the exact tech stack, folder structure, and checklists from the capstone guide.

---

## Tech Stack (per capstone Section 4, Java Track)

| Layer | Choice |
|---|---|
| Frontend | React.js + Tailwind CSS + Axios |
| Backend | Spring Boot 3.x (Java 17) |
| Auth | Spring Security + JWT (jjwt library) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Build Tool | Maven |
| Testing | JUnit 5 (unit tests, mandatory) |
| API Docs | springdoc-openapi (Swagger UI) |
| CI/CD | GitHub Actions |
| Backend Hosting | Render or Railway |
| Frontend Hosting | Vercel or Netlify |
| Managed DB | Railway / Clever Cloud / Aiven |

---

## Folder Structure (per capstone Section 6.5)

```
src/main/java/com/college/cropadvisory/
 ├─ config/         (SecurityConfig, SwaggerConfig, CorsConfig)
 ├─ controller/     (REST endpoints — thin, no business logic)
 ├─ service/        (business logic — AdvisoryRequestService, DiseaseReportService, etc.)
 ├─ repository/     (Spring Data JPA interfaces)
 ├─ model/entity/   (JPA entities: User, Farm, SoilReading, SeasonLog,
 │                    AdvisoryRequest, DiseaseReport, KnowledgeBaseEntry)
 ├─ dto/            (request/response objects)
 └─ exception/      (custom exceptions + global error handler)
src/test/java/...   (JUnit tests, mirrors main structure)
docs/diagrams/      (architecture, ER, class diagrams)
.env.example, README.md, pom.xml
```

---

## Day 1 — Problem Statement

- Commit `Problem_Statement.md` (already drafted — see companion file) to the repo root.
- Get mentor approval on scope, especially the Day 42–60 AI enhancement choice.
- Install VS Code + all mandatory + Java-track extensions (capstone Section 5.1–5.2): GitLens, Prettier, ESLint, Thunder Client, GitHub Actions, GitHub PRs, EditorConfig, Error Lens, Todo Tree, Extension Pack for Java, Spring Boot Extension Pack, Lombok Annotations Support.

---

## Phase 1 — Day 2–10: Design & Planning

### Day 2–3: Repo & environment setup
- Create repo `crop-advisory-platform` (public, kebab-case), branch protection ON for `main` from today (PR + passing CI required to merge).
- Root files: `README.md` stub, `.gitignore` (target/, node_modules/, .env), `LICENSE` (MIT), `.env.example`, `CHANGELOG.md`.
- Scaffold Spring Boot project via Spring Initializr: dependencies — Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Validation, Lombok, springdoc-openapi.
- Scaffold Vite + React frontend.
- Local PostgreSQL running (or Dockerized) for now.

### Day 4–5: Design deliverables v1
- **Architecture diagram** — React client → Spring Boot API → PostgreSQL, with the Phase 3 AI service marked on the diagram now (even if it's just a labeled future box).
- **ER diagram** — all 7 entities from `Problem_Statement.md`, PK/FK, relationship types (all One-to-Many).
- **Class/module diagram** — entity classes, service classes, and which service depends on which repository.
- Commit all three to `/docs/diagrams/`.

### Day 6–8: DB schema + auth
- JPA entities for all 7 tables with proper `@OneToMany`/`@ManyToOne` mappings.
- `SecurityConfig`: Spring Security + JWT (jjwt) — signup/login endpoints, password hashing via `BCryptPasswordEncoder`, `role` claim in the JWT (`FARMER` / `OFFICER` / `ADMIN`).
- Global exception handler (`@ControllerAdvice`) returning your consistent `{ success, data, message }` envelope.

### Day 9–10: MVP build sprint
- Build the two Review-I flows (below).
- Enable springdoc-openapi — confirm Swagger UI loads at `/swagger-ui.html` locally.

**Commit check:** ≥3 commits/week, Weeks 1–2, spread across ≥2 days/week, Conventional Commits.

---

## REVIEW-I — Day 11: MVP Demonstration

**Two end-to-end flows:**
1. **Farm + Soil Logging** — farmer signs up, creates a `Farm`, logs a `SoilReading` — persisted and visible on reload.
2. **Advisory Request submission** — farmer submits an `AdvisoryRequest` (`status = PENDING`); an officer account logs in and sees it in their queue.

**Pre-review checklist (capstone Section 8.1):**
- `Problem_Statement.md` finalized, committed, guide-approved
- Architecture / ER / Class diagrams v1 in `/docs/diagrams/`
- Standard Spring Boot folder structure followed
- README v1 (overview, tech stack, local run instructions)
- ≥6 commits across Weeks 1–2
- Login/Signup issuing a real JWT
- 2 core flows working end-to-end
- App runs locally from README instructions alone (test with a fresh clone)

---

## Phase 2 — Day 12–40: Full Development

### Week 3 (Day 15–21): Core backend + frontend integration
- `AdvisoryRequestController`/`Service`/`Repository` — full CRUD + status transitions.
- Frontend: request submission form, farmer's request list/detail view, officer's assigned-queue view.

### Week 4 (Day 22–28): Remaining modules + auth hardening
- `DiseaseReportController`/`Service` — submission with optional image upload (store as a URL/path; a simple file upload endpoint is enough, no AI classification yet).
- `SeasonLogController`/`Service` — CRUD.
- Role-based route protection: `@PreAuthorize` annotations on controller methods per role.
- Start JUnit 5 tests for the service layer — build the habit now, don't cram into Week 5.

### Week 5 (Day 29–35): Finish tests + CI pipeline
- Reach ≥40% coverage on service-layer methods.
- One test class per major module: `AdvisoryRequestServiceTest`, `DiseaseReportServiceTest`, `FarmServiceTest`, `UserServiceTest`.
- All tests passing — zero red before merge.
- `.github/workflows/backend.yml` (Maven build + test) and `frontend.yml` (npm build + deploy), using the capstone guide's sample workflows as your starting templates.

### Week 6 (Day 36–42): Deployment + security
- Backend → Render/Railway, Frontend → Vercel/Netlify, DB → managed Postgres.
- Deploy hook URLs/tokens as GitHub Secrets — never in code or YAML directly.
- Security basics: BCrypt confirmed, all queries via Spring Data JPA (no raw SQL), `@Valid` on all request DTOs, CORS configured to your actual frontend origin (not `*`).
- `/actuator/health` (Spring Boot Actuator) as your health endpoint.
- Basic logging (SLF4J) for signup/login/errors.
- README v2: live demo link, architecture diagram, Swagger docs link, deployment steps.
- Update all 4 design docs to as-built state; update `CHANGELOG.md`.

**Commit check:** ≥3/week Weeks 3–6 (≥12 this phase, ≥18 cumulative).

---

## REVIEW-II — Day 41: Full Product, Live

Confirm against capstone Section 9 before this date:
- 100% of `Problem_Statement.md` features implemented
- Login supports 2+ roles (Farmer/Officer, Admin if included) with role-based screens
- All modules match the ER diagram
- Tests: ≥40% service coverage, all passing, one test class per module
- CI/CD: pipeline runs on every push/PR, fails red on test failure, deploys on merge to `main`
- Live public URLs for backend + frontend, cloud-hosted DB
- Security checklist complete (bcrypt, JPA-only queries, validation, CORS)
- `/actuator/health` responds
- README v2 complete, diagrams updated, CHANGELOG updated

At this point you have a fully functional advisory platform — live, demoable, with real workflow logic — independent of any AI. This is intentional: Phase 3 builds *on top of* something that already works end-to-end.

---

## Phase 3 — Day 42–59: Enhancement Feature — AI Crop Recommendation Engine

### Day 43–45: Proposal + proof-of-concept
- `Enhancement_Proposal.md`: problem (officers currently suggest crops from intuition alone, with no data backing), solution (train a model on soil/weather data to auto-suggest a ranked crop list, attached to the relevant `AdvisoryRequest` as an editable draft), tech choice (see below).
- **Architecture note for the Java track:** Spring Boot doesn't have a native ML training ecosystem like Python does, so the cleanest approach is:
  - Train the model **offline in Python** (scikit-learn, Random Forest, on the Kaggle Crop Recommendation dataset) and export it, **or**
  - Train and serve it as a **small, separate Python microservice** (FastAPI, single `/predict` endpoint) that your Spring Boot backend calls internally via REST — this is realistic, common in real companies (polyglot services), and gives you a clean answer for "why is part of this in Python?" in your viva.
  - Document this choice explicitly in the architecture diagram update — a labeled "ML microservice" box calling out to Spring Boot.
- Quick notebook prototype: confirm reasonable accuracy before committing further.

### Day 46–52: Full build
- Build and deploy the small Python ML microservice (same hosting approach — Render/Railway free tier, separate service).
- In Spring Boot: new endpoint `POST /api/v1/advisory-requests/{id}/suggest-crop` — pulls the request's linked farm's latest `SoilReading`, calls the ML microservice, stores the result in `AdvisoryRequest.ai_suggestion`.
- Frontend: officer's request-detail view shows the AI suggestion with an "accept/edit" action before the officer sends the final response.
- Model card: dataset used, accuracy, confusion matrix, honest limitations — commit to `/docs/`.

### Day 53–56: Tests + integration polish
- JUnit tests for the new service method (e.g. correct handling when no soil reading exists yet, correct request/response mapping to the ML microservice).
- Merge via PR after testing, deployed to the same live product — not a separate demo-only app.

### Day 57–59: Final polish
- Update architecture diagram to show the ML microservice component and how it connects.
- README v3 (final — all 16 sections per capstone Section 12.1: title/tagline, live+video demo links, overview, architecture diagram, tech stack table, features, screenshots, getting started, env vars table, API docs link, running tests, deployment, folder structure, future enhancements, license, author).
- 2–4 minute demo video: core platform first, then the AI suggestion flow.
- Final `CHANGELOG.md` entry.

**Commit check:** ≥3/week Weeks 7–8, ≥2 for partial Week 9 — cumulative ≥26 commits for the whole program.

---

## REVIEW-III — Day 60: Final Review

Be ready to explain confidently:
- Why the core workflow (request/report state machines) was built AI-free first, and why that satisfies "real business logic" on its own
- Why the AI enhancement is a separate Python microservice rather than forced into Java, and the trade-offs of that polyglot decision
- Why the AI suggestion is human-in-the-loop (officer reviews/edits) rather than auto-sent
- Your model's real accuracy and its honest limitations
- What you'd build next with more time (disease-image classification, advisory chatbot — good answers that show forward thinking without overclaiming what's already built)

---

## Definition of Done — Apply to Every PR (capstone Section 11)
- Self-reviewed against the checklist before merging
- No hardcoded secrets/passwords/API keys anywhere
- No leftover `System.out.println`/`console.log` debug statements
- All tests passing, no red build merged to `main`
- No unhandled errors in browser console or backend logs during manual testing
- UI checked on both mobile and desktop widths
- Conventional Commit messages
- Merged only via PR, never direct push to `main`
- README updated if the change affects setup, features, or usage

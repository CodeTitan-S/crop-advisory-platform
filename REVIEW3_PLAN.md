# Review 3 Implementation Plan

## PART 1 — Remaining Core Features

### 1.1 Season Log Module (Farmer)
- **Backend**: DTO, Service with ownership checks, Controller endpoints, Repository method, `@JsonIgnore` on farm.
- **Frontend**: List & Form components, routes, dashboard tab.
- **Tests**: Service test for success, farm not found, authorization, empty list.

### 1.2 Knowledge Base Module (Admin)
- **Backend**: DTO, Service (CRUD + search), Controller endpoints, Repository method.
- **Frontend**: List & Form components, admin dashboard tabs, routes.
- **Tests**: Service test for CRUD and admin authorization.

### 1.3 Admin — User Management
- **Backend**: AdminUserController & Service (list, detail, role change, deactivate).
- **Frontend**: UserManagement table with role dropdowns and delete button.
- **Route**: `/admin/users`.

### 1.4 Admin — Analytics Dashboard
- **Backend**: AnalyticsController with JPQL aggregation endpoints (requests per week, disease frequency, status breakdown, summary).
- **Frontend**: Analytics.jsx with summary cards and tables; optional Recharts.
- **Route**: `/admin/analytics`.

## PART 2 — AI Crop Recommendation Engine (Phase 3)

### 2.1 Proposal Document
- **File**: `docs/Enhancement_Proposal.md`
- **Content**: Problem, solution (ML model → ranked crop list attached to advisory request), tech choice (Python/FastAPI microservice justification), dataset, model, integration, human-in-the-loop.

### 2.2 Train the Model
- **Notebook**: `ml/train_model.ipynb`
- **Steps**: Load Kaggle dataset, EDA, train/test split, Random Forest, evaluate, export model & label encoder with joblib.
- **Artifacts**: `ml/model/crop_model.pkl`, `ml/model/label_encoder.pkl`, model card.

### 2.3 FastAPI Microservice
- **Structure**:
  ```
  ml-service/
    app/
      main.py
      model_loader.py
      schemas.py
    model/
      crop_model.pkl
      label_encoder.pkl
    requirements.txt
    Dockerfile
    README.md
  ```
- **main.py**: Health endpoint, `/predict` endpoint returning top 3 crops with confidence.
- **Deploy**: Render as separate Python Web Service.

### 2.4 Spring Boot Integration
- **Field**: `AdvisoryRequest.aiSuggestion` (String, stores JSON/comma-separated).
- **Client**: `MlServiceClient` using `RestTemplate`/`WebClient`.
- **Service**: `AdvisoryRequestService.suggestCrop(Long requestId)` fetches latest soil reading, calls ML service, saves suggestion.
- **Endpoint**: `POST /api/advisory-requests/{id}/suggest-crop` (OFFICER only).
- **Config**: `app.ml-service.url` from environment.

### 2.5 Frontend — Officer AI Suggestion UI
- In `AdvisoryQueue.jsx` (ASSIGNED status):
  - "Get AI Suggestion" button → calls backend endpoint.
  - Display returned crops with confidence.
  - "Use this suggestion" button pre-fills response textarea.
  - Officer can edit before responding.

### 2.6 Model Card
- **File**: `docs/Model_Card.md`
- **Content**: Dataset details, model type, accuracy, confusion matrix, limitations, ethical considerations.

### 2.7 Tests
- **Backend**: `MlServiceClientTest` (mock RestTemplate), enhanced `AdvisoryRequestServiceTest` (suggestCrop success/failure).
- **Optional**: Python `pytest` for ML service.

### 2.8 Architecture Diagram Update
- Add ML microservice box with solid line to Spring Boot, label REST/JSON.

## PART 3 — Testing Gaps

### 3.1 Integration Tests
- Add `@SpringBootTest` + `MockMvc` tests:
  - `AuthControllerIntegrationTest` (signup → login → use token).
  - `AdvisoryRequestFlowIntegrationTest` (full lifecycle via HTTP).
- Use H2 in-memory DB; add test dependency and `application-test.properties`.
- Annotate with `@ActiveProfiles("test")`.

### 3.2 Coverage Report
- Ensure JaCoCo plugin configured in `pom.xml` (already present).
- Target: ≥50% overall coverage, ≥70% on service layer.

## PART 4 — CI/CD Enhancement

### 4.1 Update GitHub Actions
- **backend.yml**: Split into test and deploy jobs; deploy on main push via Render hook; upload JaCoCo report as artifact.
- **ml-service.yml**: Python CI (install deps, run pytest, deploy to Render).
- **frontend.yml**: Ensure `npm run build` passes on PR.

### 4.2 Secrets Management
- Verify all secrets are GitHub Secrets:
  - `RENDER_DEPLOY_HOOK_BACKEND`
  - `RENDER_DEPLOY_HOOK_ML`
  - `VITE_API_BASE_URL` (set in Vercel dashboard, not committed).

## PART 5 — Documentation & Polish

### 5.1 README v3 (Final)
- 16 sections per capstone Section 12.1:
  - Title + tagline
  - Live demo links (backend, frontend, ML service)
  - Video demo link
  - Overview
  - Architecture diagram
  - Tech stack table
  - Features list
  - Screenshots (farmer dashboard, officer queue, AI suggestion, Swagger)
  - Getting started (local setup)
  - Environment variables table
  - API docs link (Swagger)
  - Running tests
  - Deployment steps
  - Folder structure
  - Future enhancements
  - License + author

### 5.2 Update All Diagrams
- **Architecture**: Add ML microservice (solid line).
- **ER**: Verify as-built.
- **Class**: Add `MlServiceClient`, `AnalyticsService`, `KnowledgeBaseService`, `SeasonLogService`.
- **Sequence**: AI suggestion flow (Officer → Frontend → Backend → ML Service → Backend → Frontend).

---
**Next Steps**: Begin with PART 1.1 (Season Log) to complete core features, then proceed to PART 2 (AI Engine).
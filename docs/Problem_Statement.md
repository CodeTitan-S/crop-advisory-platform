# Problem Statement

## 1. Title
Crop Advisory & Farming Guidance Platform

## 2. Domain
AgriTech

## 3. Who is the user? (2-3 user types, with roles)
- **Farmer** — creates farm profiles, logs soil readings and season history, submits advisory requests and disease reports, views officer responses.
- **Agricultural Officer** — views advisory requests and disease reports assigned to them, responds with guidance, updates status, reviews AI-suggested crop recommendations before sending them to farmers.
- **Admin** — manages user accounts, curates the crop/disease knowledge base, views platform-wide analytics.

## 4. What problem are we solving? (3-5 sentences, real-life example)
Farmers frequently lack timely, personalized access to agricultural advisory support — decisions about what to plant, how to interpret soil conditions, and how to respond to crop disease are often made on tradition or guesswork rather than data, especially in areas with limited access to agronomists. On the other side, agricultural extension officers have no structured system to track, prioritize, and respond to farmer requests at scale — requests come in informally (phone calls, in-person visits) and nothing is recorded for future reference. For example, a farmer in a rural district may notice unusual spotting on their crop leaves but has no fast way to get expert input, while the local officer serving hundreds of farmers has no way to triage which requests are most urgent. This platform gives both sides a structured, trackable advisory workflow, with a data-driven crop recommendation feature added as an enhancement to reduce officer guesswork.

## 5. Proposed Solution (what the application will do, feature-wise)
- Farmer signup/login with role-based access (Farmer / Officer / Admin)
- Farm profile management (location, size, soil type) per farmer
- Soil reading logging (N, P, K, pH, rainfall, temperature) per farm, over time
- Season log — record what was planted each season and the outcome
- **Advisory Request workflow** — farmer submits a request; it is assigned to an officer; officer responds; farmer views the response. Status: `PENDING → ASSIGNED → RESPONDED → CLOSED`
- **Disease Report workflow** — farmer submits a description (and optionally a photo) of a crop issue; officer reviews and resolves it. Status: `REPORTED → UNDER_REVIEW → RESOLVED`
- Officer dashboard — queue of assigned requests/reports, sortable by status and age
- Admin dashboard — manage users, curate a knowledge base of crop/disease reference entries, view basic analytics (requests per week, most common disease reports)
- **Enhancement (Day 42-60): AI-Powered Crop Recommendation Engine** — given a farm's latest soil reading, a trained model suggests a ranked list of suitable crops; this suggestion is attached to the relevant `AdvisoryRequest` as a draft the officer reviews and can edit before responding — human-in-the-loop, not an auto-decision.

## 6. Core Entities / Database Tables (list all, minimum 5)
1. `User` (id, name, email, password_hash, role)
2. `Farm` (id, user_id FK, location, size, soil_type)
3. `SoilReading` (id, farm_id FK, nitrogen, phosphorus, potassium, ph, rainfall, temperature, recorded_at)
4. `SeasonLog` (id, farm_id FK, crop_planted, sowing_date, outcome_notes)
5. `AdvisoryRequest` (id, farmer_id FK, officer_id FK nullable, farm_id FK, question_text, status, ai_suggestion nullable, created_at, responded_at)
6. `DiseaseReport` (id, farmer_id FK, officer_id FK nullable, farm_id FK, description, image_url nullable, status, resolution_notes, created_at, resolved_at)
7. `KnowledgeBaseEntry` (id, crop_or_disease_name, description, remedy_or_advice, source, created_by_admin_id FK)

7 tables, real foreign-key relationships (One-to-Many throughout: User→Farm, Farm→SoilReading/SeasonLog/AdvisoryRequest/DiseaseReport), comfortably clearing the ≥5 requirement.

## 7. User Roles & Permissions (minimum 2 distinct roles, e.g. Admin & User)
| Action | Farmer | Officer | Admin |
|---|---|---|---|
| Manage own farm profiles | ✅ | ❌ | ❌ |
| Log soil readings / season history | ✅ (own farms) | ❌ | ❌ |
| Submit advisory request / disease report | ✅ | ❌ | ❌ |
| View/respond to assigned requests | ❌ | ✅ (assigned only) | ✅ (all) |
| View AI crop suggestion, accept/edit before sending | ❌ | ✅ | ✅ |
| Manage users | ❌ | ❌ | ✅ |
| Curate knowledge base | ❌ | ❌ | ✅ |
| View platform-wide analytics | ❌ | ❌ (own queue only) | ✅ |

## 8. Success Criteria
- A farmer should be able to submit an advisory request in under 1 minute.
- An officer should see their assigned request queue sorted by status and age, and be able to respond to a request in under 2 minutes.
- A farmer should be able to create a farm profile and log a soil reading in under 2 minutes combined.
- By Day 41, the full request/report workflow (both status state machines) should be demonstrable live, end-to-end, on a public URL.
- By Day 60, an officer should be able to view an AI-suggested crop recommendation for a request, edit it if needed, and send the response — with the suggestion visibly grounded in the farm's actual latest soil reading.

## 9. Out of Scope (clearly list what you will NOT build, to avoid over-commitment)
- Payments or any marketplace/e-commerce functionality
- IoT sensor integration (soil sensors, drones) — soil data is entered manually
- Multilingual or voice UI
- AI-based disease image classification (computer vision) — noted as a future enhancement beyond this capstone; the Day 42-60 AI enhancement is scoped to the crop recommendation model only
- Real-time chat between farmer and officer (advisory is async request/response, not live chat)
- Mobile native app — web only, responsive design for mobile browsers

## 10. Chosen Track: Java (Spring Boot)

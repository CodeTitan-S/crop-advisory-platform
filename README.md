# Crop Advisory & Farming Guidance Platform

## Overview
A web platform connecting farmers with agricultural officers for advisory requests, disease reporting, and data-driven crop recommendations.

## Tech Stack
- **Frontend:** React + Tailwind CSS + Axios
- **Backend:** Spring Boot 3.x (Java 17), Spring Security + JWT
- **Database:** PostgreSQL 15
- **Docs:** Swagger UI (springdoc-openapi)

## Local Development
### Prerequisites
- Java 17, Maven, Node.js 18+, PostgreSQL 15

### Setup
1. Clone the repo and copy `.env.example` to `.env`, fill in your local DB credentials and a JWT secret.
2. Backend: `./mvnw spring-boot:run`
3. Frontend: `cd frontend && npm install && npm run dev`
4. Access API docs at `http://localhost:8080/swagger-ui.html`
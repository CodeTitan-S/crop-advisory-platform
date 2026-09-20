# Deployment Guide — Render (backend) + Vercel (frontend)

Covers deploying the `cropadvisory` Spring Boot backend to Render with PostgreSQL, deploying the
React frontend to Vercel, and resolving the issues we hit along the way.

Throughout this guide, replace `https://<your-backend>.onrender.com` and
`https://<your-frontend>.vercel.app` with your actual service URLs.

## Prerequisites

- GitHub repository connected to Render and Vercel
- Render account (the free tier works)
- Java 17 / Node 18+ locally, for smoke-testing before you deploy

---

## Backend deployment

### Option A: Blueprint (recommended)

`render.yaml` in the repository root defines both the PostgreSQL database and the web service.

1. Render Dashboard → **Blueprints** → **New Blueprint Instance**
2. Connect your GitHub repository
3. Render detects `render.yaml` and provisions:
   - PostgreSQL database `cropadvisory-db`
   - Web service `cropadvisory-backend` (Docker runtime), with `DATABASE_URL` wired from the database
4. After the frontend is deployed, update `FRONTEND_URL` on the web service to your real frontend URL
5. Click **Apply** and wait for the build (first build takes 3–5 minutes)

The checked-in blueprint already sets `SPRING_PROFILES_ACTIVE=prod`, generates `JWT_SECRET`,
and reads `DATABASE_URL` directly from the provisioned database — no manual wiring needed.

### Option B: Manual setup

#### 1. Create the database

1. **New +** → **PostgreSQL**
2. Name `cropadvisory-db`, Database `crop_advisory`, Region Oregon, Plan Free
3. Copy the **Internal Database URL** (format `postgresql://user:pass@host:port/db`)

#### 2. Create the web service

1. **New +** → **Web Service**, connect your repository
2. Configure:
   - **Root Directory**: `backend`
   - **Environment**: Docker
   - **Dockerfile Path**: `backend/Dockerfile`
   - **Health Check Path**: `/actuator/health`
   - **Auto-Deploy**: Yes
3. Add the environment variables listed below
4. Click **Create Web Service**

Using the native **Java** runtime instead of Docker also works: set Build Command
`./mvnw clean package -DskipTests`, Start Command
`java -jar target/cropadvisory-0.0.1-SNAPSHOT.jar`, and `JAVA_VERSION=17`.

---

## Environment variables

### Render (backend)

| Variable | Required | Example / Notes |
|----------|----------|-----------------|
| `DATABASE_URL` | yes | `postgresql://user:pass@host:5432/crop_advisory` — Render's Internal Database URL. Auto-wired by the blueprint. |
| `JWT_SECRET` | yes | Base64 key of at least 256 bits. `generateValue: true` in the blueprint. |
| `FRONTEND_URL` | yes | Frontend origin(s) allowed by CORS. Comma-separated for multiple, no trailing slash. |
| `SPRING_PROFILES_ACTIVE` | no | Set to `prod` for production logging and `open-in-view=false` |
| `JWT_EXPIRATION_MS` | no | Default `86400000` (24 hours) |
| `DDL_AUTO` | no | Default `update`. Use `validate` in production. |
| `SHOW_SQL` | no | Default `false`. Set `true` to debug SQL. |
| `PORT` | no | Default `8080`. Render injects this automatically. |

`DataSourceConfig` normalizes the URL it receives, so it accepts `postgres://`, `postgresql://`, or
already-JDBC-form URLs, from `DATABASE_URL`, `DB_URL`, or `SPRING_DATASOURCE_URL` (in that precedence).
As an alternative to a single `DATABASE_URL`, you may set `DB_URL` + `DB_USER` + `DB_PASSWORD`.

### Vercel (frontend)

| Variable | Example |
|----------|---------|
| `VITE_API_BASE_URL` | `https://<your-backend>.onrender.com/api` |

`frontend/vercel.json` handles SPA routing (React Router deep links) — without it, direct navigation
to routes like `/farmer` returns a 404 from Vercel.

### Generating a JWT secret

Any of these produce a suitable 256-bit key:

```bash
openssl rand -base64 32                                              # Linux/Mac
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## Verifying the deployment

```bash
# Health check — should report status UP with db UP
curl https://<your-backend>.onrender.com/actuator/health

# Signup
curl -X POST https://<your-backend>.onrender.com/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123","role":"FARMER"}'

# Login
curl -X POST https://<your-backend>.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# CORS preflight for the frontend origin — expect 200 with Access-Control-Allow-Origin
curl -i -X OPTIONS https://<your-backend>.onrender.com/api/auth/signup \
  -H "Origin: https://<your-frontend>.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type"
```

Then exercise signup/login in the deployed frontend UI.

---

## Troubleshooting

### CORS errors / signup or login fails from the frontend

The most common cause: `FRONTEND_URL` on the Render service does not include the frontend origin.
`SecurityConfig` only allows origins listed in that variable.

**Fix:** set `FRONTEND_URL` to your exact frontend origin, comma-separated for multiple, e.g.

```
https://<your-frontend>.vercel.app,http://localhost:5173
```

No trailing slash. Saving the variable triggers an automatic redeploy. Confirm with the preflight
`curl` above — a blocked origin returns `403`, a correct one returns `200` with
`Access-Control-Allow-Origin`.

### "Failed to connect to database"

1. Is the PostgreSQL instance running in the Render dashboard?
2. Is `DATABASE_URL` the **Internal** Database URL (not External)?
3. Does the URL contain credentials (`postgresql://user:pass@host:port/db`)?

`DataSourceConfig` converts `postgresql://` to `jdbc:postgresql://` and extracts embedded
credentials, so pasting Render's URL as-is is correct. Check logs with `render logs -s cropadvisory-backend`.

### "JWT signing key must be at least 256 bits"

Regenerate the secret with one of the commands above and update `JWT_SECRET`.

### Build fails with "mvnw: Permission denied"

The Maven wrapper needs the execute bit committed:

```bash
git update-index --chmod=+x backend/mvnw
git commit -m "fix: make mvnw executable"
git push
```

The `Dockerfile` also runs `chmod +x mvnw` before building.

### Health check fails

- Path must be exactly `/actuator/health` (no `/api` prefix)
- Allow 2–3 minutes for the first cold start

### Build succeeds but the app won't start

Check that:
1. The container exposes port 8080 (`Dockerfile`)
2. `server.port=${PORT:8080}` picks up Render's injected `PORT`
3. The app binds `0.0.0.0`, not `localhost`

### "Cannot reach server" from the frontend

Render's free tier spins down after ~15 minutes of inactivity; the first request then takes 30–60
seconds. The login/signup pages distinguish this network case from a server error, so tell users to
wait and retry.

---

## Render free tier limits

- **Spin down** after 15 minutes idle, with a 30–60 second cold start
- **Database**: 1 GB storage, 1 million rows, expires after 90 days
- **Build minutes**: 500/month

To keep the service warm, point an uptime monitor (e.g. UptimeRobot) at `/actuator/health` every
10 minutes.

Connection pooling is already tuned for this tier in `DataSourceConfig`: max pool 5, min idle 1,
30s connection timeout.

---

## CI/CD

`.github/workflows/backend.yml` runs on pushes and pull requests touching `backend/**`:

- Runs the backend test suite against a temporary `postgres:15-alpine` service
- Builds the JAR
- Uploads surefire reports and the JAR as build artifacts

Run the same suite locally with:

```bash
cd backend && ./mvnw test
```

---

## Hardening for production

1. Upgrade off the free Render plan (no spin-down)
2. Set `DDL_AUTO=validate`
3. Rotate `JWT_SECRET`; use strong database credentials
4. Restrict CORS to HTTPS origins only
5. Enable database backups (included on paid Render PostgreSQL plans)
6. Add external monitoring/metrics

---

## Resources

- [Render Java deployment](https://render.com/docs/deploy-java) · [Environment variables](https://render.com/docs/environment-variables) · [PostgreSQL](https://render.com/docs/databases) · [Status](https://status.render.com)
- [Spring Boot CORS guide](https://spring.io/guides/gs/rest-service-cors/) · [Production best practices](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [Vercel environment variables](https://vercel.com/docs/concepts/projects/environment-variables)

# Deployment Guide — Render (backend) + Vercel (frontend)

Covers deploying the `cropadvisory` Spring Boot backend to Render with PostgreSQL, deploying the
React frontend to Vercel, and resolving the issues we hit along the way.

Throughout this guide, replace `https://<your-backend>.onrender.com` and
`https://<your-frontend>.vercel.app` with your actual service URLs.

## Prerequisites

- GitHub repository connected to Render and Vercel
- Render account (the free tier works)
- Java 17 and Node.js 20.19+ (or 22.12+) locally, for smoke-testing before you deploy

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

## Frontend deployment (Vercel)

The frontend is a static Vite build, so Vercel needs only the build settings and one environment
variable.

1. **Import the repository** — Vercel Dashboard → **Add New…** → **Project**, then connect the
   GitHub repository.
2. **Set the Root Directory to `frontend`.** This repository is a monorepo, so without this Vercel
   looks for a `package.json` at the root and the build fails.
3. **Add the environment variable** for all three environments (**Production**, **Preview** and
   **Development**) under Settings → Environment Variables:

   | Key | Value |
   |-----|-------|
   | `VITE_API_BASE_URL` | `https://<your-backend>.onrender.com/api` |

   The `/api` suffix is required — every Spring controller is mapped under `/api/...`.
4. **Deploy.** The build runs `npm run build` (`vite build`) and publishes `dist/`, both declared in
   `frontend/vercel.json` along with the SPA rewrite rules.

`frontend/vercel.json` rewrites every path to `index.html`. Without it, navigating directly to a
route such as `/farmer` — or refreshing while on it — returns a Vercel 404 even though the app works
from the landing page.

### Changing the API URL requires a redeploy

Vite only exposes variables prefixed with `VITE_`, and inlines their values into the JavaScript
bundle during `vite build`. Editing `VITE_API_BASE_URL` in the Vercel dashboard therefore does **not**
take effect until a new build runs: trigger a redeploy (Deployments → ⋯ → **Redeploy**) rather than
expecting a restart to pick it up.

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
| `APP_UPLOAD_DIR` | no | Default `uploads`. Where uploaded report photos are written. Render's free tier has an ephemeral filesystem, so files are lost on redeploy/restart — see "Uploaded photos 404" below. |
| `ADMIN_EMAIL` | no | Email for the bootstrapped admin account. Requires `ADMIN_PASSWORD`. Declared `sync: false` in the Blueprint, so set it in the dashboard. |
| `ADMIN_PASSWORD` | no | Password for the bootstrapped admin account. Set both or neither. |
| `PORT` | no | Default `8080`. Render injects this automatically. |

`DataSourceConfig` normalizes the URL it receives, so it accepts `postgres://`, `postgresql://`, or
already-JDBC-form URLs, from `DATABASE_URL`, `DB_URL`, or `SPRING_DATASOURCE_URL` (in that precedence).
As an alternative to a single `DATABASE_URL`, you may set `DB_URL` + `DB_USER` + `DB_PASSWORD`.

### Creating the first admin

Signup only offers `FARMER` and `OFFICER`, so set `ADMIN_EMAIL` and `ADMIN_PASSWORD` to have
`AdminSeeder` create the first admin account on startup. It runs only when both are set and no admin
exists yet, so redeploys are safe. If the email already belongs to a non-admin user, the seeder
leaves it alone rather than promoting it — use the admin user list to change that role.

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

Render's free tier spins down after ~15 minutes of inactivity; the first request then takes up to
~90 seconds to wake. The login/signup pages distinguish this network case from a server error, so
tell users to wait and retry. Opening `/actuator/health` a minute or two before a demo avoids it
entirely.

### Frontend changes not appearing after a deploy

Vercel is serving a cached build. Clear the build cache (Settings → General → **Clear Cache**) and
redeploy.

### Uploaded photos 404 after a deploy

Render's filesystem is ephemeral on the free plan: an uploaded report photo lives on the instance's
disk, so it disappears on redeploy, restart or spin-down and `GET /api/files/{name}` then returns
404. Re-upload the photo, or make storage durable:

- attach a persistent disk and set `APP_UPLOAD_DIR` to its mount path (paid plans), or
- replace `FileStorageService` with an object-store client (S3/Cloudinary) — it is the only class
  that touches the filesystem, and it returns the URL stored on the report.

### 404 when opening or refreshing a deep link

`frontend/vercel.json` must exist with its `rewrites` rule so every path resolves to `index.html`.
Check that Vercel's **Root Directory** is set to `frontend`, or the file will not be found at all.

### The frontend builds but every API call fails

Almost always the environment variable. Confirm `VITE_API_BASE_URL` ends with `/api`, points at the
*current* backend hostname, and that you redeployed after changing it — see "Changing the API URL
requires a redeploy" above.

---

## Render free tier limits

- **Spin down** after 15 minutes idle, with a cold start of up to ~90 seconds
- **Database**: 1 GB storage, 1 million rows, expires after 90 days
- **Build minutes**: 500/month

To keep the service warm, point an uptime monitor (e.g. UptimeRobot) at `/actuator/health` every
10 minutes.

Connection pooling is already tuned for this tier in `DataSourceConfig`: max pool 5, min idle 1,
30s connection timeout.

---

## CI/CD

Two workflows run on pushes and pull requests, each scoped to its own directory by a `paths` filter.

`.github/workflows/backend.yml` (touching `backend/**`):

- Runs the backend test suite against a temporary `postgres:15-alpine` service
- Enforces the JaCoCo 40% service-layer coverage gate, and logs the measured figure
- Builds the JAR
- Uploads surefire reports, the JaCoCo report, and the JAR as build artifacts

`.github/workflows/frontend.yml` (touching `frontend/**`):

- Runs `npm ci`, `npm run lint` and `npm run build` on Node 22
- Fails the run on either a lint error or a build failure
- Uploads the `dist` build artifact

Run the same checks locally with:

```bash
cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```

Neither workflow deploys anything: Render and Vercel each deploy from `main` through their own Git
integration, so there are no deploy hooks or tokens in GitHub Actions.

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
- [Vercel environment variables](https://vercel.com/docs/concepts/projects/environment-variables) · [Deploying a React app with Vercel](https://vercel.com/guides/deploying-react-with-vercel)
- [Vite environment variables and modes](https://vite.dev/guide/env-and-mode)

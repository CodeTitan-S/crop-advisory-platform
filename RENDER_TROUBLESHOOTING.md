# Render Deployment Troubleshooting Guide

## What We Fixed

### 1. **DataSource Configuration** (`DataSourceConfig.java`)
- **Problem**: Render provides PostgreSQL URLs in format `postgresql://user:pass@host:port/db`, but Spring Boot JDBC requires `jdbc:postgresql://host:port/db`
- **Solution**: Created smart `DataSourceConfig` that automatically:
  - Detects cloud PostgreSQL URLs (`postgres://` or `postgresql://`)
  - Normalizes them to JDBC format (`jdbc:postgresql://`)
  - Extracts username/password from the URL if embedded
  - Falls back to separate `DB_USER` and `DB_PASSWORD` if provided
  - Works with `DATABASE_URL` (Render's default), `DB_URL`, or `SPRING_DATASOURCE_URL`

### 2. **CORS Configuration** (`SecurityConfig.java`)
- **Problem**: CORS origins were hardcoded to `localhost:5173`
- **Solution**: Now reads from `FRONTEND_URL` environment variable (comma-separated for multiple origins)

### 3. **Dockerfile Build**
- **Problem**: `mvnw` didn't have execute permissions in Docker context
- **Solution**: Added `RUN chmod +x mvnw` and marked file as executable in git
- **Problem**: `dependency:go-offline` was failing in Alpine Linux
- **Solution**: Removed problematic step, build completes in one pass

### 4. **Render Blueprint** (`render.yaml`)
- **Problem**: Blueprint was using `DB_URL`, `DB_USER`, `DB_PASSWORD` separately
- **Solution**: Now uses single `DATABASE_URL` that Render automatically provides
- **Added**: `SPRING_PROFILES_ACTIVE=prod` to activate production profile

### 5. **Production Profile** (`application-prod.properties`)
- Optimized logging (less verbose)
- Disabled `open-in-view` (performance)
- Production-ready settings

---

## Deployment Steps

### Option A: Retry Blueprint Deployment (Recommended)

1. **Delete the failed web service** from Render dashboard if it exists
2. Go to **Render Dashboard** → **Blueprints** → **New Blueprint Instance**
3. Connect your `crop-advisory-platform` repository
4. Render will detect `render.yaml` and provision:
   - PostgreSQL database (`cropadvisory-db`)
   - Web service (`cropadvisory-backend`)
5. **Wait for deployment** (first build takes 3-5 minutes)
6. **Update `FRONTEND_URL`** after deployment with your actual frontend URL

### Option B: Manual Deployment

If Blueprint continues to fail, deploy manually:

#### Step 1: Create PostgreSQL Database
1. **New +** → **PostgreSQL**
2. Name: `cropadvisory-db`
3. Database: `crop_advisory`
4. Region: Oregon
5. Plan: Free
6. **Create Database**
7. **Copy Internal Database URL** (format: `postgresql://user:pass@host:port/dbname`)

#### Step 2: Create Web Service
1. **New +** → **Web Service**
2. Connect repository: `crop-advisory-platform`
3. **Root Directory**: `backend`
4. **Environment**: Docker
5. **Dockerfile Path**: `backend/Dockerfile`

**Environment Variables:**

| Key | Value |
|-----|-------|
| `DATABASE_URL` | *(Paste the Internal Database URL from Step 1)* |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JWT_SECRET` | *(Generate: `openssl rand -base64 32`)* |
| `JWT_EXPIRATION_MS` | `86400000` |
| `FRONTEND_URL` | `https://your-frontend.onrender.com,http://localhost:5173` |
| `DDL_AUTO` | `update` |

6. **Health Check Path**: `/actuator/health`
7. **Create Web Service**

---

## Verifying Deployment

Once deployed, test these endpoints:

### 1. Health Check
```bash
curl https://cropadvisory-backend.onrender.com/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2. Signup Endpoint
```bash
curl -X POST https://cropadvisory-backend.onrender.com/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123",
    "role": "FARMER"
  }'
```

### 3. Login Endpoint
```bash
curl -X POST https://cropadvisory-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

---

## Common Issues & Solutions

### Issue: "Failed to connect to database"

**Check:**
1. Is the PostgreSQL database running? (Check Render dashboard)
2. Is `DATABASE_URL` set correctly?
3. Does the URL include credentials? (should look like `postgresql://user:pass@host:port/db`)

**Solution:**
- Ensure you're using the **Internal Database URL** (not External)
- The `DataSourceConfig` automatically normalizes the URL format
- Check logs: `render logs -s cropadvisory-backend`

### Issue: "JWT signing key must be at least 256 bits"

**Solution:**
Generate a proper secret:
```bash
openssl rand -base64 32
```
Update `JWT_SECRET` environment variable in Render dashboard.

### Issue: "CORS error from frontend"

**Solution:**
1. Check `FRONTEND_URL` environment variable includes your frontend URL
2. Format: `https://your-frontend.onrender.com` (no trailing slash)
3. Multiple origins: comma-separated `https://frontend.com,http://localhost:5173`

### Issue: "Application crashes on startup"

**Check Render logs:**
```bash
render logs -s cropadvisory-backend
```

**Common causes:**
- Missing required environment variable
- Database not ready (wait 30s after DB creation)
- JWT_SECRET too short (needs 256 bits minimum)

### Issue: "Health check fails"

**Solution:**
- Health check path must be `/actuator/health` (no prefix like `/api`)
- Wait 2-3 minutes for first cold start
- Check logs for startup errors

### Issue: "Build succeeds but app won't start"

**Check:**
1. Dockerfile exposes correct port (8080)
2. `PORT` environment variable is set (Render auto-injects this)
3. Application listens on `0.0.0.0`, not `localhost`

**Solution:**
The Dockerfile and `application.properties` are already configured correctly:
- `server.port=${PORT:8080}` reads Render's `PORT` variable
- Health check is properly configured

---

## Environment Variables Reference

### Required

| Variable | Example | Notes |
|----------|---------|-------|
| `DATABASE_URL` | `postgresql://user:pass@host:5432/db` | Auto-provided by Render Blueprint |
| `JWT_SECRET` | `<base64-32-bytes>` | Generate with `openssl rand -base64 32` |
| `FRONTEND_URL` | `https://frontend.onrender.com` | For CORS, comma-separated for multiple |

### Optional (with defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `default` | Set to `prod` for production settings |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours in milliseconds |
| `DDL_AUTO` | `update` | Hibernate DDL mode: `update`, `validate`, `create-drop` |
| `SHOW_SQL` | `false` | Log SQL queries (for debugging) |
| `PORT` | `8080` | Server port (auto-injected by Render) |

---

## Performance Tips

### Render Free Tier Limitations

- **Spin Down**: Services spin down after 15 minutes of inactivity
- **Cold Start**: First request after spin-down takes 30-60 seconds
- **Database**: 1GB storage, 1M rows, expires after 90 days

### Keep Service Warm

Set up a cron job or uptime monitor (e.g., UptimeRobot) to ping the health endpoint every 10 minutes:

```bash
curl https://cropadvisory-backend.onrender.com/actuator/health
```

### Database Connection Pooling

Already optimized in `DataSourceConfig.java`:
- Max pool size: 5 connections
- Min idle: 1 connection
- Connection timeout: 30 seconds
- Optimized for Render's free tier

---

## Monitoring & Logs

### View Live Logs
```bash
# Via Render CLI (install: npm i -g render-cli)
render logs -s cropadvisory-backend -f

# Via Dashboard
Render Dashboard → cropadvisory-backend → Logs
```

### Key Metrics to Watch

1. **Response Time**: `/actuator/health` should respond in <200ms when warm
2. **Error Rate**: Check logs for exceptions
3. **Memory Usage**: Should stay under 512MB on free tier
4. **Cold Start Time**: Should be <60s after spin-down

---

## Next Steps After Deployment

1. ✅ **Test all API endpoints** with Postman or curl
2. ✅ **Update frontend** to point to your Render backend URL
3. ✅ **Set up monitoring** (UptimeRobot for health checks)
4. ✅ **Configure custom domain** (if you have one)
5. ✅ **Enable auto-deploy** on git push (already configured)

---

## Support

- **Render Status**: https://status.render.com
- **Render Community**: https://community.render.com
- **Spring Boot Docs**: https://docs.spring.io/spring-boot/docs/current/reference/html/
- **PostgreSQL Docs**: https://www.postgresql.org/docs/

---

## Files Changed Summary

| File | Purpose |
|------|---------|
| `backend/src/main/java/com/college/cropadvisory/config/DataSourceConfig.java` | **NEW**: Smart PostgreSQL URL normalization for cloud deployment |
| `backend/src/main/java/com/college/cropadvisory/config/SecurityConfig.java` | **UPDATED**: Dynamic CORS from environment variable |
| `backend/src/main/resources/application-prod.properties` | **NEW**: Production-optimized settings |
| `backend/Dockerfile` | **UPDATED**: Fixed mvnw permissions, simplified build |
| `render.yaml` | **UPDATED**: Uses `DATABASE_URL` instead of separate credentials |
| `backend/mvnw` | **UPDATED**: Marked as executable in git |

All 41 unit tests pass ✅
Build succeeds locally ✅
Ready for Render deployment ✅

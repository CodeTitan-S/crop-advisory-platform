# Deployment Troubleshooting Guide

## Current Issue: Signup Failed (CORS Error)

### Problem
The frontend on Vercel can load, but signup/login fails with "Signup failed" error.

### Root Cause
**CORS (Cross-Origin Resource Sharing) is blocking requests** from Vercel to the backend on Render.

The backend's CORS configuration only allows origins specified in the `FRONTEND_URL` environment variable. Currently, it doesn't include your Vercel URL.

### Solution

#### Step 1: Update Render Environment Variable

1. Go to **[Render Dashboard](https://dashboard.render.com/)**
2. Find your backend service: `cropadvisory-backend` (or similar)
3. Click on the service → Navigate to **Environment** tab
4. Find or add `FRONTEND_URL` environment variable
5. Set its value to:
   ```
   https://crop-advisory-platform.vercel.app,http://localhost:5173
   ```
6. Click **Save Changes**
7. Render will automatically trigger a redeployment (~2-3 minutes)

#### Step 2: Verify CORS is Fixed

Once Render shows "Live", test CORS with curl:

```bash
curl -I -X OPTIONS https://cropadvisory-backend-08uh.onrender.com/api/auth/signup \
  -H "Origin: https://crop-advisory-platform.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type"
```

**Expected response:** `HTTP/1.1 200 OK` with `Access-Control-Allow-Origin` header

**Current response:** `HTTP/1.1 403 Forbidden` (CORS blocked)

#### Step 3: Test Signup on Vercel

1. Go to https://crop-advisory-platform.vercel.app/signup
2. Fill in the form:
   - Name: Test User
   - Email: test@example.com
   - Password: password123
   - Role: Farmer
3. Click **Sign Up**
4. Should redirect to dashboard on success

---

## Backend Configuration

### Current Setup
- **Backend URL:** `https://cropadvisory-backend-08uh.onrender.com`
- **Health Check:** `https://cropadvisory-backend-08uh.onrender.com/actuator/health`
- **Status:** ✅ UP and running (verified)

### CORS Configuration
Located in: `backend/src/main/java/com/college/cropadvisory/config/SecurityConfig.java`

```java
@Value("${app.cors.allowed-origins:http://localhost:5173}")
private String allowedOrigins;
```

This reads from `FRONTEND_URL` environment variable (mapped via `application.properties`).

---

## Environment Variables Summary

### Vercel (Frontend)
| Variable | Value |
|----------|-------|
| `VITE_API_BASE_URL` | `https://cropadvisory-backend-08uh.onrender.com/api` |

### Render (Backend)
| Variable | Value |
|----------|-------|
| `FRONTEND_URL` | `https://crop-advisory-platform.vercel.app,http://localhost:5173` |
| `DATABASE_URL` | (From Render PostgreSQL) |
| `JWT_SECRET` | (Auto-generated) |
| `DDL_AUTO` | `update` |

---

## Common Issues

### 1. "Cannot reach server" Error
**Cause:** Render free tier spins down after inactivity. First request takes 30-60 seconds to wake up.

**Solution:** Wait and retry. The improved error message now tells users this.

### 2. "403 Forbidden" in Browser Console
**Cause:** CORS not configured properly on backend.

**Solution:** Follow Step 1 above to add Vercel URL to `FRONTEND_URL`.

### 3. Vercel Shows 404/NOT_FOUND
**Cause:** Missing `vercel.json` for SPA routing.

**Solution:** Already fixed! `frontend/vercel.json` is now in place.

### 4. Environment Variables Not Working
**Cause:** Variables added after deployment need a rebuild.

**Solution:** 
- Vercel: Settings → Deployments → Redeploy
- Render: Automatically redeploys when you save environment changes

---

## Verification Commands

### Test Backend Health
```bash
curl https://cropadvisory-backend-08uh.onrender.com/actuator/health
```

### Test Backend Signup (Direct)
```bash
curl -X POST https://cropadvisory-backend-08uh.onrender.com/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"pass123","role":"FARMER"}'
```

### Test CORS
```bash
curl -I -X OPTIONS https://cropadvisory-backend-08uh.onrender.com/api/auth/signup \
  -H "Origin: https://crop-advisory-platform.vercel.app" \
  -H "Access-Control-Request-Method: POST"
```

---

## Recent Changes

### Commit: `fix: configure Vercel deployment with SPA routing and environment variables`
- ✅ Added `vercel.json` for React Router SPA support
- ✅ Made API URL configurable via `VITE_API_BASE_URL`
- ✅ Updated `render.yaml` with Vercel URL

### Commit: `feat: improve error handling in Login and Signup pages`
- ✅ Better error messages distinguishing network/CORS/server errors
- ✅ Inform users when backend is spinning up
- ✅ Console logging for debugging

---

## Next Steps After Fixing CORS

1. ✅ Update `FRONTEND_URL` on Render (see Step 1 above)
2. ⏳ Wait for Render to redeploy (~2-3 min)
3. 🧪 Test signup on Vercel
4. 🎉 Should work!

---

## Resources

- [Render Environment Variables](https://render.com/docs/environment-variables)
- [Spring Boot CORS Configuration](https://spring.io/guides/gs/rest-service-cors/)
- [Vercel Environment Variables](https://vercel.com/docs/concepts/projects/environment-variables)

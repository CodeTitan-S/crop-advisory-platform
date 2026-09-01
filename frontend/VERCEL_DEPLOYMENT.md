# Vercel Deployment Guide for Frontend

## Issue Fixed
The "Code: NOT_FOUND" error was caused by:
1. Missing Vercel configuration for React SPA routing
2. Hardcoded localhost API URL that doesn't work in production
3. Missing environment variable configuration

## Changes Made

### 1. Added `vercel.json`
This configuration ensures all routes are handled by `index.html` (required for React Router):
```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### 2. Updated API Configuration
Modified `src/api/axios.js` to use environment variables:
```javascript
baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
```

### 3. Created Environment Files
- `.env.local` - Local development (gitignored)
- `.env.example` - Template for reference

## Deployment Steps

### Step 1: Configure Environment Variables in Vercel

1. Go to your Vercel project: https://vercel.com/dashboard
2. Select your project: `crop-advisory-platform`
3. Go to **Settings** → **Environment Variables**
4. Add the following variable:

   ```
   Key: VITE_API_BASE_URL
   Value: https://agro-advisory-platform.onrender.com/api
   ```
   
   *(Replace with your actual backend URL once deployed)*

5. Select environment: **Production**, **Preview**, and **Development**
6. Click **Save**

### Step 2: Redeploy

After adding environment variables, you need to trigger a new deployment:

**Option A: Through Vercel Dashboard**
1. Go to **Deployments** tab
2. Click on the latest deployment
3. Click the three dots (⋯) menu
4. Select **Redeploy**

**Option B: Push Changes to GitHub**
```bash
git add .
git commit -m "fix: configure Vercel deployment with environment variables and SPA routing"
git push origin main
```

This will automatically trigger a new deployment on Vercel.

### Step 3: Verify Deployment

1. Wait for deployment to complete
2. Visit your site: https://crop-advisory-platform.vercel.app/
3. You should now see the React app instead of the NOT_FOUND error

## Backend URL Configuration

Currently, your backend is deployed on Render at:
- **Render URL**: `https://agro-advisory-platform.onrender.com`

Update the `VITE_API_BASE_URL` in Vercel to:
```
https://agro-advisory-platform.onrender.com/api
```

## Local Development

For local development, the `.env.local` file is already configured:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

## Troubleshooting

### Issue: Changes not reflecting
- Clear Vercel's build cache: Settings → General → Clear Cache
- Trigger a new deployment

### Issue: API calls failing with CORS errors
- Ensure your backend (Spring Boot) has CORS configured for the Vercel domain
- Check `backend/src/main/java/com/agro/config/CorsConfig.java`
- Add your Vercel URL to allowed origins:
  ```java
  .allowedOrigins("https://crop-advisory-platform.vercel.app")
  ```

### Issue: 404 on page refresh
- This is fixed by `vercel.json` rewrite rules
- Ensure `vercel.json` is in the `frontend` directory root

## Project Structure
```
frontend/
├── vercel.json          # Vercel configuration (SPA routing)
├── .env.local           # Local environment variables (gitignored)
├── .env.example         # Template for environment variables
├── vite.config.js       # Vite configuration
└── src/
    └── api/
        └── axios.js     # Updated to use environment variables
```

## Next Steps

1. **Add Vercel domain to backend CORS configuration**
2. **Set up custom domain** (optional): Vercel Settings → Domains
3. **Monitor**: Check Vercel deployment logs for any errors
4. **Test**: Verify all API endpoints work with the deployed frontend

## Resources

- [Vercel Documentation](https://vercel.com/docs)
- [Vite Environment Variables](https://vitejs.dev/guide/env-and-mode.html)
- [React Router with Vercel](https://vercel.com/guides/deploying-react-with-vercel)

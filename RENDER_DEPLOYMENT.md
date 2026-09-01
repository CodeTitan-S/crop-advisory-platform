# Deploying Spring Boot Backend to Render

This guide covers deploying the `cropadvisory` Spring Boot application to Render with PostgreSQL.

## Prerequisites

- GitHub repository connected to Render
- Render account (free tier works)

## Deployment Options

### Option 1: Blueprint (Automated - Recommended)

The `render.yaml` in the repository root defines both the PostgreSQL database and web service.

1. **Go to Render Dashboard** → **Blueprints** → **New Blueprint Instance**
2. **Connect your GitHub repository** (`agro-ae`)
3. Render will automatically detect `render.yaml` and provision:
   - PostgreSQL database (`cropadvisory-db`)
   - Web service (`cropadvisory-backend`) connected to the database
4. **Update the `FRONTEND_URL`** environment variable with your actual frontend URL once deployed
5. **Click "Apply"** and wait for deployment

**Note:** Blueprint auto-wires the database connection strings. No manual configuration needed.

---

### Option 2: Manual Setup (Step-by-Step)

#### Step 1: Create PostgreSQL Database

1. Go to **Render Dashboard** → **New** → **PostgreSQL**
2. Configure:
   - **Name**: `cropadvisory-db`
   - **Database**: `crop_advisory`
   - **User**: `postgres` (default)
   - **Region**: Oregon (or nearest to your users)
   - **Plan**: Free
3. Click **Create Database**
4. **Copy the Internal Database URL** from the database info page (format: `jdbc:postgresql://...`)

#### Step 2: Deploy the Backend Web Service

1. Go to **Render Dashboard** → **New** → **Web Service**
2. Connect your GitHub repository
3. Configure:

   **Basic Settings:**
   - **Name**: `cropadvisory-backend`
   - **Region**: Same as database (Oregon)
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: `Java`
   - **Instance Type**: Free

   **Build & Deploy:**
   - **Build Command**: 
     ```bash
     ./mvnw clean package -DskipTests
     ```
   - **Start Command**: 
     ```bash
     java -jar target/cropadvisory-0.0.1-SNAPSHOT.jar
     ```

   **Advanced:**
   - **Health Check Path**: `/actuator/health`
   - **Auto-Deploy**: Yes (on git push)

4. **Add Environment Variables** (click "Add Environment Variable"):

   | Key | Value | Notes |
   |-----|-------|-------|
   | `JAVA_VERSION` | `17` | Required for Java 17 runtime |
   | `DB_URL` | `jdbc:postgresql://<host>:<port>/<database>` | From Step 1 Internal DB URL, replace `postgresql://` with `jdbc:postgresql://` |
   | `DB_USER` | `postgres` | From database credentials |
   | `DB_PASSWORD` | `<your-db-password>` | From database credentials |
   | `JWT_SECRET` | `<generate-secure-256-bit-key>` | **Generate using command below** |
   | `JWT_EXPIRATION_MS` | `86400000` | 24 hours in milliseconds |
   | `FRONTEND_URL` | `https://your-frontend.onrender.com` | Your deployed frontend URL (for CORS) |
   | `DDL_AUTO` | `update` | Hibernate DDL mode (use `validate` in production) |
   | `SHOW_SQL` | `false` | Set to `true` for debugging SQL queries |

5. Click **Create Web Service**

---

## Generating a Secure JWT Secret

Use one of these methods to generate a strong 256-bit key:

```bash
# Option 1: OpenSSL (Linux/Mac)
openssl rand -base64 32

# Option 2: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Option 3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Option 4: Online
# Visit: https://generate-secret.vercel.app/32
```

Copy the output and use it as your `JWT_SECRET`.

---

## Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | JDBC connection string | `jdbc:postgresql://dpg-xxx.oregon-postgres.render.com:5432/crop_advisory` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `<from Render dashboard>` |
| `JWT_SECRET` | Secret key for JWT signing (min 256 bits) | `<generated-secure-key>` |
| `FRONTEND_URL` | Frontend URL for CORS | `https://your-frontend.onrender.com` |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_EXPIRATION_MS` | `86400000` | Token expiration (24 hours) |
| `DDL_AUTO` | `update` | Hibernate DDL mode: `update`, `validate`, `create-drop` |
| `SHOW_SQL` | `false` | Log SQL queries (debugging) |
| `PORT` | `8080` | Server port (Render auto-injects this) |

---

## Docker Deployment (Alternative)

If you prefer Docker instead of the native Java runtime:

1. In Render, select **Docker** as runtime
2. Set **Dockerfile Path**: `backend/Dockerfile`
3. Build Command: (leave blank - Docker builds automatically)
4. Start Command: (leave blank - uses Dockerfile ENTRYPOINT)
5. Add the same environment variables as listed above

---

## Post-Deployment

### 1. Verify Deployment

Once deployed, your backend will be available at:
```
https://cropadvisory-backend.onrender.com
```

Test the health endpoint:
```bash
curl https://cropadvisory-backend.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### 2. Update Frontend CORS Settings

If your frontend is deployed separately, update the backend's `FRONTEND_URL`:

```
FRONTEND_URL=https://your-frontend.onrender.com,http://localhost:5173
```

(Comma-separated for multiple origins)

### 3. Test API Endpoints

Example registration:
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

---

## Troubleshooting

### Issue: Build fails with "mvnw: Permission denied"

**Solution**: The Maven wrapper needs execute permissions. This is handled automatically in the build, but if you see this error, ensure your repo's `mvnw` file has execute permissions:

```bash
git update-index --chmod=+x backend/mvnw
git commit -m "fix: make mvnw executable"
git push
```

### Issue: "Connection refused" to database

**Causes:**
1. Database not running or still provisioning
2. Wrong database URL format

**Solution:**
- Ensure DB_URL starts with `jdbc:postgresql://` (not just `postgresql://`)
- Use the **Internal Database URL** from Render (not External)
- Check database status in Render dashboard

### Issue: JWT token errors

**Cause:** JWT secret is too short (< 256 bits)

**Solution:** Generate a proper 256-bit key using one of the methods above.

### Issue: CORS errors from frontend

**Cause:** Frontend URL not whitelisted

**Solution:** Add your exact frontend URL to `FRONTEND_URL` environment variable (no trailing slash).

### Issue: Application crashes on startup

**Cause:** Missing required environment variables

**Solution:** Check Render logs and ensure all required variables are set:
```bash
# View logs in Render Dashboard or via CLI
render logs -s cropadvisory-backend
```

---

## Render Free Tier Limitations

- **Spin Down**: Free web services spin down after 15 minutes of inactivity
- **First Request Delay**: Expect 30-60 seconds for cold starts
- **Database**: 1 GB storage, 1 million rows, expires after 90 days
- **Build Minutes**: 500 build minutes/month

To keep the service warm, set up a cron job or uptime monitor (e.g., UptimeRobot) to ping `/actuator/health` every 10 minutes.

---

## Upgrading to Production

When ready for production:

1. **Upgrade Render plan** to a paid tier (no spin-down, better performance)
2. **Change DDL mode** to `validate`:
   ```
   DDL_AUTO=validate
   ```
3. **Use environment-specific secrets**:
   - Rotate `JWT_SECRET`
   - Use strong database passwords
   - Enable HTTPS-only CORS
4. **Set up monitoring**:
   - Render provides built-in metrics
   - Integrate with external monitoring (Sentry, Datadog, etc.)
5. **Database backups**:
   - Render PostgreSQL paid plans include automatic daily backups
   - Set up manual backup scripts if needed

---

## CI/CD with GitHub Actions

The repository includes a GitHub Actions workflow (`.github/workflows/backend.yml`) that automatically:
- Runs all 41 unit tests on every push
- Builds the JAR artifact
- Validates the build before deployment

Render will automatically deploy when tests pass and code is pushed to `main`.

---

## Additional Resources

- [Render Java Documentation](https://render.com/docs/deploy-java)
- [Spring Boot Production Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [Render PostgreSQL Docs](https://render.com/docs/databases)

---

## Support

If you encounter issues:
1. Check Render deployment logs
2. Verify environment variables are set correctly
3. Test health endpoint: `/actuator/health`
4. Review GitHub Actions workflow for test failures

For Render-specific issues, visit [Render Community](https://community.render.com/).

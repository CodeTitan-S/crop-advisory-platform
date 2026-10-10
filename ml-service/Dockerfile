# Build stage: install dependencies into a virtualenv so we can copy just the
# site-packages into a slim runtime image without pip tools or build caches.
FROM python:3.11-slim AS build
WORKDIR /app

COPY requirements.txt ./
RUN python -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r requirements.txt

# Run stage
FROM python:3.11-slim
WORKDIR /app

COPY --from=build /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Copy the app source and the trained model artifacts. The .pkl files are
# un-ignored in .gitignore (ml-service/model only) so they reach the build
# context once committed — this keeps the image self-contained, matching the
# backend image's approach (no external secret-mounting needed).
COPY app ./app
COPY model ./model

# Create the non-root user and give it the working dir (Debian-slim syntax,
# not Alpine's `addgroup -S`)
RUN groupadd -r app && useradd -r -g app app \
    && chown -R app:app /app
USER app:app

# Render injects $PORT (must bind 0.0.0.0 on all interfaces)
ENV PORT=8000
EXPOSE 8000

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]

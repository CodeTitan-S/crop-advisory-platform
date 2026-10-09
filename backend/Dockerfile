# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper & pom
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Copy source code and build jar directly
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create the non-root user, then give it /app and the upload directory. WORKDIR is created as root,
# so without this chown FileStorageService cannot create uploads/ on startup and the app fails to
# boot (which is exactly how the photo-upload deploy failed once).
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app
USER spring:spring

COPY --chown=spring:spring --from=build /app/target/cropadvisory-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

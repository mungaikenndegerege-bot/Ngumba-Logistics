# ── Stage 1: Build the fat JAR ──────────────────────────────────────────────
FROM eclipse-temurin:11-jdk AS builder

WORKDIR /build

# Copy source and dependencies
COPY src/         ./src/
COPY lib/         ./lib/
COPY web/         ./web/

# Compile Java sources
RUN mkdir -p out && \
    javac -cp "lib/*" -d out \
      src/ngumba/logistics/DataStore.java \
      src/ngumba/logistics/ApiServer.java

# Copy web assets into classpath
RUN mkdir -p out/web/css out/web/js && \
    cp web/index.html out/web/ && \
    cp web/css/style.css out/web/css/ && \
    cp web/js/app.js out/web/js/

# Extract all dependency JARs into out/
WORKDIR /build/out
RUN for jar in /build/lib/*.jar; do jar xf "$jar"; done && \
    find . -name "module-info.class" -delete

# Write manifest and create fat JAR
RUN echo "Main-Class: ngumba.logistics.ApiServer" > MANIFEST.MF && \
    jar cfm /build/NgumbaWeb.jar MANIFEST.MF -C /build/out .

# ── Stage 2: Minimal runtime image ──────────────────────────────────────────
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /build/NgumbaWeb.jar ./NgumbaWeb.jar

# DATA_DIR is where the .dat file is stored.
# On Railway, mount a persistent volume at /data and set DATA_DIR=/data
ENV DATA_DIR=/app/data
ENV PORT=8080

RUN mkdir -p /app/data

EXPOSE 8080

CMD ["java", "-jar", "NgumbaWeb.jar"]

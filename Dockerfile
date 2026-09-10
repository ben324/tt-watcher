# Build once, run as api / internal / job.
#   docker compose up --build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY riftbound-events/riftbound-events/src /src/lib
COPY riftbound-api/src /src/api
COPY riftbound-internal/src /src/internal
RUN mkdir -p /out/api /out/internal \
 && javac -encoding UTF-8 -d /out/api \
      /src/lib/com/riftbound/events/*.java \
      /src/lib/com/riftbound/events/json/*.java \
      /src/lib/com/riftbound/events/uvs/*.java \
      /src/lib/com/riftbound/events/playriftbound/*.java \
      /src/api/com/riftbound/api/*.java \
 && javac -encoding UTF-8 -d /out/internal \
      /src/lib/com/riftbound/events/*.java \
      /src/lib/com/riftbound/events/json/*.java \
      /src/lib/com/riftbound/events/uvs/*.java \
      /src/lib/com/riftbound/events/playriftbound/*.java \
      /src/internal/com/riftbound/internal/*.java

FROM eclipse-temurin:21-jre-jammy AS api
WORKDIR /app
COPY --from=build /out/api /app/out
COPY tt-watcher /app/tt-watcher
ENV PORT=8080 \
    TT_WATCHER_DIR=/app/tt-watcher \
    RIFTBOUND_STORE=/app/data/store.json \
    INTERNAL_BASE_URL=http://internal:8081 \
    INTERNAL_API_KEY=dev-internal
EXPOSE 8080
CMD ["java", "-cp", "out", "com.riftbound.api.ApiMain"]

FROM eclipse-temurin:21-jre-jammy AS internal
WORKDIR /app
COPY --from=build /out/internal /app/out
ENV INTERNAL_PORT=8081 \
    INTERNAL_API_KEY=dev-internal
EXPOSE 8081
CMD ["java", "-cp", "out", "com.riftbound.internal.InternalMain"]

FROM api AS job
CMD ["java", "-cp", "out", "com.riftbound.api.JobMain"]

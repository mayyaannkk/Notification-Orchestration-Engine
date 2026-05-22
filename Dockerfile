
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY persistence/pom.xml persistence/
COPY core/pom.xml core/
COPY auth/pom.xml auth/
COPY cache/pom.xml cache/
COPY kafka/pom.xml kafka/
COPY channels/pom.xml channels/
COPY api/pom.xml api/


RUN apk add --no-cache maven
RUN mvn dependency:go-offline -B


COPY persistence/src persistence/src
COPY core/src core/src
COPY auth/src auth/src
COPY cache/src cache/src
COPY kafka/src kafka/src
COPY channels/src channels/src
COPY api/src api/src

RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/api/target/*.jar app.jar

USER appuser

EXPOSE 8080


ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

  
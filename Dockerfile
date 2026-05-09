FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

COPY pom.xml .
COPY settings-docker.xml .

RUN --mount=type=cache,target=/root/.m2  \
    mvn dependency:go-offline -B -s settings-docker.xml

COPY src ./src

RUN mvn clean package -DskipTests -Dcheckstyle.skip=true -Dspotbugs.skip=true -Djacoco.skip=true -s settings-docker.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
# Start with a base image containing Java runtime
FROM gradle:jdk22 AS builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle uberJar

FROM eclipse-temurin:22-jre

COPY --from=builder /home/gradle/src/build/libs/kafis.jar /app/
WORKDIR /app

CMD ["java", "-cp", "kafis.jar", "com.smartelect.AppKt"]

# =================================
# This is the builder image, which
# compiles the jar from source.
# =================================
FROM gradle:8.12-jdk21-alpine AS builder

WORKDIR /app/source
# Copy necessary stuff, which rarely changes
COPY ./gradlew ./build.gradle.kts ./settings.gradle.kts ./
COPY gradle/ ./gradle

# The source changes more often,
# so keep the layer separate.
COPY src/ ./src

# Compile the jar
RUN chmod u+x ./gradlew 
RUN ./gradlew clean build --no-daemon

# ====================================
# This is the image that will be run
# only with the jar, much smaller than
# the gradle one.
# ====================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/source/build/libs/FileHostingBackend-*-all.jar vault-backend.jar

# By default we are listening on 8181
EXPOSE 8181

ENTRYPOINT ["java", "-jar", "vault-backend.jar"]

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
# If a prebuilt jar is already in backend/target (release pipeline), reuse it;
# otherwise build everything inside Docker (local `docker build` still works).
RUN ls backend/target/fanel-backend-*.jar >/dev/null 2>&1 || mvn -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN adduser -S -D -h /app fanel
WORKDIR /app
COPY --from=build /build/backend/target/fanel-backend-*.jar app.jar
RUN mkdir /data && chown fanel:fanel /data /app
USER fanel
VOLUME /data
ENV FANEL_SQLITE_PATH=/data/fanel.db
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD ["wget", "-qO-", "http://127.0.0.1:8080/actuator/health"]
ENTRYPOINT ["java", "-jar", "app.jar"]

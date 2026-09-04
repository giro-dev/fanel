FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --home-dir /app fanel
WORKDIR /app
COPY --from=build /build/backend/target/fanel-backend-*.jar app.jar
RUN mkdir /data && chown fanel:fanel /data /app
USER fanel
VOLUME /data
ENV FANEL_SQLITE_PATH=/data/fanel.db
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD ["bash", "-c", "exec 3<>/dev/tcp/localhost/8080 && printf 'GET /actuator/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
ENTRYPOINT ["java", "-jar", "app.jar"]

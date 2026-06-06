FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S axiqra && adduser -S axiqra -G axiqra && \
    mkdir -p /var/log/axiqra && chown -R axiqra:axiqra /app /var/log/axiqra

COPY target/axiqra-start-*.jar app.jar

USER axiqra

EXPOSE 8080 9090

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]

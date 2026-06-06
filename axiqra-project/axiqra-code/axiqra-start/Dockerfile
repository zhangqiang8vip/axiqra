FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S axiqra && adduser -S axiqra -G axiqra && \
    mkdir -p /var/log/axiqra && chown -R axiqra:axiqra /app /var/log/axiqra

COPY target/axiqra-start-*.jar app.jar

USER axiqra

EXPOSE 8080 9090

ENTRYPOINT ["sh", "-c", "exec java -Dfile.encoding=UTF-8 -Duser.timezone=${APP_TIMEZONE:-UTC} -jar /app/app.jar"]

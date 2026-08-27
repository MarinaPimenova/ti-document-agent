FROM alpine/java:21-jre

LABEL org.opencontainers.image.source=https://github.com/MarinaPimenova/ti-document-agent

COPY build/libs/*.jar /app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","/app.jar"]

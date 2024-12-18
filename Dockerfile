FROM eclipse-temurin:17-jre
EXPOSE 9753

ENV OTEL_SERVICE_NAME=PetClinic
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:5050
ENV OTEL_LOGS_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_RESOURCE_ATTRIBUTES=digma.environment=PETCLINIC,digma.environment.type=Public


ADD build/libs/spring-petclinic-*.jar /app.jar
ADD build/otel/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
ADD build/otel/digma-otel-agent-extension.jar /digma-otel-agent-extension.jar

HEALTHCHECK --interval=20s --timeout=3s --start-period=10s --retries=4 \
  CMD curl -f http://localhost:9753/ || exit 1

ENTRYPOINT java -Dotel.instrumentation.common.experimental.controller.telemetry.enabled=true -Dotel.instrumentation.common.experimental.view.telemetry.enabled=true -Dotel.instrumentation.experimental.span-suppression-strategy=none -jar -javaagent:/opentelemetry-javaagent.jar -Dotel.javaagent.extensions=/digma-otel-agent-extension.jar app.jar

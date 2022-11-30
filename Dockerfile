FROM eclipse-temurin:11-jre

ADD build/libs/spring-petclinic-*.jar /app.jar
ADD build/otel/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
ADD build/otel/digma-otel-agent-extension.jar /digma-otel-agent-extension.jar

ENTRYPOINT java -jar -javaagent:/opentelemetry-javaagent.jar -Dotel.javaagent.extensions=/digma-otel-agent-extension.jar app.jar

#
# Build
#
FROM maven:3.9.6-amazoncorretto-17-al2023@sha256:4c8bd9ec72b372f587f7b9d92564a307e4f5180b7ec08455fb346617bae1757e as buildtime
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests


FROM amazoncorretto:17.0.10-alpine3.19@sha256:180e9c91bdbaad3599fedd2f492bf0d0335a9382835aa64669b2c2a8de7c9a22 as builder
COPY --from=buildtime /build/target/*.jar ./application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM ghcr.io/pagopa/docker-base-springboot-openjdk17:v1.1.3@sha256:a4e970ef05ecf2081424a64707e7c20856bbc40ddb3e99b32a24cd74591817c4
#COPY --chown=spring:spring --from=buildtime docker/run.sh ./app/run.sh
#
#WORKDIR /app
#ADD --chown=spring:spring https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar .

#COPY --chown=spring:spring  --from=builder /app/dependencies/ ./
#COPY --chown=spring:spring  --from=builder /app/snapshot-dependencies/ ./
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
#COPY --chown=spring:spring  --from=builder /app/spring-boot-loader/ ./
#COPY --chown=spring:spring  --from=builder /app/application/ ./

EXPOSE 8080

#ENTRYPOINT ["sh", "./run.sh"]

#ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","--enable-preview","org.springframework.boot.loader.JarLauncher"]

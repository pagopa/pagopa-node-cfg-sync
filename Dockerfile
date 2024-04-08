#
# Build
#
FROM maven:3.9.6-amazoncorretto-17-al2023@sha256:4c8bd9ec72b372f587f7b9d92564a307e4f5180b7ec08455fb346617bae1757e as buildtime
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:17.0.10-alpine3.19@sha256:180e9c91bdbaad3599fedd2f492bf0d0335a9382835aa64669b2c2a8de7c9a22 as builder
COPY --from=buildtime /build/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM ghcr.io/pagopa/docker-base-springboot-openjdk17:v1.1.3@sha256:a4e970ef05ecf2081424a64707e7c20856bbc40ddb3e99b32a24cd74591817c4
COPY --chown=spring:spring --from=builder dependencies/ ./
COPY --chown=spring:spring --from=builder snapshot-dependencies/ ./
RUN true
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
RUN true
COPY --chown=spring:spring --from=builder spring-boot-loader/ ./
COPY --chown=spring:spring --from=builder application/ ./


EXPOSE 8080

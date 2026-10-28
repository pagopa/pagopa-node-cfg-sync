# 3.9.9-amazoncorretto-21-al2023
FROM maven@sha256:39f77b0b26ef8be4297b3cfdcedc1ed8ff7699fcd5c5181aed26e89ed2d4ef2b AS buildtime
WORKDIR /build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true

#amazoncorretto:21-alpine
FROM amazoncorretto@sha256:d5a42afb372d2bf5d937d7e6fb65b39c21b62a3f2af3cbcef46848a2f8364c6f AS builder
COPY --from=buildtime /build/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# eclipse-temurin:21-jre
FROM eclipse-temurin@sha256:227118764cccd3c3294617061aaa93a08309c63a395eefa83e6b35032992ea99

RUN addgroup spring && useradd -g spring spring
USER spring:spring

# https://github.com/microsoft/ApplicationInsights-Java/releases
ADD --chown=spring:spring https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.6.0/applicationinsights-agent-3.6.0.jar /applicationinsights-agent.jar

COPY --chown=spring:spring docker-resources/applicationinsights.json ./applicationinsights.json
# ---- end inlined base ----

COPY --chown=spring:spring  --from=builder dependencies/ ./
COPY --chown=spring:spring  --from=builder snapshot-dependencies/ ./
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
RUN true
COPY --chown=spring:spring  --from=builder spring-boot-loader/ ./
COPY --chown=spring:spring  --from=builder application/ ./

EXPOSE 8080


COPY --chown=spring:spring docker-resources/run.sh ./run.sh
RUN chmod +x ./run.sh
ENTRYPOINT ["./run.sh"]

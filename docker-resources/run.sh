#!/bin/sh
#exec java -javaagent:/applicationinsights-agent.jar ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher "$@"
exec java -javaagent:/applicationinsights-agent.jar ${JAVA_OPTS} -jar application.jar
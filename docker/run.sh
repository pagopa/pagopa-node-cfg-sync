#!/bin/sh
ls -la
exec java -javaagent:/applicationinsights-agent.jar ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher "$@"
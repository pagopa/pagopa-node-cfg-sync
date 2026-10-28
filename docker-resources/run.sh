#!/bin/sh

spring_boot_version=$(cat ./META-INF/MANIFEST.MF | grep 'Spring-Boot-Version:' | cut -d ' ' -f 2)
major_version=$(echo "$spring_boot_version" | cut -d '.' -f 1)

# Check if the major_version is 3
if  [ "$major_version" -eq "3" ] ; then
  exec java -javaagent:/applicationinsights-agent.jar ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher "$@"
else
   exec java -javaagent:/applicationinsights-agent.jar ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher "$@"
fi
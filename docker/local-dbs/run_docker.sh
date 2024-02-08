#!/bin/bash

rm -rf postgres-data
docker-compose up -d --remove-orphans --force-recreate
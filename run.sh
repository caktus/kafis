#!/usr/bin/env bash

task="kafis"
./gradlew --quiet "installDist" && "./build/install/${task}/bin/${task}" "$@"

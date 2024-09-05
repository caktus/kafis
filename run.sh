#!/usr/bin/env bash

task="hands-on-clikt"
./gradlew --quiet "installDist" && "./build/install/${task}/bin/${task}" "$@"

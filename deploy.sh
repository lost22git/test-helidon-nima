#!/usr/bin/env bash

./gradlew app:clean && ./gradlew app:build && java --enable-preview -jar ./app/build/libs/app-0.0.1-all.jar

#!/usr/bin/env bash
./gradlew clean build
./gradlew copyCordaJar copyNetworkMapJar copyNotaryConfigs copyShell

cd ./build/docker || exit
docker build . -f ../../DockerfileNotaryOne -t notary_one || exit
docker build . -f ../../DockerfileNotaryTwo -t notary_two || exit
docker build . -f ../../DockerfileNMS -t network_services || exit

docker tag notary_one roastario/notary-one:latest
docker tag notary_two roastario/notary-two:latest
docker tag network_services roastario/network-services:latest

docker push roastario/notary-one:latest
docker push roastario/notary-two:latest
docker push roastario/network-services:latest
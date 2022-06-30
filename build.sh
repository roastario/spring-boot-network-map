#!/usr/bin/env bash
./gradlew clean build || exit
./gradlew copyCordaJar copyNetworkMapJar copyNotaryConfigs copyShell || exit

cd ./build/docker || exit
docker build --build-arg NOTARY_NAME="O=BISNotary, L=Basel, C=CH" . -f ../../DockerfileNotary -t notary_one --no-cache || exit
docker build --build-arg NOTARY_NAME="O=RBANotary, L=Sydney, C=AU" . -f ../../DockerfileNotary -t notary_two --no-cache || exit
docker build --build-arg NOTARY_NAME="O=BNMNotary, L=KualaLumpur, C=MY" . -f ../../DockerfileNotary -t notary_three --no-cache || exit
docker build --build-arg NOTARY_NAME="O=MASNotary, L=Singapore, C=SG" . -f ../../DockerfileNotary -t notary_four --no-cache || exit
docker build --build-arg NOTARY_NAME="O=SARBNotary, L=Pretoria, C=ZA" . -f ../../DockerfileNotary -t notary_five --no-cache || exit
docker build . -f ../../DockerfileNMS -t network_services --no-cache || exit
#
docker tag notary_one roastario/notary-one-fixed:7
docker tag notary_two roastario/notary-two-fixed:7
docker tag notary_three roastario/notary-three-fixed:7
docker tag notary_four roastario/notary-four-fixed:7
docker tag notary_five roastario/notary-five-fixed:7
docker tag network_services roastario/network-services-fixed:7
#
docker push roastario/notary-one-fixed:7
docker push roastario/notary-two-fixed:7
docker push roastario/notary-three-fixed:7
docker push roastario/notary-four-fixed:7
docker push roastario/notary-five-fixed:7

docker push roastario/network-services-fixed:7
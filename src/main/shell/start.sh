#!/bin/bash

echo "starting network map service"

# start the server
echo "starting network map"
echo "$@"
java -Djava.security.egd=file:/dev/urandom -jar /app.jar --nodesDirectoryUrl=file:///opt/notaries/ "$@"
#!/bin/bash

echo "starting network map service"

# start the server
exec java -Djava.security.egd=file:/dev/urandom -jar /app.jar --nodesDirectoryUrl=file:///opt/nodes

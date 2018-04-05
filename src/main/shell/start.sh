#!/bin/bash

echo boot jar: ${JAR_FILE}
echo location of nodes: ${NODES_DIR}

PROPERTIES=""

# start the server
exec java -Djava.security.egd=file:/dev/urandom ${PROFILE} ${PROPERTIES} ${DIST} ${SECURITY} -jar /app.jar

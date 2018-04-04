FROM openjdk:8-jdk-alpine
LABEL maintainer="info@b3i.tech"
VOLUME /tmp
ARG JAR_FILE
ARG NODES_DIR
RUN apk --no-cache --update add bash

RUN mkdir -p /opt/nodes
COPY ${NODES_DIR} /opt/nodes/
ADD ${JAR_FILE} app.jar
COPY src/main/shell/start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
CMD ["/start.sh"]
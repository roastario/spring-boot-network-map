FROM openjdk:8-jdk-alpine
LABEL maintainer="info@b3i.tech"
VOLUME /tmp
ARG JAR_FILE
ARG NODES_DIR
RUN apk --no-cache --update add bash

RUN mkdir -p /opt/nodes
COPY nodes /opt/nodes/
ADD build/libs/spring-boot-network-map.jar app.jar
COPY src/main/shell/start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
CMD ["/start.sh"]
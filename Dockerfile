FROM openjdk:8-jdk-alpine
LABEL maintainer="info@b3i.tech"
VOLUME /tmp
ARG JAR_FILE
RUN apk --no-cache --update add bash
RUN apk add curl
RUN mkdir -p /opt/nodes
ADD ${JAR_FILE} app.jar
ADD start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
CMD ["/start.sh"]
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
RUN apk --no-cache --update add bash
RUN apk add curl
RUN mkdir -p /opt/nodes
COPY ${JAR_FILE} app.jar
COPY start.sh start.sh
COPY nodes/ /opt/nodes/
RUN chmod +x start.sh
EXPOSE 8080
CMD ["/start.sh"]
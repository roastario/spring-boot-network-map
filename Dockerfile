FROM openjdk:8-jdk-alpine
VOLUME /tmp
RUN apk --no-cache --update add bash
RUN apk add curl
RUN mkdir -p /opt/corda
COPY node.conf /opt/corda/node.conf
COPY corda.jar /opt/corda/corda.jar

WORKDIR /opt/corda
RUN export PUBLIC_ADDRESS=localhost && cd /opt/corda && java -jar corda.jar --just-generate-node-info
WORKDIR /

COPY start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
EXPOSE 10200
CMD ["/start.sh"]
COPY app.jar app.jar
RUN mkdir -p /jars
VOLUME ["/jars"]

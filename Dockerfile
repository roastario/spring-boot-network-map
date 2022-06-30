FROM openjdk:8-jdk-alpine
VOLUME /tmp
RUN apk --no-cache --update add bash
RUN apk add curl
RUN mkdir -p /opt/corda
COPY node.conf /opt/notaries/node.conf
COPY corda.jar /opt/notaries/corda.jar

WORKDIR /opt/notaries
RUN export NETWORK_SERVICES_URL=http://localhost PUBLIC_ADDRESS=localhost && java -jar /opt/notaries/corda.jar run-migration-scripts --core-schemas --app-schemas && java -jar /opt/notaries/corda.jar --just-generate-node-info
WORKDIR /

COPY start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
EXPOSE 10200
COPY app.jar app.jar
RUN mkdir -p /jars
VOLUME ["/jars"]
ENTRYPOINT ["/start.sh"]
CMD ["--minimumPlatformVersion=4"]

FROM arm32v7/ubuntu:18.10
RUN apt-get update \
    && apt-get install -y wget \
    && mkdir -p /opt/zulu \
    && wget  -O /opt/zulu/zulu_jvm.tar.gz http://cdn.azul.com/zulu-embedded/bin/zulu8.36.0.152-ca-jdk1.8.0_202-linux_aarch32hf.tar.gz \
    && cd /opt/zulu && tar -zxvf zulu_jvm.tar.gz && cd / \
    && mkdir -p /opt/corda
ENV PATH=$PATH:/opt/zulu/zulu8.36.0.152-ca-jdk1.8.0_202-linux_aarch32hf/bin
COPY node.conf /opt/notaries/node.conf
COPY corda.jar /opt/notaries/corda.jar

WORKDIR /opt/corda
RUN export PUBLIC_ADDRESS=localhost && cd /opt/corda && java -jar /opt/notaries/corda.jar --just-generate-node-info --base-directory=/opt/notaries
WORKDIR /

COPY start.sh start.sh
RUN chmod +x start.sh
EXPOSE 8080
EXPOSE 10200
COPY app.jar app.jar
RUN mkdir -p /jars
VOLUME ["/jars"]
ENTRYPOINT ["/start.sh"]

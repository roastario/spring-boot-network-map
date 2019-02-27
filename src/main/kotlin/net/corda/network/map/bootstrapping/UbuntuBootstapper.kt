package net.corda.network.map.bootstrapping

import org.springframework.stereotype.Component

@Component
class UbuntuBootstapper {

    fun installScript(host: String): String {
        return """
#!/bin/sh

apt-get update && apt-get install -y openjdk-8-jre sudo

PUBLIC_IP=${'$'}(curl ifconfig.me)
ORG=${'$'}(</dev/urandom tr -dc A-Za-z0-9 | head -c16)

NODE_CONF=${'$'}(cat <<EOF
myLegalName : "O=${'$'}ORG, L=Zurich, C=CH"
p2pAddress : "${'$'}PUBLIC_IP:10000"
rpcSettings : {
address = "0.0.0.0:10001"
adminAddress = "localhost:10002"
}
rpcUsers: [
{ username=changeme, password=changeme, permissions=[ALL]}
]
devMode = false
networkServices {
doormanURL = "http://$host"
networkMapURL = "http://$host"
}
sshd {
port = 2222
}
detectPublicIp = true
EOF
)

CORDA_SERVICE=${'$'}(cat <<EOF
[Unit]
Description=Corda Node
Requires=network.target

[Service]
Type=simple
User=corda
WorkingDirectory=/opt/corda
ExecStart=/usr/bin/java -jar /opt/corda/corda.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
)

adduser --system --no-create-home --group corda
mkdir /opt/corda && chown corda:corda /opt/corda

sudo -u corda bash -c "
curl -o /opt/corda/corda.jar https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda/4.0/corda-4.0.jar
mkdir /opt/corda/certificates
curl -o /opt/corda/certificates/network-root-truststore.jks http://$host/truststore
echo '${'$'}NODE_CONF' > /opt/corda/node.conf
cd /opt/corda; java -jar corda.jar initial-registration -p trustpass"

echo "${'$'}CORDA_SERVICE" > /etc/systemd/system/corda.service
systemctl daemon-reload
sudo systemctl enable --now corda
            """.trimIndent()
    }

}
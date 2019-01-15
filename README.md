# The network map
Corda uses a http protocol for sharing address/identity mappings
This container aims to be a simple way of getting started with corda networks. 
To do this, it provides a simple non-validating notary and a network map within a single container


# The doorman 
Corda is a _permissioned_ system, and control of which identities are allowed onto a network is provided by the doorman 
protocol as described [here](https://docs.corda.net/permissioning.html). 

This container implements provides an "Auto" accept doorman, with a trust root of the corda development certificate authority. This trust root is available at the endpoint `http://<serving_url>/trustroot` 
with a password of `trustpass`. 


## Building the project

* To build the network map jar use: ```./gradlew clean build```
* To build the network map jar and the docker image use: ```./gradlew clean build && ./gradlew docker``` (This must be run as two commands due to an issue with the docker plugin used //TODO - change plugin)

## Notary Config
The notary must be configured to know what address to advertise itself on. 
This is done via a PUBLIC_ADDRESS environment variable passed to the container

## WhiteListing of contracts
If your cordapp makes use of the zone whitelist, you must provide a set of jars for the container to whitelist. 
This is done via mounting a folder into the /jars directory. 


### Example Run with default ports
 
```$xslt
docker run -it -v corda/samples/bank-of-corda-demo/build/nodes/BankOfCorda/cordapps:/jars -p 8080:8080 -p 10200:10200 -e PUBLIC_ADDRESS=stefano-corda.azure.io roastario/notary-and-network-map:latest 
```

will expose the network map on port 8080, whilst the notary will be exposed on port 10200 and advertised as `stefano-corda.azure.io`

This is a very simple corda-3 compatible network map service. 

There are two ways of using the service. 

1. As a plain java -jar application
2. As a dockerised container

**Building the project:**

java -jar

    ./gradlew clean build
    
docker

    ./gradlew clean docker
    
**Configuring Notaries:** 

The network-map service requires the notaries that will comprise the compatibility zone ahead of time. 

java -jar

    java -jar <path_to_artifact> --nodesDirectoryUrl=<location_of_folder_containing_notary_nodes>
    
docker
    
    docker run --volume <location_of_folder_containing_notary_nodes>:/opt/nodes  net.corda/spring-boot-network-map 

This directory must have the following structure. 

    -dir
        - Notary1
            - node.conf
            - nodeinfo-298349238749283742...
        - Notary2
            - node.conf
            - nodeinfo-293847293847293448... 
            
            

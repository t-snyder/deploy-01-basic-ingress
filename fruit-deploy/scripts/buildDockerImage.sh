#!/bin/bash

# Load image to minikube when built
eval $(minikube docker-env)

cp -r ../../papaya/target/lib/  ./lib
cp    ../../papaya/target/PapayaServer.jar ./PapayaServer.jar

docker build -t library/papaya:1.0 -f PapayaDockerFile .

rm PapayaServer.jar
rm -rf lib

cp -r ../../passionfruit/target/lib ./lib
cp    ../../passionfruit/target/PassionFruitServer.jar ./PassionFruitServer.jar

docker build -t library/passionfruit:1.0 -f PassionDockerFile .

rm PassionFruitServer.jar
rm -rf lib

cd $CURRENTDIR

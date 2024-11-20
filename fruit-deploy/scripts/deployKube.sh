#!/bin/bash

######## Note - this script is not designed to run automated.
###########################################################################################
# Cmds for creatig a Fresh Minikube deployment
minikube delete

# minikube start - change for your machine and requirements
minikube start --cpus 4 --memory 12288 --vm-driver kvm2 --disk-size 100g --insecure-registry="192.168.39.0/24" 

# Enable addons
minikube addons enable dashboard
minikube addons enable ingress

# Start dashboard
minikube dashboard

####### Open a new terminal #################
##### TODO - Change path to your deployment #####################################
PROTODIR=/media/tim/ExtraDrive1/Projects/ingress-nginx-test/fruit-deploy/

#Install cert-manager
echo "    "
echo "    "
echo "Installing cert-manager"
/bin/bash $PROTODIR/scripts/deployCertManager.sh

/bin/bash $PROTODIR/scripts/initNamespaces.sh

kubectl -n mango apply -f $PROTODIR/kube/root-tls-cert-issuer.yaml
kubectl -n mango wait --timeout=30s --for=condition=Ready issuer/root-tls-cert-issuer

# Use the self-signing issuer to generate the org Issuers, one for each org.
kubectl -n mango apply -f $PROTODIR/kube/mango-tls-cert-issuer.yaml
kubectl -n mango wait --timeout=30s --for=condition=Ready issuer/mango-tls-cert-issuer

kubectl apply -f $PROTODIR/kube/apple-path.yaml  -n apple
kubectl apply -f $PROTODIR/kube/apple-host.yaml  -n apple
kubectl apply -f $PROTODIR/kube/banana-path.yaml -n banana
kubectl apply -f $PROTODIR/kube/banana-host.yaml -n banana

kubectl create -f $PROTODIR/kube/mango.yaml -n mango

echo "Waiting for all components to be up"
sleep 30

#Build pekko-http server app docker images
echo "    "
echo "    "
echo "Build docker image and put into minikube image repo"
CURRENTDIR=${PWD}
echo $CURRENTDIR

cd $PROTODIR/docker
/bin/bash $PROTODIR/scripts/buildDockerImage.sh
cd $CURRENTDIR

echo "      "
echo "      "
echo "Creating papaya server secret"
kubectl apply -f $PROTODIR/kube/papaya-auth.yaml -n papaya

echo "      "
echo "      "
echo "Creating papaya server persistent volume claim"
kubectl apply -f $PROTODIR/kube/papaya-pvc.yaml -n papaya

echo "      "
echo "      "
echo "Deploying root-tls-cert-issuer"
kubectl -n papaya apply -f $PROTODIR/kube/root-tls-cert-issuer.yaml
kubectl -n papaya wait --timeout=30s --for=condition=Ready issuer/root-tls-cert-issuer

echo "      "
echo "      "
echo "Deploying tls_server-tls-cert-issuer"
kubectl -n papaya apply -f $PROTODIR/kube/papaya-tls-cert-issuer.yaml
kubectl -n papaya wait --timeout=30s --for=condition=Ready issuer/papaya-tls-cert-issuer

echo "      "
echo "      "
echo "Deploying tls_server deployment, service and ingress"
kubectl apply -f $PROTODIR/kube/papaya.yaml -n papaya

echo "      "
echo "      "
echo "Deploying passion deployment, service and ingress"
kubectl apply -f $PROTODIR/kube/passionfruit.yaml -n passion

ipAddr=$(minikube ip)
echo "Minikube ip = $ipAddr"
sudo -- sh -c 'echo "\n'"$ipAddr"' passion.foo.com apple.foo.com banana.foo.com mango.foo.com papaya.foo.com\n" >> /etc/hosts'


# Below are some sample curl calls to test the deployment.
#curl -kL http://$ipAddr/apple
# Successful response = apple-path

#curl -kL http://$ipAddr/banana
# Successful response = banana-path

#echo "Invoking a not found"
#curl -kL http://$ipAddr/notfound

#curl -kL http://apple.foo.com/apple
# Successful response = apple-host

#curl -kL http://banana.foo.com/banana
# Successful response = banana-host

#curl -kL https://mango.foo.com/mango
# Successful response = juicy mango

#curl -kL http://passion.foo.com/passion
# Successful response = <h1>Pekko-http loves passion fruit</h1>

#curl -kL https://papaya.foo.com/papaya
# Successful response = Pekko-http says that Papaya is a sweet fruit

